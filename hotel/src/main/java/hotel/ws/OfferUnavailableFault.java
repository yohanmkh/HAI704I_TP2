package hotel.ws;

import java.io.Serializable;

public class OfferUnavailableFault implements Serializable {
    private static final long serialVersionUID = 1L;
    private String offerId;
    private String reason;
    private String message;

    public OfferUnavailableFault() {}
    public OfferUnavailableFault(String offerId, String reason, String message) {
        this.offerId = offerId;
        this.reason = reason;
        this.message = message;
    }

    public String getOfferId() { return offerId; }
    public void setOfferId(String offerId) { this.offerId = offerId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
