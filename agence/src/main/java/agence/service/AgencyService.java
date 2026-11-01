package agence.service;

import agence.client.HotelSoapClient;
import agence.generated.OfferNotFoundException;
import agence.generated.OfferUnavailableException;
import agence.generated.PaymentDto;
import agence.generated.PaymentValidationException;
import agence.generated.ReservationConfirmation;
import agence.generated.ReservationRequest;
import agence.generated.RoomOffer;
import agence.generated.SearchRequest;
import agence.generated.TravelerDto;
import agence.generated.UnauthorizedAgencyException;

import java.util.*;

public class AgencyService {

    private final String agencyId;
    private final String agencySecret;
    private final Map<String, HotelSoapClient> clients = new LinkedHashMap<>();

    // cache des offres trouvées lors de la dernière recherche
    private final Map<Integer, AgencyOffer> offresCourantes = new HashMap<>();

    public AgencyService(String agencyId, String agencySecret, List<String> hotelUrls) {
        this.agencyId = agencyId;
        this.agencySecret = agencySecret;
        if (hotelUrls != null) {
            for (String u : hotelUrls) {
                this.clients.put(u, new HotelSoapClient(u));
            }
        }
    }

    public AgencySearchResult searchStays(String ville, String checkIn, String checkOut,
                                          int personnes, int etoiles, double maxPrix) {
        AgencySearchResult res = new AgencySearchResult();
        offresCourantes.clear();

        SearchRequest req = new SearchRequest();
        req.setAgencyId(this.agencyId);
        req.setCity(ville);
        req.setCheckIn(checkIn);
        req.setCheckOut(checkOut);
        req.setGuests(personnes);
        req.setMinStars(etoiles);
        req.setMaxPrice(maxPrix);

        List<RawOffer> brutes = new ArrayList<>();

        for (Map.Entry<String, HotelSoapClient> entry : clients.entrySet()) {
            String url = entry.getKey();
            HotelSoapClient client = entry.getValue();

            try {
                List<RoomOffer> rep = client.searchRooms(req);
                if (rep != null) {
                    for (RoomOffer o : rep) {
                        brutes.add(new RawOffer(url, o));
                    }
                }
            } catch (UnauthorizedAgencyException e) {
                res.addWarning(url + " : agence non reconnue (" + e.getMessage() + ")");
            } catch (Exception e) {
                // si un hôtel répond pas, on l'ajoute aux warnings sans planter
                res.addWarning(url + " : indisponible (" + e.getMessage() + ")");
            }
        }

        // on trie par prix total croissant
        brutes.sort(Comparator.comparingDouble(b -> b.offer.getTotalPrice()));

        int i = 1;
        for (RawOffer b : brutes) {
            AgencyOffer o = new AgencyOffer(
                    i, b.offer.getOfferId(), b.url, b.offer.getHotelName(),
                    b.offer.getRoomType(), b.offer.getStars(),
                    b.offer.getPricePerNight(), b.offer.getTotalPrice(),
                    checkIn, checkOut
            );
            offresCourantes.put(i, o);
            res.addOffer(o);
            i++;
        }

        return res;
    }

    public ReservationReceipt bookOffer(int num, String prenom, String nom,
                                        String carte, int mois, int annee, String cvc) {

        AgencyOffer o = offresCourantes.get(num);
        if (o == null) {
            return ReservationReceipt.failure("Numéro d'offre invalide : #" + num);
        }

        HotelSoapClient client = clients.get(o.getHotelUrl());
        if (client == null) {
            return ReservationReceipt.failure("Hôtel non accessible : " + o.getHotelUrl());
        }

        TravelerDto traveler = new TravelerDto();
        traveler.setFirstName(prenom);
        traveler.setLastName(nom);

        PaymentDto payment = new PaymentDto();
        payment.setCardNumber(carte);
        payment.setExpiryMonth(mois);
        payment.setExpiryYear(annee);
        payment.setCvc(cvc);

        ReservationRequest req = new ReservationRequest();
        req.setAgencyId(this.agencyId);
        req.setAgencySecret(this.agencySecret);
        req.setOfferId(o.getOfferId());
        req.setRequestId(UUID.randomUUID().toString());
        req.setTraveler(traveler);
        req.setPayment(payment);

        try {
            ReservationConfirmation conf = client.reserve(req);
            String dates = conf.getCheckIn() + " au " + conf.getCheckOut();
            return ReservationReceipt.success(
                    conf.getReservationReference(),
                    conf.getHotelName(),
                    conf.getRoomType(),
                    dates,
                    conf.getTotalPrice(),
                    conf.getMaskedCardNumber(),
                    conf.getTravelerName(),
                    conf.isReplayed()
            );

        } catch (OfferUnavailableException e) {
            String msg = (e.getMessage() != null && !e.getMessage().isBlank()) ? e.getMessage() : "Chambre déjà réservée.";
            return ReservationReceipt.failure("Cette offre n'est plus disponible : " + msg);

        } catch (PaymentValidationException e) {
            return ReservationReceipt.failure("Paiement refusé : " + e.getMessage());

        } catch (OfferNotFoundException e) {
            return ReservationReceipt.failure("Offre introuvable ou expirée : " + e.getMessage());

        } catch (UnauthorizedAgencyException e) {
            return ReservationReceipt.failure("Authentification refusée par l'hôtel : " + e.getMessage());

        } catch (Exception e) {
            return ReservationReceipt.failure("Erreur de communication avec l'hôtel : " + e.getMessage());
        }
    }

    private static class RawOffer {
        final String url;
        final RoomOffer offer;

        RawOffer(String url, RoomOffer offer) {
            this.url = url;
            this.offer = offer;
        }
    }
}
