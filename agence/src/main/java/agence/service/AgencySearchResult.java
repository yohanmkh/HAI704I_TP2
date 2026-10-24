package agence.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AgencySearchResult {
    private final List<AgencyOffer> offers = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public void addOffer(AgencyOffer offer) {
        this.offers.add(offer);
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    public List<AgencyOffer> getOffers() {
        return Collections.unmodifiableList(offers);
    }

    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}
