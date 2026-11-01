package agence.cli;

import agence.service.AgencyOffer;
import agence.service.AgencySearchResult;
import agence.service.AgencyService;
import agence.service.ReservationReceipt;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class AgencyCli {

    private final AgencyService service;
    private final Scanner scanner = new Scanner(System.in);

    public AgencyCli(AgencyService service) {
        this.service = service;
    }

    public void run() {
        while (true) {
            System.out.println("\n1. Chercher un séjour");
            System.out.println("2. Quitter");
            System.out.print("Choix : ");
            String choix = scanner.nextLine().trim();

            if ("1".equals(choix)) {
                chercherEtReserver();
            } else if ("2".equals(choix)) {
                break;
            } else {
                System.out.println("Erreur : choix invalide.");
            }
        }
    }

    private void chercherEtReserver() {
        System.out.print("\nVille [Montpellier] : ");
        String ville = scanner.nextLine().trim();
        if (ville.isEmpty()) {
            ville = "Montpellier";
        }

        LocalDate in = lireDate("Date arrivée (YYYY-MM-DD) [2026-10-31] : ", LocalDate.of(2026, 10, 31));
        LocalDate out = lireDate("Date départ (YYYY-MM-DD) [2026-11-03] : ", in.plusDays(3));

        if (!out.isAfter(in)) {
            System.out.println("Erreur : la date de départ doit être après la date d'arrivée.");
            return;
        }

        int personnes = lireEntier("Nombre de personnes [2] : ", 2);
        int etoiles = lireEntier("Étoiles minimum [3] : ", 3);
        double maxPrix = lireDouble("Prix max par nuit (0 si pas de limite) [0] : ", 0.0);

        AgencySearchResult res = service.searchStays(ville, in.toString(), out.toString(), personnes, etoiles, maxPrix);

        // affiche les alertes si un hôtel est down
        if (res.hasWarnings()) {
            for (String w : res.getWarnings()) {
                System.out.println("Avertissement : " + w);
            }
        }

        List<AgencyOffer> offres = res.getOffers();
        if (offres.isEmpty()) {
            System.out.println("Aucune offre disponible.");
            return;
        }

        System.out.println("\nOffres disponibles :");
        for (AgencyOffer o : offres) {
            System.out.printf("%d. %s - %s (%d*) : %.2f€/nuit - Total : %.2f€\n",
                    o.getNumber(), o.getHotelName(), o.getRoomType(), o.getStars(),
                    o.getPricePerNight(), o.getTotalPrice());
        }

        int index = lireEntier("\nNuméro d'offre à réserver (0 pour annuler) : ", 0);
        if (index <= 0 || index > offres.size()) {
            if (index != 0) {
                System.out.println("Erreur : numéro d'offre hors liste.");
            }
            return;
        }

        System.out.print("Prénom du voyageur : ");
        String prenom = scanner.nextLine().trim();
        System.out.print("Nom du voyageur : ");
        String nom = scanner.nextLine().trim();

        System.out.print("Numéro de carte [4532 0150 9876 4321] : ");
        String carte = scanner.nextLine().trim();
        if (carte.isEmpty()) {
            carte = "4532 0150 9876 4321";
        }

        int mois = lireEntier("Mois expiration (1-12) [12] : ", 12);
        int annee = lireEntier("Année expiration [2028] : ", 2028);
        System.out.print("CVC : ");
        String cvc = scanner.nextLine().trim();
        if (cvc.isEmpty()) {
            cvc = "123";
        }

        ReservationReceipt recu = service.bookOffer(index, prenom, nom, carte, mois, annee, cvc);

        if (recu.isSuccess()) {
            System.out.println("Réservation confirmée : " + recu.getReference() +
                    " (" + recu.getTotalPrice() + "€, payé avec carte " + recu.getMaskedCard() + ")");
            if (recu.isReplayed()) {
                System.out.println("Note : rejeu de demande, réservation déjà existante.");
            }
        } else {
            System.out.println("Erreur : " + recu.getErrorMessage());
        }
    }

    private LocalDate lireDate(String label, LocalDate defaut) {
        while (true) {
            System.out.print(label);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return defaut;
            }
            try {
                return LocalDate.parse(input);
            } catch (DateTimeParseException e) {
                System.out.println("Erreur : format de date invalide (attendu : YYYY-MM-DD).");
            }
        }
    }

    private int lireEntier(String label, int defaut) {
        while (true) {
            System.out.print(label);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return defaut;
            }
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Erreur : veuillez entrer un entier.");
            }
        }
    }

    private double lireDouble(String label, double defaut) {
        while (true) {
            System.out.print(label);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return defaut;
            }
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                System.out.println("Erreur : veuillez entrer un nombre.");
            }
        }
    }
}
