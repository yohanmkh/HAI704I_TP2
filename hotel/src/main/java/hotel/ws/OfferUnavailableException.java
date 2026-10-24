package hotel.ws;

import jakarta.xml.ws.WebFault;

@WebFault(name = "OfferUnavailableFault", targetNamespace = "http://ws.hotel/")
public class OfferUnavailableException extends Exception {
    private static final long serialVersionUID = 1L;
    private final OfferUnavailableFault faultInfo;

    public OfferUnavailableException(String message, OfferUnavailableFault faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    public OfferUnavailableException(String message, OfferUnavailableFault faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    public OfferUnavailableFault getFaultInfo() {
        return faultInfo;
    }
}
