package agence.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Loads agency configuration supporting multiple hotel URLs.
 * Overridable dynamically via -Dhotel.urls=... or -Dagency.id=...
 */
public class AgencyConfig {
    private static final String DEFAULT_PROPERTIES_FILE = "/agency.properties";

    private final String agencyId;
    private final String agencySecret;
    private final List<String> hotelUrls = new ArrayList<>();

    public AgencyConfig() {
        Properties props = new Properties();
        try (InputStream in = getClass().getResourceAsStream(DEFAULT_PROPERTIES_FILE)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            System.err.println("Warning: could not load " + DEFAULT_PROPERTIES_FILE + ": " + e.getMessage());
        }

        this.agencyId = System.getProperty("agency.id", props.getProperty("agency.id", "AGENCY_VOYAGE_PRO"));
        this.agencySecret = System.getProperty("agency.secret", props.getProperty("agency.secret", "secret_voyage_123"));

        String rawUrls = System.getProperty("hotel.urls", props.getProperty("hotel.urls", "http://localhost:8080/hotel"));
        for (String u : rawUrls.split(",")) {
            if (!u.trim().isEmpty()) {
                hotelUrls.add(u.trim());
            }
        }
    }

    public AgencyConfig(String agencyId, String agencySecret, List<String> hotelUrls) {
        this.agencyId = agencyId;
        this.agencySecret = agencySecret;
        if (hotelUrls != null) {
            this.hotelUrls.addAll(hotelUrls);
        }
    }

    public String getAgencyId() {
        return agencyId;
    }

    public String getAgencySecret() {
        return agencySecret;
    }

    public List<String> getHotelUrls() {
        return hotelUrls;
    }
}
