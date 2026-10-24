package hotel.ws;

import java.io.Serializable;

public class OfferNotFoundFault implements Serializable {
    private static final long serialVersionUID = 1L;
    private String offerId;
    private String message;

    public OfferNotFoundFault() {}
    public OfferNotFoundFault(String offerId, String message) {
        this.offerId = offerId;
        this.message = message;
    }

    public String getOfferId() { return offerId; }
    public void setOfferId(String offerId) { this.offerId = offerId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
