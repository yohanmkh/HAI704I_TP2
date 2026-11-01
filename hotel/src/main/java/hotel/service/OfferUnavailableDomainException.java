package hotel.service;

import java.io.Serializable;

public class OfferUnavailableDomainException extends Exception implements Serializable {
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
