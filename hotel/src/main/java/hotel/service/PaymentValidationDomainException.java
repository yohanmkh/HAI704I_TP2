package hotel.service;

public class PaymentValidationDomainException extends Exception {
    private static final long serialVersionUID = 1L;
    private final String reason;

    public PaymentValidationDomainException(String reason, String message) {
        super(message);
        this.reason = reason;
    }

    public String getReason() { return reason; }
}
