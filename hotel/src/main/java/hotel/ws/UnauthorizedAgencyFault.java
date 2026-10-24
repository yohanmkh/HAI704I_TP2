package hotel.ws;

import java.io.Serializable;

/**
 * Fault Bean describing the details of an unauthorized agency refusal in the SOAP fault detail.
 */
public class UnauthorizedAgencyFault implements Serializable {
    private static final long serialVersionUID = 1L;

    private String message;
    private String agencyId;

    public UnauthorizedAgencyFault() {
    }

    public UnauthorizedAgencyFault(String message, String agencyId) {
        this.message = message;
        this.agencyId = agencyId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAgencyId() {
        return agencyId;
    }

    public void setAgencyId(String agencyId) {
        this.agencyId = agencyId;
    }
}
