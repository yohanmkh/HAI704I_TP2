package agence.service;

/**
 * Clean UI/Business model representing an offer presented to the user.
 * Independent of generated SOAP stubs.
 */
public class AgencyOffer {
    private final int number;
    private final String offerId;
    private final String hotelUrl;
    private final String hotelName;
    private final String roomType;
    private final int stars;
    private final double pricePerNight;
    private final double totalPrice;
    private final String checkIn;
    private final String checkOut;

    public AgencyOffer(int number, String offerId, String hotelUrl, String hotelName,
                       String roomType, int stars, double pricePerNight, double totalPrice,
                       String checkIn, String checkOut) {
        this.number = number;
        this.offerId = offerId;
        this.hotelUrl = hotelUrl;
        this.hotelName = hotelName;
        this.roomType = roomType;
        this.stars = stars;
        this.pricePerNight = pricePerNight;
        this.totalPrice = totalPrice;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
    }

    public int getNumber() { return number; }
    public String getOfferId() { return offerId; }
    public String getHotelUrl() { return hotelUrl; }
    public String getHotelName() { return hotelName; }
    public String getRoomType() { return roomType; }
    public int getStars() { return stars; }
    public double getPricePerNight() { return pricePerNight; }
    public double getTotalPrice() { return totalPrice; }
    public String getCheckIn() { return checkIn; }
    public String getCheckOut() { return checkOut; }
}
