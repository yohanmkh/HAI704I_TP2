package hotel.ws;

import hotel.repo.HotelRepository;
import hotel.repo.InMemoryHotelRepository;
import hotel.service.HotelService;
import jakarta.xml.ws.Endpoint;

public class HotelPublisher {

    public static void main(String[] args) {
        String portStr = System.getProperty("port", (args.length > 0) ? args[0] : "8080");
        String profile = System.getProperty("profile", (args.length > 1) ? args[1] : "hotel1");

        int port = Integer.parseInt(portStr.trim());
        String url = "http://localhost:" + port + "/hotel";

        // init composants
        HotelRepository repo = new InMemoryHotelRepository(profile);
        HotelService service = new HotelService(repo);
        HotelEndpoint endpoint = new HotelEndpoint(service);

        Endpoint.publish(url, endpoint);
        System.out.println("Hôtel (" + profile + ") démarré sur " + url);
        System.out.println("WSDL : " + url + "?wsdl");
    }
}
