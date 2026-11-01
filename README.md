# HAI704I — TP2 : Services Web SOAP / WSDL

**Binôme :** Makhbouche Oussama (GL) & Laouici Heythem (GL)  
**Université de Montpellier — Master 1 Informatique (2026–2027)**

Application distribuée de recherche et de réservation de séjours hôteliers basée sur les standards **SOAP 1.1 / WSDL 1.1** avec **Jakarta XML Web Services (Metro 4.0.5)** et **Java 17**.

---

## 1. Architecture globale

Le système se compose de deux sous-projets Maven indépendants respectant strictement le principe d'interopérabilité (aucune dépendance Maven directe entre l'agence et l'hôtel) :

```text
 ┌─────────────────────────────────────────────────────────────────┐
 │                   Agence de Voyages (Client CLI)                │
 │  - Stubs générés via wsimport (contrat WSDL)                    │
 │  - Agrégation multi-hôtels avec tri par prix total croissant    │
 │  - Tolérance aux pannes partielles (timeouts 3s / 5s)           │
 └─────────────────┬─────────────────────────────┬─────────────────┘
                   │ SOAP / HTTP                 │ SOAP / HTTP
                   ▼                             ▼
 ┌──────────────────────────────────┐ ┌──────────────────────────────────┐
 │     Hôtel 1 (port 8080)          │ │     Hôtel 2 (port 8081)          │
 │  - Profil : Grand Hotel & City   │ │  - Profil : Comédie & Mas        │
 │  - Remises partenaires dédiées   │ │  - Remises partenaires dédiées   │
 │  - Sécurité : BCrypt             │ │  - Sécurité : BCrypt             │
 │  - Idempotence & Devis           │ │  - Idempotence & Devis           │
 └──────────────────────────────────┘ └──────────────────────────────────┘
```

### Caractéristiques clés
- **Multi-instanciation :** Deux serveurs SOAP hôteliers distincts tournent en parallèle avec des catalogues de chambres et des grilles tarifaires différentes.
- **Contrat SOAP WSDL / XSD :** Deux opérations à granularité grossière :
  - `searchRooms(SearchRequest)` : recherche multicritères et génération de devis temporaires datés.
  - `reserve(ReservationRequest)` : validation d'offre, vérification bancaire, contrôle idempotence et confirmation.
- **Fautes métier typées (`@WebFault`) :** Exceptions SOAP standardisées avec classes de détail (`UnauthorizedAgencyFault`, `PaymentValidationFault`, `OfferNotFoundFault`, `OfferUnavailableFault`).
- **Tolérance aux pannes & Résilience :** Timeouts de connexion (3s) et de requête (5s) configurés via `BindingProvider` ; si un hôtel est indisponible, l'agence affiche un avertissement et continue de servir les offres du second.
- **Idempotence :** Un identifiant unique `requestId` protège contre les doubles réservations en cas de rejeu réseau.

---

## 2. Prérequis

- **Java :** JDK 17 ou supérieur
- **Maven :** 3.8 ou supérieur

Vérification de l'environnement :
```bash
java -version
mvn -version
```

---

## 3. Guide de démarrage pas-à-pas (Commandes Maven manuelles)

Pour tester le système complet, ouvrez **trois terminaux distincts** :

### Étape 1 : Démarrer l'Hôtel 1 (Terminal 1)
L'Hôtel 1 est configuré sur le port **8080** avec le profil de données `hotel1`.

```bash
cd hotel
mvn clean compile
mvn exec:exec -Dport=8080 -Dprofile=hotel1
```
* Le service est déployé sur : `http://localhost:8080/hotel`
* Le contrat WSDL est accessible sur : `http://localhost:8080/hotel?wsdl`

> **Note importante :** L'Hôtel 1 doit impérativement être démarré avant de compiler l'agence pour la première fois. En effet, le plugin `jaxws-maven-plugin:wsimport` télécharge le WSDL depuis `http://localhost:8080/hotel?wsdl` pour générer le code source des stubs clients.

---

### Étape 2 : Démarrer l'Hôtel 2 (Terminal 2)
L'Hôtel 2 est configuré sur le port **8081** avec le profil de données `hotel2`.

```bash
cd hotel
mvn exec:exec -Dport=8081 -Dprofile=hotel2
```
* Le service est déployé sur : `http://localhost:8081/hotel`
* Le contrat WSDL est accessible sur : `http://localhost:8081/hotel?wsdl`

---

### Étape 3 : Compiler et lancer l'Agence (Terminal 3)

```bash
cd agence
mvn clean compile
mvn exec:java
```

L'application console (CLI) interactive démarre et interroge simultanément les deux hôtels.

---

### Étape 4 (Optionnelle) : Lancer les tests unitaires
Les tests JUnit 5 valident la logique métier du service d'hôtel en mémoire :

```bash
cd hotel
mvn test
```

---

## 4. Structure du dépôt

```text
HAI704I_TP2/
├── hotel/                              # Service Web SOAP Hôtel (Producteur)
│   ├── pom.xml                         # Dépendances Metro JAX-WS 4.0.5, BCrypt, JUnit 5
│   └── src/
│       ├── main/java/hotel/
│       │   ├── dto/                    # DTOs JAXB (SearchRequest, RoomOffer, ReservationRequest...)
│       │   ├── model/                  # Objets métier (HotelOffer, Quote, Reservation, SearchCriteria)
│       │   ├── repo/                   # HotelRepository & InMemoryHotelRepository (BCrypt, devis, résas)
│       │   ├── service/                # HotelService (métier pur, validation, idempotence)
│       │   └── ws/                     # HotelEndpoint (@WebService), HotelPublisher, @WebFault exceptions
│       └── test/java/hotel/service/    # Tests unitaires JUnit 5 (HotelServiceTest)
│
├── agence/                             # Client SOAP Agence de voyages (Consommateur)
│   ├── pom.xml                         # Configuration jaxws-maven-plugin (wsimport)
│   └── src/
│       ├── main/resources/
│       │   └── agency.properties       # Configuration par défaut (identifiants, URLs des hôtels)
│       └── main/java/agence/
│           ├── AgencyApp.java          # Point d'entrée de l'application
│           ├── cli/                    # AgencyCli (interface console interactive)
│           ├── client/                 # HotelSoapClient (BindingProvider, timeouts) & AgencyConfig
│           └── service/                # AgencyService (agrégation multi-hôtels, tri, conversion)
│
├── captures/                           # Traces XML des échanges SOAP et contrats
│   ├── wsdl.xml                        # Contrat WSDL exporté
│   ├── schema.xsd                      # Schéma XSD des types JAXB
│   ├── 01_search_request.xml           # Requête SOAP de recherche
│   ├── 01_search_response.xml          # Réponse SOAP avec offres
│   ├── 02_reserve_request.xml          # Requête SOAP de réservation
│   ├── 02_reserve_response.xml         # Réponse SOAP de confirmation
│   ├── 03_fault_wrong_agency_request.xml   # Requête avec identifiants invalides
│   └── 03_fault_wrong_agency_response.xml  # Réponse SOAP Fault correspondante
│
└── README.md                           # Documentation principale du projet
```

---

## 5. Gestion de la configuration

Le client agence charge sa configuration par défaut depuis `agence/src/main/resources/agency.properties` :

```properties
agency.id=AGENCY_VOYAGE_PRO
agency.secret=secret_voyage_123
hotel.urls=http://localhost:8080/hotel,http://localhost:8081/hotel
```

Il est possible de surcharger ces valeurs de deux manières sans modifier le fichier :

1. **Via les propriétés système Java / Maven :**
   ```bash
   cd agence
   mvn exec:java -Dagency.id=AGENCY_SELECTOUR -Dagency.secret=secret_selectour_456
   ```

2. **Via les variables d'environnement système :**
   ```bash
   export AGENCY_ID="AGENCY_BOOKING_EXP"
   export AGENCY_SECRET="secret_booking_789"
   export HOTEL_URLS="http://localhost:8080/hotel"
   mvn exec:java
   ```

---

## 6. Données de test préchargées

### Agences partenaires et remises négociées
Les mots de passe sont vérifiés via hachage **BCrypt** côté hôtel. Les pourcentages de réduction varient selon l'hôtel :

| Identifiant Agence (`agencyId`) | Mot de passe (`agencySecret`) | Remise Hôtel 1 (8080) | Remise Hôtel 2 (8081) |
| :--- | :--- | :---: | :---: |
| `AGENCY_VOYAGE_PRO` *(défaut)* | `secret_voyage_123` | **15 %** | 5 % |
| `AGENCY_SELECTOUR` | `secret_selectour_456` | 5 % | **20 %** |
| `AGENCY_BOOKING_EXP` | `secret_booking_789` | 10 % | 15 % |

### Offres de chambres par profil

* **Profil `hotel1` (port 8080) :**
  - *Grand Hotel Montpellier* (4★) : `Single Comfort` (1 pers., 100 €/n.), `Double Deluxe` (2 pers., 150 €/n.), `Family Suite` (4 pers., 250 €/n.)
  - *Montpellier City Centre* (3★) : `Standard Double` (2 pers., 90 €/n.)

* **Profil `hotel2` (port 8081) :**
  - *Hôtel de la Comédie* (4★) : `Superior Classic` (2 pers., 130 €/n.), `Executive King` (2 pers., 190 €/n.), `Panoramique Suite` (4 pers., 310 €/n.)
  - *Le Mas Saint-Roch* (5★) : `Royal Villa` (4 pers., 520 €/n.)

---

## 7. Scénarios de validation dans la CLI

Une fois l'agence lancée (`mvn exec:java`), appuyez sur **Entrée** sur les invites entre crochets `[...]` pour accepter les valeurs préremplies.

### Scénario A : Recherche et réservation nominale
1. Choisir `1` (Chercher un séjour).
2. Valider les filtres par défaut (Montpellier, dates suggérées, 2 personnes, 3 étoiles min, sans plafond).
3. Les offres des deux hôtels sont affichées, fusionnées et classées par ordre de prix total croissant.
4. Saisir le numéro d'une offre (ex. `1`), renseigner le nom/prénom du voyageur et confirmer le paiement.
5. La confirmation s'affiche avec la référence unique et le numéro de carte bancaire masqué :
   ```text
   Réservation confirmée : RES-7D9A12BE (382.5€, payé avec carte ****-****-****-4321)
   ```

### Scénario B : Tolérance aux pannes (Panne partielle)
1. Arrêter l'Hôtel 2 (`Ctrl+C` dans le terminal du port 8081).
2. Lancer une recherche dans l'agence.
3. L'agence ne bloque pas : après le timeout (3s), un avertissement est affiché et les offres de l'Hôtel 1 restent accessibles et réservables :
   ```text
   Avertissement : http://localhost:8081/hotel : indisponible (...)
   ```

### Scénario C : Fautes SOAP métier (@WebFault)
- **Authentification invalide :**
  ```bash
  mvn exec:java -Dagency.secret=mauvais_secret
  ```
  Lors de la recherche ou de la réservation, l'hôtel lève un `UnauthorizedAgencyFault`.
- **Chambre déjà réservée :** Tenter de réserver deux fois la même offre. L'hôtel renvoie un `OfferUnavailableFault` (*"Chambre déjà réservée"*).
- **Carte expirée :** Lors de la saisie bancaire, renseigner une année passée (ex. `2020`). L'hôtel lève un `PaymentValidationFault` (*"Carte bancaire expirée"*).

---

## 8. Dépannage fréquent

- **Erreur `Address already in use` (port 8080 ou 8081 déjà occupé) :**
  ```bash
  kill -9 $(lsof -t -i:8080) $(lsof -t -i:8081) 2>/dev/null
  ```
- **Erreur `Failed to read the WSDL document: Connection refused` lors de `mvn compile` dans `agence` :**
  L'Hôtel 1 doit être en cours d'exécution sur le port 8080 au moment de la compilation afin que `wsimport` puisse générer les classes clientes.
