# HAI704I — TP2 : Services Web SOAP / WSDL

**Binôme :** Makhbouche Oussama (GL) & Laouici Heythem (GL)  
**Université de Montpellier — Master 1 Informatique (2026–2027)**

Application distribuée de recherche et de réservation hôtelière en SOAP / WSDL avec Jakarta XML Web Services (Metro 4.0.5).

---

## 1. Prérequis

- **Java :** JDK 17 ou supérieur
- **Maven :** 3.8 ou supérieur

Vérification rapide :
```bash
java -version
mvn -version
```

---

## 2. Lancement rapide

Trois scripts sont fournis à la racine pour lancer les deux hôtels et l'agence dans trois terminaux séparés :

```bash
# Terminal 1 : Hôtel 1 sur le port 8080
./start-hotel1.sh

# Terminal 2 : Hôtel 2 sur le port 8081
./start-hotel2.sh

# Terminal 3 : Client Agence (CLI)
./start-agence.sh
```

> **Important :** L'Hôtel 1 (`port 8080`) doit être démarré avant de compiler l'agence pour la première fois, car le plugin Maven `wsimport` récupère le contrat sur `http://localhost:8080/hotel?wsdl` pour générer les classes clientes.

---

## 3. Compilation et exécution manuelle (sans scripts)

### 1. Démarrer l'Hôtel 1 (port 8080)
```bash
cd hotel
mvn clean compile
mvn exec:exec -Dport=8080 -Dprofile=hotel1
```
WSDL publié sur : `http://localhost:8080/hotel?wsdl`

### 2. Démarrer l'Hôtel 2 (port 8081)
Dans un second terminal :
```bash
cd hotel
mvn exec:exec -Dport=8081 -Dprofile=hotel2
```
WSDL publié sur : `http://localhost:8081/hotel?wsdl`

### 3. Compiler et lancer l'Agence
Dans un troisième terminal :
```bash
cd agence
mvn clean compile
mvn exec:exec
```

### 4. Lancer les tests unitaires du service hôtel
Les tests JUnit 5 vérifient `HotelService` directement en mémoire sans démarrer de serveur SOAP :
```bash
cd hotel
mvn test
```

---

## 4. Structure du dépôt

```text
HAI704I_TP2/
├── hotel/                              # Serveur SOAP Hôtel
│   ├── pom.xml
│   └── src/
│       ├── main/java/hotel/
│       │   ├── dto/                    # DTOs JAXB (SearchRequest, RoomOffer, ReservationRequest...)
│       │   ├── model/                  # Objets métier (Quote, Reservation, SearchCriteria)
│       │   ├── repo/                   # HotelRepository + InMemoryHotelRepository (BCrypt, devis)
│       │   ├── service/                # HotelService (métier pur, zéro import jakarta.xml.ws)
│       │   └── ws/                     # HotelEndpoint (@WebService), exceptions @WebFault, Publisher
│       └── test/java/hotel/service/    # Tests unitaires JUnit 5
│
├── agence/                             # Client SOAP Agence (aucune dépendance vers hotel/)
│   ├── pom.xml                         # Configuration jaxws-maven-plugin (wsimport)
│   └── src/
│       ├── main/resources/
│       │   └── agency.properties       # Identifiants agence et liste des URLs d'hôtels
│       └── main/java/agence/
│           ├── client/                 # HotelSoapClient (BindingProvider, timeouts 3s/5s) + AgencyConfig
│           ├── service/                # AgencyService (agrégation multi-hôtels, tri par prix)
│           ├── cli/                    # AgencyCli (menu console, zéro import SOAP/WSDL)
│           └── AgencyApp.java          # Point d'entrée
│
├── captures/                           # Captures XML (WSDL, XSD, requêtes, réponses, fautes)
├── Compte-Rendu.md                     # Rapport technique complet
├── start-hotel1.sh                     # Lancement Hôtel 1 (port 8080)
├── start-hotel2.sh                     # Lancement Hôtel 2 (port 8081)
└── start-agence.sh                     # Lancement Agence CLI
```

---

## 5. Données préchargées (Jeu d'essai)

### Chambres par profil d'hôtel
- **Profil `hotel1` (port 8080) :**
  - *Grand Hotel Montpellier* (4*) : `Single Comfort` (1 pers., 100 €/nuit), `Double Deluxe` (2 pers., 150 €/nuit), `Family Suite` (4 pers., 250 €/nuit)
  - *Montpellier City Centre* (3*) : `Standard Double` (2 pers., 90 €/nuit)
- **Profil `hotel2` (port 8081) :**
  - *Hôtel de la Comédie* (4*) : `Superior Classic` (2 pers., 130 €/nuit), `Executive King` (2 pers., 190 €/nuit), `Panoramique Suite` (4 pers., 310 €/nuit)
  - *Le Mas Saint-Roch* (5*) : `Royal Villa` (4 pers., 520 €/nuit)

### Agences partenaires et remises
Les secrets sont stockés sous forme de hachages BCrypt côté serveur :

| Identifiant (`agencyId`) | Secret (`agencySecret`) | Remise Hôtel 1 | Remise Hôtel 2 |
| :--- | :--- | :--- | :--- |
| `AGENCY_VOYAGE_PRO` *(défaut)* | `secret_voyage_123` | 15 % | 5 % |
| `AGENCY_SELECTOUR` | `secret_selectour_456` | 5 % | 20 % |
| `AGENCY_BOOKING_EXP` | `secret_booking_789` | 10 % | 15 % |

---

## 6. Scénarios de test dans la CLI

Au lancement de `./start-agence.sh`, appuyez sur **Entrée** sur les champs entre crochets `[...]` pour accepter les valeurs par défaut.

### 1. Recherche et réservation nominale
1. Choisir `1` (Chercher un séjour).
2. Laisser les valeurs par défaut (`Montpellier`, dates proposées, `2` personnes, `3` étoiles, plafond `300` €/nuit).
3. Les offres des deux hôtels s'affichent triées par prix total croissant.
4. Saisir le numéro d'une offre (ex. `1`), entrer le prénom/nom du voyageur et valider les informations de carte.
5. La référence de confirmation et la carte masquée s'affichent :
   ```text
   Réservation confirmée : RES-4F52E255 (382.5€, payé avec carte ****-****-****-4321)
   ```

### 2. Panne partielle d'un hôtel (Checkpoint F)
1. Arrêter l'Hôtel 2 (`Ctrl+C` dans le terminal du port 8081).
2. Relancer une recherche dans l'agence.
3. L'agence affiche un avertissement d'une ligne pour le port 8081 et présente normalement les offres de l'Hôtel 1 :
   ```text
   Avertissement : http://localhost:8081/hotel : indisponible (...)
   ```

### 3. Test des fautes métier (`@WebFault`)
- **Agence inconnue ou mauvais secret :** lancer l'agence en surchargeant le secret ou l'identifiant :
  ```bash
  cd agence
  mvn exec:exec -Dagency.secret=mauvais_secret
  ```
  Lors de la réservation, l'hôtel renvoie `UnauthorizedAgencyFault` et la CLI affiche :
  ```text
  Erreur : Authentification refusée par l'hôtel : Invalid credentials for agency 'AGENCY_VOYAGE_PRO'.
  ```
- **Carte expirée :** lors d'une réservation, saisir l'année `2022`. L'hôtel lève `PaymentValidationException`.

---

## 7. Dépannage rapide

- **Port 8080 ou 8081 déjà occupé (`Address already in use`) :**
  ```bash
  kill -9 $(lsof -t -i:8080) $(lsof -t -i:8081) 2>/dev/null
  ```
- **Erreur `zsh: no matches found: http://localhost:8080/hotel?wsdl` avec `curl` :**
  Entourer l'URL de guillemets doubles : `curl "http://localhost:8080/hotel?wsdl"`.
