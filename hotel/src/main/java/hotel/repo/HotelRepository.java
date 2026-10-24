package hotel.repo;

import hotel.model.Quote;
import hotel.model.Reservation;

import java.util.List;
import java.util.Optional;

public interface HotelRepository {
    List<Hotel> findAll();
    List<Hotel> findByCity(String city);
    boolean isPartnerAgency(String agencyId);
    boolean verifyAgencySecret(String agencyId, String candidateSecret);
    double getAgencyDiscount(String agencyId);

    void saveQuote(Quote quote);
    Optional<Quote> findQuoteById(String offerId);

    Optional<Reservation> findReservationByRequestId(String requestId);
    void saveReservation(Reservation reservation);
}
