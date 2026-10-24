package hotel.dto;

import java.io.Serializable;

/**
 * Data Transfer Object representing the search parameters for room availability.
 * Includes agency authentication/identification details for partner verification.
 */
public class SearchRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String agencyId;
    private String city;
    private String checkIn;
    private String checkOut;
    private int guests;
    private int minStars;
    private double maxPrice;

    public SearchRequest() {
    }

    public SearchRequest(String agencyId, String city, String checkIn, String checkOut, int guests, int minStars, double maxPrice) {
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

    public void setAgencyId(String agencyId) {
        this.agencyId = agencyId;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(String checkIn) {
        this.checkIn = checkIn;
    }

    public String getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(String checkOut) {
        this.checkOut = checkOut;
    }

    public int getGuests() {
        return guests;
    }

    public void setGuests(int guests) {
        this.guests = guests;
    }

    public int getMinStars() {
        return minStars;
    }

    public void setMinStars(int minStars) {
        this.minStars = minStars;
    }

    public double getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(double maxPrice) {
        this.maxPrice = maxPrice;
    }

    @Override
    public String toString() {
        return "SearchRequest{" +
                "agencyId='" + agencyId + '\'' +
                ", city='" + city + '\'' +
                ", checkIn='" + checkIn + '\'' +
                ", checkOut='" + checkOut + '\'' +
                ", guests=" + guests +
                ", minStars=" + minStars +
                ", maxPrice=" + maxPrice +
                '}';
    }
}
