package hotel.service;

import hotel.model.HotelOffer;
import hotel.model.ReservationCommand;
import hotel.model.ReservationResult;
import hotel.model.SearchCriteria;
import hotel.repo.InMemoryHotelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HotelServiceTest {

    private HotelService service;

    @BeforeEach
    void setUp() {
        service = new HotelService(new InMemoryHotelRepository("hotel1"));
    }

    @Test
    void testRechercheEtReservation() throws Exception {
        SearchCriteria crit = new SearchCriteria(
                "AGENCY_VOYAGE_PRO", "Montpellier",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4),
                2, 4, 300.0
        );

        List<HotelOffer> offres = service.searchRooms(crit);
        assertFalse(offres.isEmpty());
        HotelOffer offre = offres.get(0);

        String reqId = UUID.randomUUID().toString();
        ReservationCommand cmd = new ReservationCommand(
                "AGENCY_VOYAGE_PRO", "secret_voyage_123",
                offre.getOfferId(), reqId,
                "Alice", "Dupont",
                "4532 1111 2222 3333", 12, 2028, "123"
        );

        ReservationResult res = service.reserve(cmd);
        assertNotNull(res);
        assertFalse(res.isReplayed());
        assertNotNull(res.getReservation().getReservationReference());
        assertEquals("****-****-****-3333", res.getReservation().getMaskedCardNumber());

        // rejeu avec le même requestId : doit renvoyer la même résa
        ReservationResult rejeu = service.reserve(cmd);
        assertTrue(rejeu.isReplayed());
        assertEquals(res.getReservation().getReservationReference(), rejeu.getReservation().getReservationReference());

        // nouvelle résa sur la même offre déjà prise : doit refuser
        ReservationCommand autreCmd = new ReservationCommand(
                "AGENCY_VOYAGE_PRO", "secret_voyage_123",
                offre.getOfferId(), UUID.randomUUID().toString(),
                "Bob", "Martin",
                "4532 9999 8888 7777", 10, 2029, "456"
        );
        assertThrows(OfferUnavailableDomainException.class, () -> service.reserve(autreCmd));
    }

    @Test
    void testMauvaisSecret() throws Exception {
        SearchCriteria crit = new SearchCriteria(
                "AGENCY_VOYAGE_PRO", "Montpellier",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3), 1, 1, 500.0);
        List<HotelOffer> offres = service.searchRooms(crit);

        ReservationCommand cmd = new ReservationCommand(
                "AGENCY_VOYAGE_PRO", "mauvais_mdp",
                offres.get(0).getOfferId(), UUID.randomUUID().toString(),
                "Alice", "Dupont",
                "4532 1111 2222 3333", 12, 2028, "123"
        );

        assertThrows(UnauthorizedAgencyDomainException.class, () -> service.reserve(cmd));
    }
}
