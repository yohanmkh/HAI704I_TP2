# Compte-Rendu de TP2 — Services Web SOAP / WSDL

**Université de Montpellier — Faculté des Sciences**  
**Formation :** Master 1 Informatique (Parcours Génie Logiciel)  
**Unité d'Enseignement :** HAI704I — Architectures Logicielles Distribuées  
**Enseignant :** M. Bachar Rima  
**Année universitaire :** 2026–2027  

**Binôme :**  
- Makhbouche Oussama (Parcours GL)  
- Laouici Heythem (Parcours GL)  

---

## Table des matières

1. [Objectifs du travail](#1-objectifs-du-travail)
2. [Architecture globale](#2-architecture-globale)
3. [Réalisation étape par étape (A0 à A8)](#3-réalisation-étape-par-étape-a0-à-a8)
   - [3.1 Étape A0 — Mise en place de l'environnement](#31-étape-a0--mise-en-place-de-lenvironnement)
   - [3.2 Étape A1 — Contrat de recherche et grain des opérations](#32-étape-a1--contrat-de-recherche-et-grain-des-opérations)
   - [3.3 Étape A2 — Séparation des responsabilités côté serveur](#33-étape-a2--séparation-des-responsabilités-côté-serveur)
   - [3.4 Étape A3 — Gestion des fautes métier (@WebFault)](#34-étape-a3--gestion-des-fautes-métier-webfault)
   - [3.5 Étape A4 — Génération du client Agence par contrat WSDL](#35-étape-a4--génération-du-client-agence-par-contrat-wsdl)
   - [3.6 Étape A5 — Opération de réservation, devis et idempotence](#36-étape-a5--opération-de-réservation-devis-et-idempotence)
   - [3.7 Étape A6 — Agrégation multi-hôtels et tolérance aux pannes](#37-étape-a6--agrégation-multi-hôtels-et-tolérance-aux-pannes)
   - [3.8 Étape A8 — Interface en ligne de commande et isolation](#38-étape-a8--interface-en-ligne-de-commande-et-isolation)
4. [Analyse des captures réseau et traçabilité (A7)](#4-analyse-des-captures-réseau-et-traçabilité-a7)
   - [4.1 Recherche de chambres (Requête et Réponse)](#41-recherche-de-chambres-requête-et-réponse)
   - [4.2 Réservation d'une offre (Requête et Réponse)](#42-réservation-dune-offre-requête-et-réponse)
   - [4.3 Faute SOAP sur agence non reconnue](#43-faute-soap-sur-agence-non-reconnue)
   - [4.4 Traçabilité de bout en bout du champ maxPrice](#44-traçabilité-de-bout-en-bout-du-champ-maxprice)
5. [Synthèse et état d'avancement](#5-synthèse-et-état-davancement)
6. [Regard critique et limites](#6-regard-critique-et-limites)
7. [Usage des outils d'IA générative](#7-usage-des-outils-dia-générative)
8. [Références](#8-références)

---

## 1. Objectifs du travail

Ce projet met en œuvre une architecture distribuée de recherche et de réservation hôtelière reposant sur le protocole SOAP 1.1 et les contrats WSDL 1.1 avec Jakarta XML Web Services (JAX-WS / Metro 4.0.5) en Java 17. L'application met en relation deux systèmes indépendants : des services d'hôtellerie autonomes exposant leurs disponibilités et leurs tarifs négociés, et une application cliente d'agence de voyage chargée d'interroger plusieurs hôtels, de consolider leurs offres et de procéder aux réservations.

L'enjeu principal consiste à garantir un couplage strictement limité au contrat XML : le projet de l'agence ne partage aucune dépendance Maven ni aucune classe compilée avec le projet de l'hôtel. Le travail aborde également le découpage interne en couches, la sécurité des données d'authentification et de paiement, l'idempotence des écritures en cas de rejeu, ainsi que la continuité de service lorsqu'un nœud distant ne répond plus.

---

## 2. Architecture globale

Le système se divise en deux projets Maven distincts (`hotel/` et `agence/`). Deux instances du serveur hôtelier tournent dans des processus JVM séparés sur les ports `8080` (profil `hotel1`) et `8081` (profil `hotel2`).

```text
+-------------------------------------------------------------------+
|                        Processus Agence (JVM 3)                   |
|                                                                   |
|  +--------------+      +-----------------+      +---------------+ |
|  |  AgencyCli   | ---> |  AgencyService  | ---> |HotelSoapClient| |
|  | (Console UI) |      | (Agrégation/Tri)|      | (Stubs/Timeouts)|
|  +--------------+      +-----------------+      +-------+-------+ |
+---------------------------------------------------------|---------+
                                                          |
                        SOAP / HTTP (XML)                 |
               +------------------------------------------+---------+
               |                                                    |
               v (Port 8080)                                        v (Port 8081)
+------------------------------+                     +------------------------------+
|     Hôtel 1 (JVM 1)          |                     |     Hôtel 2 (JVM 2)          |
|                              |                     |                              |
|  +------------------------+  |                     |  +------------------------+  |
|  |     HotelEndpoint      |  |                     |  |     HotelEndpoint      |  |
|  |     (@WebService)      |  |                     |  |     (@WebService)      |  |
|  +-----------+------------+  |                     |  +-----------+------------+  |
|              |               |                     |              |               |
|  +-----------v------------+  |                     |  +-----------v------------+  |
|  |      HotelService      |  |                     |  |      HotelService      |  |
|  |  (Métier pur, sans WS) |  |                     |  |  (Métier pur, sans WS) |  |
|  +-----------+------------+  |                     |  +-----------+------------+  |
|              |               |                     |              |               |
|  +-----------v------------+  |                     |  +-----------v------------+  |
|  |InMemoryHotelRepository |  |                     |  |InMemoryHotelRepository |  |
|  | (Profil "hotel1" - 15%)|  |                     |  | (Profil "hotel2" - 5%) |  |
|  +------------------------+  |                     |  +------------------------+  |
+------------------------------+                     +------------------------------+
```

Côté serveur, l'architecture isole la couche de transport SOAP (`hotel.ws`), la logique métier (`hotel.service`) et l'accès aux données (`hotel.repo`). Les objets de transfert sérialisés par JAXB résident dans `hotel.dto`, tandis que les structures internes (`Quote`, `Reservation`, `SearchCriteria`) sont placées dans `hotel.model`.

Côté agence, les classes générées par `wsimport` dans le paquetage `agence.generated` restent confinées derrière `HotelSoapClient`. `AgencyService` manipule ses propres objets (`AgencyOffer`, `AgencySearchResult`, `ReservationReceipt`) pour alimenter `AgencyCli`.

---

## 3. Réalisation étape par étape (A0 à A8)

### 3.1 Étape A0 — Mise en place de l'environnement

Sur les versions récentes du JDK (Java 17+), les API Jakarta XML Web Services et JAXB ne font plus partie de la bibliothèque standard. Les deux `pom.xml` intègrent donc l'implémentation de référence Metro (`com.sun.xml.ws:jaxws-rt:4.0.5`) ainsi que `org.glassfish.jaxb:jaxb-runtime:4.0.5`. L'ajout explicite de `jaxb-runtime` est indispensable au moment de l'exécution : sans lui, la sérialisation d'une faute SOAP personnalisée par `SOAPFaultBuilder` échoue avec une `ClassNotFoundException` sur `ContextFactory`. Pour éviter les problèmes de délégation de classloader propres au goal `exec:java` avec le serveur HTTP embarqué du JDK, l'exécution passe par le plugin `exec-maven-plugin` configuré sur le goal `exec:exec` qui lance un processus `java` standard.

### 3.2 Étape A1 — Contrat de recherche et grain des opérations

Le service expose l'opération `searchRooms(SearchRequest)` qui retourne une `List<RoomOffer>`. La publication est assurée par `Endpoint.publish()` dans `HotelPublisher`, rendant le document WSDL consultable sur `http://localhost:8080/hotel?wsdl`.

**Justification du grain des opérations :** Nous avons retenu une opération unique à gros grain (*coarse-grained*) plutôt qu'une succession d'appels fins du type `getHotelsByCity()`, `isRoomAvailable()` puis `getRoomPrice()`. Sur le réseau, chaque appel SOAP entraîne un coût fixe de connexion HTTP et d'analyse DOM/SAX de l'enveloppe XML. Envoyer l'ensemble des critères dans un seul objet `SearchRequest` et récupérer toutes les offres calculées en un seul message `searchRoomsResponse` évite l'anti-patron *chatty interface* et réduit la latence globale à un unique aller-retour par hôtel.

### 3.3 Étape A2 — Séparation des responsabilités côté serveur

Le code initial a été restructuré autour de trois classes principales : `HotelEndpoint`, `HotelService` et `InMemoryHotelRepository` (implémentant l'interface `HotelRepository`).

`HotelEndpoint` porte les annotations `@WebService` et `@WebMethod`. Son rôle se limite à recevoir les DTOs JAXB, les convertir en objets du domaine (`SearchCriteria`), appeler `HotelService` et transformer le résultat en DTOs de sortie. `HotelService` ne contient aucun import `jakarta.xml.ws.*` ou `jakarta.xml.bind.*`. Cette indépendance nous a permis d'écrire `HotelServiceTest` avec JUnit 5 pour vérifier le calcul des remises et la validation des réservations directement en mémoire, en moins de 50 millisecondes, sans démarrer de pile HTTP/SOAP. Si l'on souhaitait remplacer la persistance en mémoire par une base relationnelle JPA/H2, seule une nouvelle implémentation de `HotelRepository` serait nécessaire, sans toucher à `HotelService` ni au contrat WSDL.

### 3.4 Étape A3 — Gestion des fautes métier (@WebFault)

Lorsqu'une agence transmet un identifiant inconnu (ou un mot de passe invalide lors de la réservation), le service lève `UnauthorizedAgencyException`. Cette classe est annotée avec `@WebFault(name = "UnauthorizedAgencyFault")` et encapsule le bean de détail `UnauthorizedAgencyFault` qui transporte l'`agencyId` fautif et le message d'explication.

Contrairement à une `RuntimeException` générique qui produirait un bloc `<S:Fault>` opaque ou une erreur HTTP 500 générique, l'annotation `@WebFault` force JAX-WS à déclarer un élément `<fault name="UnauthorizedAgencyException">` dans le `<portType>` et le `<binding>` du WSDL. Le client généré par `wsimport` reconstruit alors une véritable exception Java typée côté agence, permettant de récupérer proprement les informations du bloc `<detail>`.

### 3.5 Étape A4 — Génération du client Agence par contrat WSDL

Le sous-projet `agence` utilise le plugin `jaxws-maven-plugin` (version 4.0.5, goal `wsimport`) rattaché à la phase `generate-sources`. À la compilation, Maven télécharge `http://localhost:8080/hotel?wsdl` et écrit les classes clientes dans `target/generated-sources/wsimport` sous le paquetage `agence.generated`.

L'indépendance entre les deux projets est totale : le `pom.xml` de l'agence ne déclare aucune dépendance vers l'artefact `hotel`. L'URL cible inscrite en dur par `wsimport` dans le stub `HotelService` sert uniquement de valeur par défaut ; à l'exécution, `HotelSoapClient` écrase systématiquement cette adresse via la propriété `BindingProvider.ENDPOINT_ADDRESS_PROPERTY` à partir de la liste d'URLs lue dans `agency.properties` (ou surchargée en ligne de commande par `-Dhotel.urls=...`).

### 3.6 Étape A5 — Opération de réservation, devis et idempotence

L'opération `reserve(ReservationRequest)` traite les réservations fermes et peut lever quatre fautes déclarées au contrat : `UnauthorizedAgencyException`, `OfferNotFoundException`, `OfferUnavailableException` et `PaymentValidationException`. La requête imbrique deux sous-objets : `TravelerDto` (nom et prénom) et `PaymentDto` (numéro de carte, mois/année d'expiration, CVC).

**Nature d'une offre (Devis daté) :** Une offre renvoyée par `searchRooms` n'est pas un simple pointeur vers une chambre, mais un devis daté matérialisé côté serveur par la classe `Quote` et stocké dans une `ConcurrentHashMap`. À sa création, le devis fige l'`offerId`, l'identifiant de l'agence, les dates du séjour, le prix total remise déduite, ainsi qu'une date d'expiration fixée à `Instant.now().plusSeconds(7200)` (2 heures). Lors de l'appel à `reserve`, l'hôtel retrouve ce `Quote` via l'`offerId`, vérifie qu'il appartient bien à l'agence appelante et qu'il n'est pas expiré, ce qui garantit que le tarif facturé au client correspond au centime près au tarif affiché lors de la recherche.

**Idempotence du rejeu :** Pour éviter qu'une coupure réseau survenant juste après le traitement serveur ne conduise à une double facturation si le client réémet sa demande, `ReservationRequest` embarque un champ `requestId`. Le dépôt serveur maintient une table `idempotencyStore` (`Map<String, Reservation>`). Au début de `HotelService.reserve()`, si `findReservationByRequestId(req.getRequestId())` trouve une entrée, la méthode court-circuite la validation et renvoie immédiatement la réservation existante en positionnant le booléen `replayed` à `true`. *Limitation actuelle :* dans notre implémentation de `AgencyService.bookOffer()`, un nouvel `UUID.randomUUID()` est généré à chaque appel de la méthode. Depuis une interface CLI synchrone, cela ne permet pas de rejouer le même `requestId` après un échec réseau ; dans un système de production, l'agence devrait générer et conserver ce `requestId` au niveau du dossier client avant l'appel SOAP afin de le réutiliser sur toutes les tentatives de retransmission.

**Sécurité des secrets et des données bancaires :** Les mots de passe des agences partenaires ne sont jamais stockés en clair côté hôtel. À l'initialisation de `InMemoryHotelRepository`, chaque secret est haché avec `BCrypt.hashpw(..., BCrypt.gensalt(10))`. La vérification à la réservation utilise `BCrypt.checkpw()`, qui résiste aux attaques par analyse temporelle. Côté agence, l'identifiant et le secret sont lus depuis `agency.properties`. Pour le paiement, le code CVC reçu dans `PaymentDto` est validé par expression régulière (`\d{3,4}`), puis immédiatement abandonné : aucun champ CVC n'existe dans la classe métier `Reservation`. De même, le numéro de carte complet est nettoyé, vérifié sur 16 chiffres, puis tronqué par `maskCardNumber()` au format `****-****-****-1234` avant toute sauvegarde en mémoire ou renvoi dans `ReservationConfirmation`.

### 3.7 Étape A6 — Agrégation multi-hôtels et tolérance aux pannes

Deux instances du service hôtel sont lancées simultanément :
- **Hôtel 1 (`port 8080`, `-Dprofile=hotel1`) :** héberge le *Grand Hotel Montpellier* (4*) et le *Montpellier City Centre* (3*). Il accorde 15 % de remise à `AGENCY_VOYAGE_PRO` et 5 % à `AGENCY_SELECTOUR`.
- **Hôtel 2 (`port 8081`, `-Dprofile=hotel2`) :** héberge l'*Hôtel de la Comédie* (4*) et *Le Mas Saint-Roch* (5*). Il accorde 5 % de remise à `AGENCY_VOYAGE_PRO` et 20 % à `AGENCY_SELECTOUR`.

Lors d'une recherche, `AgencyService` parcourt tous les clients enregistrés, collecte les listes de `RoomOffer`, les regroupe et les trie par prix total croissant (`Comparator.comparingDouble`).

**Distinction entre refus métier et panne réseau :** Dans `AgencyService`, les appels SOAP sont entourés de blocs `try/catch` hiérarchisés. Les fautes métier déclarées au WSDL (`UnauthorizedAgencyException`, `PaymentValidationException`, `OfferNotFoundException`, `OfferUnavailableException`) sont interceptées séparément des exceptions techniques générales (`Exception`, comme un `WebServiceException` causé par un `ConnectException` ou un `SocketTimeoutException`). Lors d'une recherche, si l'Hôtel 2 est éteint, l'exception technique est capturée, un avertissement d'une ligne est ajouté à `AgencySearchResult`, et l'utilisateur reçoit malgré tout les offres de l'Hôtel 1. Pour éviter que l'agence ne reste figée sur un serveur muet, `HotelSoapClient` configure sur le `BindingProvider` un délai de connexion de 3 secondes (`connect.timeout = 3000`) et un délai de lecture de 5 secondes (`request.timeout = 5000`).

### 3.8 Étape A8 — Interface en ligne de commande et isolation

La classe `AgencyCli` propose une boucle interactive simple (1. Chercher un séjour, 2. Quitter). Conformément aux règles d'architecture du sujet, `AgencyCli` ne contient aucun import lié à SOAP, JAX-WS, `BindingProvider` ou au paquetage `agence.generated`. Elle dialogue exclusivement avec `AgencyService`, qui lui renvoie des objets métier propres à l'agence (`AgencyOffer`, `ReservationReceipt`). Les erreurs sont affichées sur une seule ligne préfixée par `Erreur : `, et les confirmations se résument à l'essentiel (référence, montant, carte masquée).

---

## 4. Analyse des captures réseau et traçabilité (A7)

Les messages XML ont été capturés sur le fil lors d'appels réels aux endpoints locaux et archivés dans le répertoire `captures/`.

### 4.1 Recherche de chambres (Requête et Réponse)

**Fichier `captures/01_search_request.xml` :**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ws="http://ws.hotel/">
  <soapenv:Header/>
  <soapenv:Body>
    <ws:searchRooms>
      <searchRequest>
        <agencyId>AGENCY_VOYAGE_PRO</agencyId>
        <city>Montpellier</city>
        <checkIn>2026-11-01</checkIn>
        <checkOut>2026-11-04</checkOut>
        <guests>2</guests>
        <minStars>4</minStars>
        <maxPrice>250.0</maxPrice>
      </searchRequest>
    </ws:searchRooms>
  </soapenv:Body>
</soapenv:Envelope>
```
Le message utilise l'espace de noms SOAP 1.1 (`http://schemas.xmlsoap.org/soap/envelope/`) et cible l'espace de noms du service `http://ws.hotel/`. L'élément `<ws:searchRooms>` correspond au wrapper *document/literal wrapped* de l'opération. L'agence `AGENCY_VOYAGE_PRO` demande un séjour de 3 nuits (du 1er au 4 novembre 2026) à Montpellier pour 2 personnes, dans un hôtel d'au moins 4 étoiles, avec un plafond de 250,0 € par nuit.

**Fichier `captures/01_search_response.xml` :**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
  <S:Body>
    <ns2:searchRoomsResponse xmlns:ns2="http://ws.hotel/">
      <roomOffer>
        <hotelName>Grand Hotel Montpellier</hotelName>
        <offerId>OFFER-H1-H1-R2</offerId>
        <pricePerNight>127.5</pricePerNight>
        <roomType>Double Deluxe</roomType>
        <stars>4</stars>
        <totalPrice>382.5</totalPrice>
      </roomOffer>
      <roomOffer>
        <hotelName>Grand Hotel Montpellier</hotelName>
        <offerId>OFFER-H1-H1-R3</offerId>
        <pricePerNight>212.5</pricePerNight>
        <roomType>Family Suite</roomType>
        <stars>4</stars>
        <totalPrice>637.5</totalPrice>
      </roomOffer>
    </ns2:searchRoomsResponse>
  </S:Body>
</S:Envelope>
```
L'Hôtel 1 écarte la chambre *Single Comfort* (capacité insuffisante : 1 personne) ainsi que l'établissement *Montpellier City Centre* (3 étoiles, inférieur au critère `minStars=4`). Les deux chambres retenues voient leur tarif de base (150 € et 250 €) réduit des 15 % négociés avec `AGENCY_VOYAGE_PRO`, soit 127,5 € et 212,5 € par nuit. Sur 3 nuits, les totaux s'établissent à 382,5 € et 637,5 €.

### 4.2 Réservation d'une offre (Requête et Réponse)

**Fichier `captures/02_reserve_request.xml` :**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ws="http://ws.hotel/">
  <soapenv:Header/>
  <soapenv:Body>
    <ws:reserve>
      <reservationRequest>
        <agencyId>AGENCY_VOYAGE_PRO</agencyId>
        <agencySecret>secret_voyage_123</agencySecret>
        <offerId>OFFER-H1-H1-R3</offerId>
        <requestId>c4b8e210-91fa-4f51-b8ef-51a83e012345</requestId>
        <traveler>
          <firstName>Sophie</firstName>
          <lastName>Germain</lastName>
        </traveler>
        <payment>
          <cardNumber>4532 9999 8888 1234</cardNumber>
          <expiryMonth>12</expiryMonth>
          <expiryYear>2028</expiryYear>
          <cvc>888</cvc>
        </payment>
      </reservationRequest>
    </ws:reserve>
  </soapenv:Body>
</soapenv:Envelope>
```
La requête référence le devis `OFFER-H1-H1-R3` (Family Suite) et fournit la clé d'idempotence `c4b8e210-91fa-4f51-b8ef-51a83e012345`. Les sous-structures `<traveler>` et `<payment>` traduisent l'imbrication des DTOs `TravelerDto` et `PaymentDto`.

**Fichier `captures/02_reserve_response.xml` :**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
  <S:Body>
    <ns2:reserveResponse xmlns:ns2="http://ws.hotel/">
      <reservationConfirmation>
        <checkIn>2026-11-01</checkIn>
        <checkOut>2026-11-04</checkOut>
        <hotelName>Grand Hotel Montpellier</hotelName>
        <maskedCardNumber>****-****-****-1234</maskedCardNumber>
        <replayed>false</replayed>
        <reservationReference>RES-4F52E255</reservationReference>
        <roomType>Family Suite</roomType>
        <totalPrice>637.5</totalPrice>
        <travelerName>Sophie Germain</travelerName>
      </reservationConfirmation>
    </ns2:reserveResponse>
  </S:Body>
</S:Envelope>
```
La confirmation porte la référence `RES-4F52E255`. Le montant facturé (`637.5`) est strictement identique à celui du devis généré lors de la recherche. Le numéro de carte revient masqué (`****-****-****-1234`), le CVC a disparu, et `<replayed>false</replayed>` indique qu'il s'agit du traitement initial de cette requête.

### 4.3 Faute SOAP sur agence non reconnue

**Fichiers `captures/03_fault_wrong_agency_request.xml` et `03_fault_wrong_agency_response.xml` :**
Lorsque la requête de recherche est soumise avec `<agencyId>ROGUE_AGENCY_404</agencyId>`, le serveur répond avec un code HTTP 500 et l'enveloppe suivante :
```xml
<?xml version="1.0" encoding="UTF-8"?>
<S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
  <S:Body>
    <S:Fault xmlns:ns4="http://www.w3.org/2003/05/soap-envelope">
      <faultcode>S:Server</faultcode>
      <faultstring>Access denied: agency 'ROGUE_AGENCY_404' is not a recognized partner.</faultstring>
      <detail>
        <ns2:UnauthorizedAgencyFault xmlns:ns2="http://ws.hotel/">
          <agencyId>ROGUE_AGENCY_404</agencyId>
          <message>Access denied: agency 'ROGUE_AGENCY_404' is not a recognized partner.</message>
        </ns2:UnauthorizedAgencyFault>
      </detail>
    </S:Fault>
  </S:Body>
</S:Envelope>
```
Le bloc `<S:Fault>` contient le code standard `S:Server` et sérialise notre bean `UnauthorizedAgencyFault` à l'intérieur de la balise `<detail>`, ce qui permet au client JAX-WS de reconstruire l'exception typée correspondante.

### 4.4 Traçabilité de bout en bout du champ `maxPrice`

Le suivi du plafond tarifaire par nuit (`maxPrice`) à travers les quatre couches illustre la chaîne de transformation JAXB / WSDL :

1. **Classe Java serveur (`hotel/src/main/java/hotel/dto/SearchRequest.java`) :**
   Le champ est déclaré comme un primitif `private double maxPrice;` avec ses accesseurs `getMaxPrice()` et `setMaxPrice(double maxPrice)` dans une classe annotée `@XmlAccessorType(XmlAccessType.FIELD)`.
2. **Schéma XML du contrat (`captures/schema.xsd`) :**
   Le générateur JAXB traduit ce champ dans le type complexe `searchRequest` sous la forme :
   `<xs:element minOccurs="0" name="maxPrice" type="xs:double"/>`
3. **Message SOAP sur le réseau (`captures/01_search_request.xml`) :**
   Lors de l'appel, la valeur sérialisée dans le corps XML est :
   `<maxPrice>250.0</maxPrice>`
4. **Stub généré côté client (`agence/target/generated-sources/wsimport/agence/generated/SearchRequest.java`) :**
   L'outil `wsimport` lit `minOccurs="0"` et `type="xs:double"` dans le XSD et génère un attribut objet `protected Double maxPrice;` accompagné des méthodes `public Double getMaxPrice()` et `public void setMaxPrice(Double value)`.

---

## 5. Synthèse et état d'avancement

L'ensemble des étapes de la partie A (points de contrôle 0 à H) est fonctionnel et validé :

| Point de contrôle | Fonctionnalité | État |
| :--- | :--- | :--- |
| **Checkpoint 0 / A** | Publication WSDL et opération `searchRooms` à gros grain | Validé |
| **Checkpoint B** | Découpage 3-tiers (`Endpoint`, `Service` sans import JAX-WS, `Repository`) | Validé |
| **Checkpoint C** | Faute métier `@WebFault` (`UnauthorizedAgencyFault`) vérifiée sur le fil | Validé |
| **Checkpoint D** | Client `agence` généré par `wsimport`, zéro dépendance vers le projet `hotel` | Validé |
| **Checkpoint E** | Opération `reserve`, devis datés (`Quote`), BCrypt, masquage CB, idempotence | Validé |
| **Checkpoint F** | Multi-hôtels (ports 8080/8081), remises différenciées, timeouts 3s/5s, panne partielle | Validé |
| **Checkpoint G / H** | Captures XML annotées, traçabilité `maxPrice`, CLI isolée de la couche SOAP | Validé |

Les extensions optionnelles (Parties B, C et D : migration Spring Boot / CXF, transfert binaire MTOM des photos de chambres, interface graphique) n'ont pas été abordées afin de privilégier la tenue de la partie principale.

---

## 6. Regard critique et limites

Bien que l'architecture réponde à toutes les exigences du cahier des charges, plusieurs points mériteraient d'être revus avant d'envisager un déploiement réel :

1. **Gestion du `requestId` côté client pour les retries :** Comme relevé à la section 3.6, `AgencyService.bookOffer()` instancie un nouvel `UUID` à chaque exécution. Le serveur gère correctement l'idempotence si on lui soumet deux fois le même message SOAP (vérifié par test direct et cURL), mais l'agence CLI ne conserve pas la clé d'une tentative échouée pour la rejouer automatiquement avec un mécanisme de *retry/backoff*.
2. **Stockage volatile en mémoire :** Les devis (`quoteStore`), les réservations et la table d'idempotence résident dans des `ConcurrentHashMap`. Le redémarrage d'un processus hôtel efface tous les devis en cours (qui ont normalement une durée de vie de 2 heures) ainsi que l'historique des réservations. Un stockage persistant (base relationnelle ou Redis pour les devis avec TTL) est indispensable.
3. **Protection des secrets en transit :** Le mot de passe de l'agence (`agencySecret`) et les coordonnées bancaires circulent en clair dans le corps XML sur HTTP. Le hachage BCrypt ne protège les secrets qu'au repos sur le serveur. En production, l'utilisation obligatoire de TLS (HTTPS) ou l'ajout d'en-têtes WS-Security (jetons signés ou chiffrés) s'impose.
4. **Absence de verrouillage transactionnel sur le stock de chambres :** La vérification de disponibilité et l'enregistrement de la réservation ne sont pas protégés par une section critique par chambre, ce qui laisse une fenêtre de concurrence (*race condition*) si deux agences réservent simultanément la dernière chambre d'une catégorie.
5. **Représentation des dates en `String` :** Les dates d'arrivée et de départ transitent sous forme de chaînes `YYYY-MM-DD` pour contourner les lourdeurs de conversion entre `javax.xml.datatype.XMLGregorianCalendar` et `java.time.LocalDate`. L'ajout d'un `XmlAdapter` JAXB dédié permettrait de conserver un typage fort `xs:date` dans le schéma XSD.

---

## 7. Usage des outils d'IA générative

Conformément aux consignes de transparence du cours, nous déclarons avoir utilisé l'assistant Claude (Anthropic) au cours de ce TP pour trois usages précis :
- **Génération et structuration initiale de code :** aide à la mise en place de la configuration Maven (`jaxws-maven-plugin`, résolution du problème de `jaxb-runtime` sur JDK récent pour la sérialisation des `@WebFault`) et écriture de squelettes de DTOs. Tout le code produit a été relu, testé, simplifié et adapté par les deux membres du binôme.
- **Éclaircissement de concepts :** approfondissement du fonctionnement du style *document/literal wrapped* dans WSDL, du salage intégré de `jBCrypt` et du paramétrage des timeouts sur le `BindingProvider` de Metro.
- **Aide à la rédaction :** structuration et relecture stylistique du présent compte-rendu à partir de nos notes techniques et de nos traces XML.

L'architecture en couches, les choix de modélisation (devis `Quote`, séparation des exceptions dans `AgencyService`) et l'ensemble des tests de validation multi-processus ont été conçus et vérifiés par le binôme.

---

## 8. Références

1. Support de cours et sujet de TP2 *HAI704I — Architectures logicielles distribuées : Services Web SOAP / WSDL*, M. Bachar Rima, Université de Montpellier, 2026–2027.
2. Documentation officielle Eclipse Metro / Jakarta XML Web Services 4.0 : https://eclipse-ee4j.github.io/metro-jax-ws/
3. Spécification W3C SOAP Version 1.1 : https://www.w3.org/TR/2000/NOTE-SOAP-20000508/
4. Documentation jBCrypt (`org.mindrot:jbcrypt`) : https://www.mindrot.org/projects/jBCrypt/

