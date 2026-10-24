package agence.service;

/**
 * Representation of a booking outcome returned to the CLI layer.
 * Contains no SOAP or JAX-WS references.
 */
public class ReservationReceipt {
    private final boolean success;
    private final String reference;
    private final String hotelName;
    private final String roomType;
    private final String stayDates;
    private final double totalPrice;
    private final String maskedCard;
    private final String travelerName;
    private final boolean replayed;
    private final String errorMessage;

    public static ReservationReceipt success(String reference, String hotelName, String roomType,
                                             String stayDates, double totalPrice, String maskedCard,
                                             String travelerName, boolean replayed) {
        return new ReservationReceipt(true, reference, hotelName, roomType, stayDates,
                totalPrice, maskedCard, travelerName, replayed, null);
    }

    public static ReservationReceipt failure(String errorMessage) {
        return new ReservationReceipt(false, null, null, null, null, 0.0, null, null, false, errorMessage);
    }

    private ReservationReceipt(boolean success, String reference, String hotelName, String roomType,
                               String stayDates, double totalPrice, String maskedCard,
                               String travelerName, boolean replayed, String errorMessage) {
        this.success = success;
        this.reference = reference;
        this.hotelName = hotelName;
        this.roomType = roomType;
        this.stayDates = stayDates;
        this.totalPrice = totalPrice;
        this.maskedCard = maskedCard;
        this.travelerName = travelerName;
        this.replayed = replayed;
        this.errorMessage = errorMessage;
    }

    public boolean isSuccess() { return success; }
    public String getReference() { return reference; }
    public String getHotelName() { return hotelName; }
    public String getRoomType() { return roomType; }
    public String getStayDates() { return stayDates; }
    public double getTotalPrice() { return totalPrice; }
    public String getMaskedCard() { return maskedCard; }
    public String getTravelerName() { return travelerName; }
    public boolean isReplayed() { return replayed; }
    public String getErrorMessage() { return errorMessage; }
}
