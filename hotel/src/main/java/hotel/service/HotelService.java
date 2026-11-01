package hotel.service;

import hotel.model.HotelOffer;
import hotel.model.Quote;
import hotel.model.Reservation;
import hotel.model.ReservationCommand;
import hotel.model.ReservationResult;
import hotel.model.SearchCriteria;
import hotel.repo.Hotel;
import hotel.repo.HotelRepository;
import hotel.repo.Room;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class HotelService {
    private final HotelRepository repo;

    public HotelService(HotelRepository repo) {
        this.repo = repo;
    }

    public List<HotelOffer> searchRooms(SearchCriteria crit) throws UnauthorizedAgencyDomainException {
        List<HotelOffer> list = new ArrayList<>();
        if (crit == null) {
            return list;
        }

        // on vérifie quand même si l'agence est partenaire
        String agencyId = crit.getAgencyId();
        if (!repo.isPartnerAgency(agencyId)) {
            throw new UnauthorizedAgencyDomainException(agencyId, "Agence non reconnue : " + agencyId);
        }

        double discount = repo.getAgencyDiscount(agencyId);
        long nuits = calculNuits(crit);
        List<Hotel> hotels = repo.findByCity(crit.getCity());

        for (Hotel h : hotels) {
            if (crit.getMinStars() > 0 && h.getStars() < crit.getMinStars()) {
                continue;
            }

            for (Room r : h.getRooms()) {
                if (crit.getGuests() > 0 && r.getCapacity() < crit.getGuests()) {
                    continue;
                }

                // tarif négocié avec l'agence
                double prixNuit = Math.round(r.getPricePerNight() * (1.0 - discount) * 100.0) / 100.0;

                if (crit.getMaxPrice() > 0 && prixNuit > crit.getMaxPrice()) {
                    continue;
                }

                double total = Math.round(prixNuit * nuits * 100.0) / 100.0;
                String offerId = "OFFER-" + h.getId() + "-" + r.getId();

                // Check if quote already exists - if booked, don't overwrite (preserve booked status)
                // but still include the room in search results so user sees it and gets error on reserve
                Optional<Quote> existingQuote = repo.findQuoteById(offerId);
                if (existingQuote.isEmpty() || !existingQuote.get().isBooked()) {
                    LocalDate in = crit.getCheckIn() != null ? crit.getCheckIn() : LocalDate.now();
                    LocalDate out = crit.getCheckOut() != null ? crit.getCheckOut() : LocalDate.now().plusDays(nuits);
                    Instant exp = Instant.now().plus(2, ChronoUnit.HOURS);

                    // devis daté pour figer le prix
                    Quote q = new Quote(offerId, h.getId(), h.getName(), r.getType(), in, out, prixNuit, total, agencyId, exp);
                    repo.saveQuote(q);
                }

                list.add(new HotelOffer(offerId, h.getName(), r.getType(), prixNuit, h.getStars(), total));
            }
        }

        return list;
    }

    public ReservationResult reserve(ReservationCommand cmd)
            throws UnauthorizedAgencyDomainException,
                   OfferNotFoundDomainException,
                   PaymentValidationDomainException,
                   OfferUnavailableDomainException {

        if (cmd == null) {
            throw new IllegalArgumentException("Commande nulle");
        }

        // vérif du mot de passe avec le hash bcrypt
        if (!repo.verifyAgencySecret(cmd.getAgencyId(), cmd.getAgencySecret())) {
            throw new UnauthorizedAgencyDomainException(cmd.getAgencyId(), "Secret agence invalide pour " + cmd.getAgencyId());
        }

        // rejeu idempotent : si on a déjà traité cette demande, on renvoie la résa existante
        String reqId = cmd.getRequestId();
        if (reqId != null && !reqId.isBlank()) {
            Optional<Reservation> existante = repo.findReservationByRequestId(reqId);
            if (existante.isPresent()) {
                return new ReservationResult(existante.get(), true);
            }
        }

        // récup du devis
        Optional<Quote> opt = repo.findQuoteById(cmd.getOfferId());
        if (opt.isEmpty()) {
            throw new OfferNotFoundDomainException(cmd.getOfferId(), "Offre inconnue : " + cmd.getOfferId());
        }

        Quote q = opt.get();
        if (q.isBooked()) {
            throw new OfferUnavailableDomainException(cmd.getOfferId(), "ALREADY_BOOKED", "Chambre déjà réservée.");
        }
        if (q.isExpired()) {
            throw new OfferUnavailableDomainException(cmd.getOfferId(), "EXPIRED", "Offre expirée.");
        }

        validerPaiement(cmd.getCardNumber(), cmd.getExpiryMonth(), cmd.getExpiryYear(), cmd.getCvc());

        // on masque le numéro et on ne garde surtout pas le CVC
        String raw = cmd.getCardNumber().replaceAll("\\s+", "");
        String masque = "****-****-****-" + raw.substring(raw.length() - 4);

        q.setBooked(true);

        String ref = "RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String voyageur = (cmd.getTravelerFirstName() + " " + cmd.getTravelerLastName()).trim();

        Reservation res = new Reservation(
                ref, reqId, q.getOfferId(), q.getAgencyId(),
                q.getHotelName(), q.getRoomType(), q.getCheckIn(), q.getCheckOut(),
                q.getTotalPrice(), masque, voyageur, Instant.now()
        );

        repo.saveReservation(res);
        return new ReservationResult(res, false);
    }

    private void validerPaiement(String card, int month, int year, String cvc) throws PaymentValidationDomainException {
        if (card == null || card.isBlank()) {
            throw new PaymentValidationDomainException("EMPTY_CARD", "Numéro de carte manquant.");
        }

        String digits = card.replaceAll("\\s+", "");
        if (!digits.matches("\\d{13,19}")) {
            throw new PaymentValidationDomainException("INVALID_CARD_NUMBER", "Numéro de carte invalide (doit contenir 13 à 19 chiffres).");
        }

        if (month < 1 || month > 12) {
            throw new PaymentValidationDomainException("INVALID_EXPIRY_MONTH", "Mois d'expiration invalide.");
        }

        YearMonth cur = YearMonth.now();
        YearMonth exp = YearMonth.of(year, month);
        if (exp.isBefore(cur)) {
            throw new PaymentValidationDomainException("CARD_EXPIRED", "Carte bancaire expirée.");
        }

        if (cvc == null || !cvc.trim().matches("\\d{3,4}")) {
            throw new PaymentValidationDomainException("INVALID_CVC", "CVC invalide (3 ou 4 chiffres attendus).");
        }
    }

    private long calculNuits(SearchCriteria crit) {
        if (crit.getCheckIn() == null || crit.getCheckOut() == null) {
            return 1;
        }
        long d = ChronoUnit.DAYS.between(crit.getCheckIn(), crit.getCheckOut());
        return d > 0 ? d : 1;
    }
}
