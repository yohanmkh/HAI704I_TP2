package hotel.model;

import java.time.LocalDate;

/**
 * Domain model representing search parameters within the core business logic.
 * Decoupled from transport DTOs.
 */
public class SearchCriteria {
    private final String agencyId;
    private final String city;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final int minStars;
    private final double maxPrice;

    public SearchCriteria(String agencyId, String city, LocalDate checkIn, LocalDate checkOut, int guests, int minStars, double maxPrice) {
        this.agencyId = agencyId;
        this.city = city;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.minStars = minStars;
        this.maxPrice = maxPrice;
    }

    public String getAgencyId() {
        return agencyId;
    }

    public String getCity() {
        return city;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public int getGuests() {
        return guests;
    }

    public int getMinStars() {
        return minStars;
    }

    public double getMaxPrice() {
        return maxPrice;
    }
}
