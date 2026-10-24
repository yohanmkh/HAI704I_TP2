package hotel.service;

public class OfferNotFoundDomainException extends Exception {
    private static final long serialVersionUID = 1L;
    private final String offerId;

    public OfferNotFoundDomainException(String offerId, String message) {
        super(message);
        this.offerId = offerId;
    }

    public String getOfferId() { return offerId; }
}
