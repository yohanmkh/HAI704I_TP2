package hotel.model;

/**
 * Domain model representing a calculated room offer within the business service.
 */
public class HotelOffer {
    private final String offerId;
    private final String hotelName;
    private final String roomType;
    private final double pricePerNight;
    private final int stars;
    private final double totalPrice;

    public HotelOffer(String offerId, String hotelName, String roomType, double pricePerNight, int stars, double totalPrice) {
        this.offerId = offerId;
        this.hotelName = hotelName;
        this.roomType = roomType;
        this.pricePerNight = pricePerNight;
        this.stars = stars;
        this.totalPrice = totalPrice;
    }

    public String getOfferId() {
        return offerId;
    }

    public String getHotelName() {
        return hotelName;
    }

    public String getRoomType() {
        return roomType;
    }

    public double getPricePerNight() {
        return pricePerNight;
    }

    public int getStars() {
        return stars;
    }

    public double getTotalPrice() {
        return totalPrice;
    }
}
