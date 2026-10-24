package hotel.model;

/**
 * Domain command object carrying data necessary to execute a reservation.
 * Decoupled from transport DTOs.
 */
public class ReservationCommand {
    private final String agencyId;
    private final String agencySecret;
    private final String offerId;
    private final String requestId;
    private final String travelerFirstName;
    private final String travelerLastName;
    private final String cardNumber;
    private final int expiryMonth;
    private final int expiryYear;
    private final String cvc;

    public ReservationCommand(String agencyId, String agencySecret, String offerId, String requestId,
                              String travelerFirstName, String travelerLastName,
                              String cardNumber, int expiryMonth, int expiryYear, String cvc) {
        this.agencyId = agencyId;
        this.agencySecret = agencySecret;
        this.offerId = offerId;
        this.requestId = requestId;
        this.travelerFirstName = travelerFirstName;
        this.travelerLastName = travelerLastName;
        this.cardNumber = cardNumber;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.cvc = cvc;
    }

    public String getAgencyId() { return agencyId; }
    public String getAgencySecret() { return agencySecret; }
    public String getOfferId() { return offerId; }
    public String getRequestId() { return requestId; }
    public String getTravelerFirstName() { return travelerFirstName; }
    public String getTravelerLastName() { return travelerLastName; }
    public String getCardNumber() { return cardNumber; }
    public int getExpiryMonth() { return expiryMonth; }
    public int getExpiryYear() { return expiryYear; }
    public String getCvc() { return cvc; }
}
