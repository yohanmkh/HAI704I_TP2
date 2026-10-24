package hotel.dto;

import java.io.Serializable;

/**
 * Data Transfer Object representing an individual hotel room offer.
 * Bundles all necessary details into a coarse-grained response.
 */
public class RoomOffer implements Serializable {
    private static final long serialVersionUID = 1L;

    private String offerId;
    private String hotelName;
    private String roomType;
    private double pricePerNight;
    private int stars;
    private double totalPrice;

    public RoomOffer() {
    }

    public RoomOffer(String offerId, String hotelName, String roomType, double pricePerNight, int stars, double totalPrice) {
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

    public void setOfferId(String offerId) {
        this.offerId = offerId;
    }

    public String getHotelName() {
        return hotelName;
    }

    public void setHotelName(String hotelName) {
        this.hotelName = hotelName;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public double getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(double pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    public int getStars() {
        return stars;
    }

    public void setStars(int stars) {
        this.stars = stars;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    @Override
    public String toString() {
        return "RoomOffer{" +
                "offerId='" + offerId + '\'' +
                ", hotelName='" + hotelName + '\'' +
                ", roomType='" + roomType + '\'' +
                ", pricePerNight=" + pricePerNight +
                ", stars=" + stars +
                ", totalPrice=" + totalPrice +
                '}';
    }
}
