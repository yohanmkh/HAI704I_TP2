package hotel.repo;

public class Room {
    private String id;
    private String type;
    private int capacity;
    private double pricePerNight;

    public Room(String id, String type, int capacity, double pricePerNight) {
        this.id = id;
        this.type = type;
        this.capacity = capacity;
        this.pricePerNight = pricePerNight;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public int getCapacity() {
        return capacity;
    }

    public double getPricePerNight() {
        return pricePerNight;
    }
}
