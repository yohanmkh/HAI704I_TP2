package hotel.ws;

import hotel.dto.ReservationConfirmation;
import hotel.dto.ReservationRequest;
import hotel.dto.RoomOffer;
import hotel.dto.SearchRequest;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;

import java.util.List;

/**
 * Service Endpoint Interface (SEI) defining the SOAP web service contract.
 * Exposes coarse-grained operations for room searching and booking.
 */
@WebService(name = "HotelWebService", targetNamespace = "http://ws.hotel/")
public interface HotelWebService {

    @WebMethod(operationName = "searchRooms")
    @WebResult(name = "roomOffer")
    List<RoomOffer> searchRooms(@WebParam(name = "searchRequest") SearchRequest request)
            throws UnauthorizedAgencyException;

    @WebMethod(operationName = "reserve")
    @WebResult(name = "reservationConfirmation")
    ReservationConfirmation reserve(@WebParam(name = "reservationRequest") ReservationRequest req)
            throws UnauthorizedAgencyException,
                   OfferNotFoundException,
                   PaymentValidationException,
                   OfferUnavailableException;
}
