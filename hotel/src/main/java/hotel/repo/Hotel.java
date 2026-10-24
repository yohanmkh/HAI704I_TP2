package hotel.repo;

import java.util.ArrayList;
import java.util.List;

public class Hotel {
    private String id;
    private String name;
    private String city;
    private int stars;
    private List<Room> rooms;

    public Hotel(String id, String name, String city, int stars) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.stars = stars;
        this.rooms = new ArrayList<>();
    }

    public Hotel(String id, String name, String city, int stars, List<Room> rooms) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.stars = stars;
        this.rooms = rooms;
    }

    public void addRoom(Room room) {
        this.rooms.add(room);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public int getStars() {
        return stars;
    }

    public List<Room> getRooms() {
        return rooms;
    }
}
