package hotel.model;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Domain entity representing an Offer as a dated quote.
 * Locks in the price, dates, room, and agency at creation time.
 * "Le prix annoncé doit être le prix payé."
 */
public class Quote {
    private final String offerId;
    private final String hotelId;
    private final String hotelName;
    private final String roomType;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final double pricePerNight;
    private final double totalPrice;
    private final String agencyId;
    private final Instant expiresAt;
    private boolean booked;

    public Quote(String offerId, String hotelId, String hotelName, String roomType,
                 LocalDate checkIn, LocalDate checkOut, double pricePerNight,
                 double totalPrice, String agencyId, Instant expiresAt) {
        this.offerId = offerId;
        this.hotelId = hotelId;
        this.hotelName = hotelName;
        this.roomType = roomType;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.pricePerNight = pricePerNight;
        this.totalPrice = totalPrice;
        this.agencyId = agencyId;
        this.expiresAt = expiresAt;
        this.booked = false;
    }

    public String getOfferId() { return offerId; }
    public String getHotelId() { return hotelId; }
    public String getHotelName() { return hotelName; }
    public String getRoomType() { return roomType; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public double getPricePerNight() { return pricePerNight; }
    public double getTotalPrice() { return totalPrice; }
    public String getAgencyId() { return agencyId; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isBooked() { return booked; }
    public void setBooked(boolean booked) { this.booked = booked; }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
