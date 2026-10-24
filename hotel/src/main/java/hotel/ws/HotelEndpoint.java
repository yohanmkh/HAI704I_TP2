package hotel.ws;

import hotel.dto.PaymentDto;
import hotel.dto.ReservationConfirmation;
import hotel.dto.ReservationRequest;
import hotel.dto.RoomOffer;
import hotel.dto.SearchRequest;
import hotel.dto.TravelerDto;
import hotel.model.HotelOffer;
import hotel.model.Reservation;
import hotel.model.ReservationCommand;
import hotel.model.ReservationResult;
import hotel.model.SearchCriteria;
import hotel.repo.InMemoryHotelRepository;
import hotel.service.HotelService;
import hotel.service.OfferNotFoundDomainException;
import hotel.service.OfferUnavailableDomainException;
import hotel.service.PaymentValidationDomainException;
import hotel.service.UnauthorizedAgencyDomainException;
import jakarta.jws.WebService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@WebService(
        endpointInterface = "hotel.ws.HotelWebService",
        serviceName = "HotelService",
        portName = "HotelPort",
        targetNamespace = "http://ws.hotel/"
)
public class HotelEndpoint implements HotelWebService {

    private final HotelService service;

    public HotelEndpoint() {
        this(new HotelService(new InMemoryHotelRepository()));
    }

    public HotelEndpoint(HotelService service) {
        this.service = service;
    }

    @Override
    public List<RoomOffer> searchRooms(SearchRequest req) throws UnauthorizedAgencyException {
        SearchCriteria crit = toCriteria(req);
        try {
            List<HotelOffer> domainOffers = service.searchRooms(crit);
            List<RoomOffer> dtos = new ArrayList<>();
            for (HotelOffer o : domainOffers) {
                dtos.add(new RoomOffer(o.getOfferId(), o.getHotelName(), o.getRoomType(), o.getPricePerNight(), o.getStars(), o.getTotalPrice()));
            }
            return dtos;
        } catch (UnauthorizedAgencyDomainException e) {
            throw new UnauthorizedAgencyException(e.getMessage(), new UnauthorizedAgencyFault(e.getMessage(), e.getAgencyId()));
        }
    }

    @Override
    public ReservationConfirmation reserve(ReservationRequest req)
            throws UnauthorizedAgencyException,
                   OfferNotFoundException,
                   PaymentValidationException,
                   OfferUnavailableException {

        if (req == null) {
            throw new IllegalArgumentException("Requête nulle");
        }

        TravelerDto t = req.getTraveler();
        PaymentDto p = req.getPayment();

        ReservationCommand cmd = new ReservationCommand(
                req.getAgencyId(),
                req.getAgencySecret(),
                req.getOfferId(),
                req.getRequestId(),
                t != null ? t.getFirstName() : "",
                t != null ? t.getLastName() : "",
                p != null ? p.getCardNumber() : "",
                p != null ? p.getExpiryMonth() : 0,
                p != null ? p.getExpiryYear() : 0,
                p != null ? p.getCvc() : ""
        );

        try {
            ReservationResult res = service.reserve(cmd);
            Reservation r = res.getReservation();

            return new ReservationConfirmation(
                    r.getReservationReference(),
                    r.getHotelName(),
                    r.getRoomType(),
                    r.getCheckIn().toString(),
                    r.getCheckOut().toString(),
                    r.getTotalPrice(),
                    r.getMaskedCardNumber(),
                    r.getTravelerName(),
                    res.isReplayed()
            );

        } catch (UnauthorizedAgencyDomainException e) {
            throw new UnauthorizedAgencyException(e.getMessage(), new UnauthorizedAgencyFault(e.getMessage(), e.getAgencyId()));
        } catch (OfferNotFoundDomainException e) {
            throw new OfferNotFoundException(e.getMessage(), new OfferNotFoundFault(e.getOfferId(), e.getMessage()));
        } catch (PaymentValidationDomainException e) {
            throw new PaymentValidationException(e.getMessage(), new PaymentValidationFault(e.getReason(), e.getMessage()));
        } catch (OfferUnavailableDomainException e) {
            throw new OfferUnavailableException(e.getMessage(), new OfferUnavailableFault(e.getOfferId(), e.getReason(), e.getMessage()));
        }
    }

    private SearchCriteria toCriteria(SearchRequest req) {
        if (req == null) return null;
        LocalDate in = parse(req.getCheckIn());
        LocalDate out = parse(req.getCheckOut());
        return new SearchCriteria(req.getAgencyId(), req.getCity(), in, out, req.getGuests(), req.getMinStars(), req.getMaxPrice());
    }

    private LocalDate parse(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
