package hotel.ws;

import java.io.Serializable;
import jakarta.xml.ws.WebFault;

/**
 * Checked business exception mapped to a WSDL &lt;fault&gt; via @WebFault.
 */
@WebFault(name = "UnauthorizedAgencyFault", targetNamespace = "http://ws.hotel/")
public class UnauthorizedAgencyException extends Exception implements Serializable {
    private static final long serialVersionUID = 1L;

    private final UnauthorizedAgencyFault faultInfo;

    public UnauthorizedAgencyException(String message, UnauthorizedAgencyFault faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    public UnauthorizedAgencyException(String message, UnauthorizedAgencyFault faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    public UnauthorizedAgencyFault getFaultInfo() {
        return faultInfo;
    }
}
