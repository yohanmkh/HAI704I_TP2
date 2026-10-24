package agence.client;

import agence.generated.HotelService;
import agence.generated.HotelWebService;
import agence.generated.OfferNotFoundException;
import agence.generated.OfferUnavailableException;
import agence.generated.PaymentValidationException;
import agence.generated.ReservationConfirmation;
import agence.generated.ReservationRequest;
import agence.generated.RoomOffer;
import agence.generated.SearchRequest;
import agence.generated.UnauthorizedAgencyException;
import jakarta.xml.ws.BindingProvider;

import java.util.List;

public class HotelSoapClient {

    private final String url;
    private final HotelWebService port;

    public HotelSoapClient(String url) {
        this.url = url;

        HotelService svc = new HotelService();
        this.port = svc.getHotelPort();

        // config dynamique de l'adresse du service
        BindingProvider bp = (BindingProvider) this.port;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, this.url);

        // timeouts pour pas bloquer si un hôtel est éteint
        bp.getRequestContext().put("com.sun.xml.ws.connect.timeout", 3000);
        bp.getRequestContext().put("com.sun.xml.ws.request.timeout", 5000);
        bp.getRequestContext().put("jakarta.xml.ws.client.connectionTimeout", 3000);
        bp.getRequestContext().put("jakarta.xml.ws.client.receiveTimeout", 5000);
    }

    public List<RoomOffer> searchRooms(SearchRequest req) throws UnauthorizedAgencyException {
        return port.searchRooms(req);
    }

    public ReservationConfirmation reserve(ReservationRequest req)
            throws UnauthorizedAgencyException,
                   OfferNotFoundException,
                   PaymentValidationException,
                   OfferUnavailableException {
        return port.reserve(req);
    }

    public String getUrl() {
        return url;
    }
}
