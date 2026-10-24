package hotel.model;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Domain entity representing a confirmed booking.
 * Strictly adheres to security rules:
 * - NEVER stores CVC (field does not exist).
 * - NEVER stores full card number (only masked representation).
 */
public class Reservation {
    private final String reservationReference;
    private final String requestId; // Idempotency key
    private final String offerId;
    private final String agencyId;
    private final String hotelName;
    private final String roomType;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final double totalPrice;
    private final String maskedCardNumber;
    private final String travelerName;
    private final Instant createdAt;

    public Reservation(String reservationReference, String requestId, String offerId, String agencyId,
                       String hotelName, String roomType, LocalDate checkIn, LocalDate checkOut,
                       double totalPrice, String maskedCardNumber, String travelerName, Instant createdAt) {
        this.reservationReference = reservationReference;
        this.requestId = requestId;
        this.offerId = offerId;
        this.agencyId = agencyId;
        this.hotelName = hotelName;
        this.roomType = roomType;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.totalPrice = totalPrice;
        this.maskedCardNumber = maskedCardNumber;
        this.travelerName = travelerName;
        this.createdAt = createdAt;
    }

    public String getReservationReference() { return reservationReference; }
    public String getRequestId() { return requestId; }
    public String getOfferId() { return offerId; }
    public String getAgencyId() { return agencyId; }
    public String getHotelName() { return hotelName; }
    public String getRoomType() { return roomType; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public double getTotalPrice() { return totalPrice; }
    public String getMaskedCardNumber() { return maskedCardNumber; }
    public String getTravelerName() { return travelerName; }
    public Instant getCreatedAt() { return createdAt; }
}
