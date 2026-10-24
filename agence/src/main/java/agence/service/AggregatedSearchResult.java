package agence.service;

import agence.generated.RoomOffer;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of a multi-hotel search.
 * Contains aggregated offers (sorted by price) and warnings for any unreachable hotels.
 */
public class AggregatedSearchResult {
    private final List<RoomOffer> offers = new ArrayList<>();
    private final List<String> unreachableHotels = new ArrayList<>();

    public void addOffers(List<RoomOffer> newOffers) {
        if (newOffers != null) {
            this.offers.addAll(newOffers);
        }
    }

    public void addUnreachableHotel(String hotelUrl, String reason) {
        this.unreachableHotels.add(hotelUrl + " (Warning: " + reason + ")");
    }

    public List<RoomOffer> getOffers() {
        return offers;
    }

    public List<String> getUnreachableHotels() {
        return unreachableHotels;
    }

    public boolean hasWarnings() {
        return !unreachableHotels.isEmpty();
    }
}
