package hotel.ws;

import java.io.Serializable;
import jakarta.xml.ws.WebFault;

@WebFault(name = "PaymentValidationFault", targetNamespace = "http://ws.hotel/")
public class PaymentValidationException extends Exception implements Serializable {
    private static final long serialVersionUID = 1L;
    private final PaymentValidationFault faultInfo;

    public PaymentValidationException(String message, PaymentValidationFault faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    public PaymentValidationException(String message, PaymentValidationFault faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    public PaymentValidationFault getFaultInfo() {
        return faultInfo;
    }
}
