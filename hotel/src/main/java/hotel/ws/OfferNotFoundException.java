package hotel.ws;

import java.io.Serializable;
import jakarta.xml.ws.WebFault;

@WebFault(name = "OfferNotFoundFault", targetNamespace = "http://ws.hotel/")
public class OfferNotFoundException extends Exception implements Serializable {
    private static final long serialVersionUID = 1L;
    private final OfferNotFoundFault faultInfo;

    public OfferNotFoundException(String message, OfferNotFoundFault faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    public OfferNotFoundException(String message, OfferNotFoundFault faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    public OfferNotFoundFault getFaultInfo() {
        return faultInfo;
    }
}
