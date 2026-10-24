package hotel.dto;

import java.io.Serializable;

/**
 * Transfer object carrying payment information on the wire.
 * Note: Never log this object directly. CVC is discarded immediately upon validation.
 */
public class PaymentDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String cardNumber;
    private int expiryMonth;
    private int expiryYear;
    private String cvc;

    public PaymentDto() {
    }

    public PaymentDto(String cardNumber, int expiryMonth, int expiryYear, String cvc) {
        this.cardNumber = cardNumber;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.cvc = cvc;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public int getExpiryMonth() {
        return expiryMonth;
    }

    public void setExpiryMonth(int expiryMonth) {
        this.expiryMonth = expiryMonth;
    }

    public int getExpiryYear() {
        return expiryYear;
    }

    public void setExpiryYear(int expiryYear) {
        this.expiryYear = expiryYear;
    }

    public String getCvc() {
        return cvc;
    }

    public void setCvc(String cvc) {
        this.cvc = cvc;
    }

    @Override
    public String toString() {
        // Safe string representation avoiding leaking sensitive PAN or CVC
        String masked = (cardNumber != null && cardNumber.length() >= 4)
                ? "****-****-****-" + cardNumber.substring(cardNumber.length() - 4)
                : "****";
        return "PaymentDto{cardNumber='" + masked + "', expiry=" + expiryMonth + "/" + expiryYear + "}";
    }
}
