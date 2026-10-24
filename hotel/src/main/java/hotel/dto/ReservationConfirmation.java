package hotel.dto;

import java.io.Serializable;

/**
 * Transfer object returned upon successful reservation.
 * Contains the booking reference, confirmation of locked price, masked card number, and replay status.
 */
public class ReservationConfirmation implements Serializable {
    private static final long serialVersionUID = 1L;

    private String reservationReference;
    private String hotelName;
    private String roomType;
    private String checkIn;
    private String checkOut;
    private double totalPrice;
    private String maskedCardNumber;
    private String travelerName;
    private boolean replayed;

    public ReservationConfirmation() {
    }

    public ReservationConfirmation(String reservationReference, String hotelName, String roomType,
                                   String checkIn, String checkOut, double totalPrice,
                                   String maskedCardNumber, String travelerName, boolean replayed) {
        this.reservationReference = reservationReference;
        this.hotelName = hotelName;
        this.roomType = roomType;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.totalPrice = totalPrice;
        this.maskedCardNumber = maskedCardNumber;
        this.travelerName = travelerName;
        this.replayed = replayed;
    }

    public String getReservationReference() {
        return reservationReference;
    }

    public void setReservationReference(String reservationReference) {
        this.reservationReference = reservationReference;
    }

    public String getHotelName() {
        return hotelName;
    }

    public void setHotelName(String hotelName) {
        this.hotelName = hotelName;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
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

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getMaskedCardNumber() {
        return maskedCardNumber;
    }

    public void setMaskedCardNumber(String maskedCardNumber) {
        this.maskedCardNumber = maskedCardNumber;
    }

    public String getTravelerName() {
        return travelerName;
    }

    public void setTravelerName(String travelerName) {
        this.travelerName = travelerName;
    }

    public boolean isReplayed() {
        return replayed;
    }

    public void setReplayed(boolean replayed) {
        this.replayed = replayed;
    }

    @Override
    public String toString() {
        return "ReservationConfirmation{" +
                "reference='" + reservationReference + '\'' +
                ", hotel='" + hotelName + '\'' +
                ", room='" + roomType + '\'' +
                ", dates=" + checkIn + " to " + checkOut +
                ", totalPrice=" + totalPrice +
                ", card='" + maskedCardNumber + '\'' +
                ", traveler='" + travelerName + '\'' +
                ", replayed=" + replayed +
                '}';
    }
}
