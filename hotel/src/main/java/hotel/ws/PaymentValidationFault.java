package hotel.ws;

import java.io.Serializable;

public class PaymentValidationFault implements Serializable {
    private static final long serialVersionUID = 1L;
    private String reason;
    private String message;

    public PaymentValidationFault() {}
    public PaymentValidationFault(String reason, String message) {
        this.reason = reason;
        this.message = message;
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
