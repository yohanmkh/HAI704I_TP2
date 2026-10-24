package hotel.repo;

import hotel.model.Quote;
import hotel.model.Reservation;
import org.mindrot.jbcrypt.BCrypt;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryHotelRepository implements HotelRepository {
    private final String hotelProfile;
    private final List<Hotel> hotels = new ArrayList<>();
    private final Map<String, String> agencyHashedSecrets = new HashMap<>();
    private final Map<String, Double> agencyDiscounts = new HashMap<>();

    private final Map<String, Quote> quoteStore = new ConcurrentHashMap<>();
    private final Map<String, Reservation> idempotencyStore = new ConcurrentHashMap<>();
    private final Map<String, Reservation> reservationStore = new ConcurrentHashMap<>();

    public InMemoryHotelRepository() {
        this("hotel1");
    }

    public InMemoryHotelRepository(String hotelProfile) {
        this.hotelProfile = hotelProfile != null ? hotelProfile.toLowerCase().trim() : "hotel1";
        initSampleData();
        initPartnerAgencies();
    }

    private void initPartnerAgencies() {
        // Shared partner agencies
        agencyHashedSecrets.put("AGENCY_VOYAGE_PRO", BCrypt.hashpw("secret_voyage_123", BCrypt.gensalt(10)));
        agencyHashedSecrets.put("AGENCY_SELECTOUR", BCrypt.hashpw("secret_selectour_456", BCrypt.gensalt(10)));
        agencyHashedSecrets.put("AGENCY_BOOKING_EXP", BCrypt.hashpw("secret_booking_789", BCrypt.gensalt(10)));

        // Distinct partner discount rates per hotel profile
        if ("hotel2".equals(hotelProfile)) {
            // Hotel 2 negotiated rates: Selectour has best rate (20%), VoyagePro has 5%
            agencyDiscounts.put("AGENCY_VOYAGE_PRO", 0.05);
            agencyDiscounts.put("AGENCY_SELECTOUR", 0.20);
            agencyDiscounts.put("AGENCY_BOOKING_EXP", 0.15);
        } else {
            // Hotel 1 negotiated rates: VoyagePro has best rate (15%), Selectour has 5%
            agencyDiscounts.put("AGENCY_VOYAGE_PRO", 0.15);
            agencyDiscounts.put("AGENCY_SELECTOUR", 0.05);
            agencyDiscounts.put("AGENCY_BOOKING_EXP", 0.10);
        }
    }

    private void initSampleData() {
        if ("hotel2".equals(hotelProfile)) {
            // Hotel 2 profile: "Hôtel de la Comédie"
            Hotel h2 = new Hotel("H2", "Hôtel de la Comédie", "Montpellier", 4);
            h2.addRoom(new Room("H2-R1", "Superior Classic", 2, 130.0));
            h2.addRoom(new Room("H2-R2", "Executive King", 2, 190.0));
            h2.addRoom(new Room("H2-R3", "Panoramique Suite", 4, 310.0));
            hotels.add(h2);

            Hotel h2b = new Hotel("H2B", "Le Mas Saint-Roch", "Montpellier", 5);
            h2b.addRoom(new Room("H2B-R1", "Royal Villa", 4, 520.0));
            hotels.add(h2b);
        } else {
            // Hotel 1 profile: "Grand Hotel Montpellier"
            Hotel h1 = new Hotel("H1", "Grand Hotel Montpellier", "Montpellier", 4);
            h1.addRoom(new Room("H1-R1", "Single Comfort", 1, 100.0));
            h1.addRoom(new Room("H1-R2", "Double Deluxe", 2, 150.0));
            h1.addRoom(new Room("H1-R3", "Family Suite", 4, 250.0));
            hotels.add(h1);

            Hotel h1b = new Hotel("H1B", "Montpellier City Centre", "Montpellier", 3);
            h1b.addRoom(new Room("H1B-R1", "Standard Double", 2, 90.0));
            hotels.add(h1b);
        }
    }

    @Override
    public List<Hotel> findAll() {
        return Collections.unmodifiableList(hotels);
    }

    @Override
    public List<Hotel> findByCity(String city) {
        if (city == null || city.trim().isEmpty()) {
            return findAll();
        }
        return hotels.stream()
                .filter(h -> h.getCity().equalsIgnoreCase(city.trim()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isPartnerAgency(String agencyId) {
        if (agencyId == null || agencyId.trim().isEmpty()) {
            return false;
        }
        return agencyHashedSecrets.containsKey(agencyId.trim());
    }

    @Override
    public boolean verifyAgencySecret(String agencyId, String candidateSecret) {
        if (agencyId == null || candidateSecret == null) {
            return false;
        }
        String hashed = agencyHashedSecrets.get(agencyId.trim());
        if (hashed == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(candidateSecret, hashed);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public double getAgencyDiscount(String agencyId) {
        if (agencyId == null) {
            return 0.0;
        }
        return agencyDiscounts.getOrDefault(agencyId.trim(), 0.0);
    }

    @Override
    public void saveQuote(Quote quote) {
        if (quote != null) {
            quoteStore.put(quote.getOfferId(), quote);
        }
    }

    @Override
    public Optional<Quote> findQuoteById(String offerId) {
        if (offerId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(quoteStore.get(offerId));
    }

    @Override
    public Optional<Reservation> findReservationByRequestId(String requestId) {
        if (requestId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(idempotencyStore.get(requestId));
    }

    @Override
    public void saveReservation(Reservation reservation) {
        if (reservation != null) {
            idempotencyStore.put(reservation.getRequestId(), reservation);
            reservationStore.put(reservation.getReservationReference(), reservation);
        }
    }
}
