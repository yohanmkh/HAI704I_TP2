package hotel.service;

public class OfferUnavailableDomainException extends Exception {
    private static final long serialVersionUID = 1L;
    private final String offerId;
    private final String reason;

    public OfferUnavailableDomainException(String offerId, String reason, String message) {
        super(message);
        this.offerId = offerId;
        this.reason = reason;
    }

    public String getOfferId() { return offerId; }
    public String getReason() { return reason; }
}
