package hotel.service;

/**
 * Pure Java domain exception indicating that an agency is not a recognized partner.
 * Zero JAX-WS annotations.
 */
public class UnauthorizedAgencyDomainException extends Exception {
    private static final long serialVersionUID = 1L;
    private final String agencyId;

    public UnauthorizedAgencyDomainException(String agencyId, String message) {
        super(message);
        this.agencyId = agencyId;
    }

    public String getAgencyId() {
        return agencyId;
    }
}
