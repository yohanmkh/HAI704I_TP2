# HAI704I — TP 2 : Services Web SOAP / WSDL
### Système Distribué de Recherche et Réservation Hôtelière Multi-Partenaires

Projet distribué client/serveur simulant la recherche et la réservation de séjours d'hôtels via des services web SOAP contract-first/code-first en Java 17+.

---

## 1. Prérequis et Environnement

- **Java Development Kit (JDK)** : Version 17 ou supérieure (testé et validé sur Java 17, 21, 25 et 27).
- **Apache Maven** : Version 3.8 ou supérieure.
- **Système d'exploitation** : macOS, Linux, ou Windows.
- **Connexion réseau** : Aucune dépendance externe au runtime (tout tourne en local sur `localhost`).

Vérifiez vos versions installées dans un terminal :
```bash
java -version
mvn -version
```

---

## 2. Démarrage Rapide (Quick Start)

Trois scripts prêts à l'emploi sont fournis à la racine du projet :

```bash
# Terminal 1 : Démarrer l'Hôtel 1 (port 8080)
./start-hotel1.sh

# Terminal 2 : Démarrer l'Hôtel 2 (port 8081)
./start-hotel2.sh

# Terminal 3 : Lancer la console de l'Agence
./start-agence.sh
```

---

## 3. Guide d'Installation et d'Exécution Manuel (Étape par Étape)

Le projet est composé de deux sous-projets Maven strictement indépendants : `hotel` (service web) et `agence` (client consommateur).

### Étape 1 : Compiler et Démarrer l'Hôtel 1 (port 8080)
Ouvrez un **premier terminal** :
```bash
cd hotel
mvn clean compile
mvn exec:exec -Dport=8080 -Dprofile=hotel1
```
*Le service écoute sur `http://localhost:8080/hotel` et le contrat WSDL est publié sur `http://localhost:8080/hotel?wsdl`.*

---

### Étape 2 : Démarrer l'Hôtel 2 (port 8081)
Ouvrez un **deuxième terminal** :
```bash
cd hotel
mvn exec:exec -Dport=8081 -Dprofile=hotel2
```
*Le service écoute sur `http://localhost:8081/hotel` et le contrat WSDL est publié sur `http://localhost:8081/hotel?wsdl`.*

---

### Étape 3 : Compiler et Lancer le Client Agence
Ouvrez un **troisième terminal** :
```bash
cd agence
mvn clean compile
mvn exec:exec
```

> **Note importante sur `wsimport`** :  
> Lors de la phase `mvn clean compile` du projet `agence`, le plugin Maven `jaxws-maven-plugin` interroge le WSDL de l'Hôtel 1 (`http://localhost:8080/hotel?wsdl`) et engendre automatiquement les stubs Java clients dans `target/generated-sources/wsimport`.  
> **L'Hôtel 1 doit donc être démarré avant de compiler l'Agence pour la première fois.**

---

## 4. Architecture et Séparation des Responsabilités

```text
HAI704I_TP2/
├── hotel/                   # Service Web SOAP (Fournisseur)
│   ├── pom.xml
│   └── src/
│       ├── main/java/hotel/
│       │   ├── dto/         # Objets de transfert de données (SearchRequest, RoomOffer, ReservationRequest, etc.)
│       │   ├── model/       # Modèles métier (Room, Hotel, Quote, Reservation, PartnerAgency)
│       │   ├── repo/        # Interface HotelRepository + InMemoryHotelRepository (données, tarifs, BCrypt)
│       │   ├── service/     # Logique métier pure (HotelService : calculs, remises, ZERO import JAX-WS)
│       │   └── ws/          # Couche SOAP (HotelEndpoint avec @WebService, @WebFault, HotelPublisher)
│       └── test/            # Tests unitaires JUnit 5 (test de HotelService sans serveur web)
│
├── agence/                  # Client Agence (Consommateur indépendant)
│   ├── pom.xml              # Déclaration du plugin wsimport (AUCUNE dépendance sur le projet hotel)
│   └── src/
│       ├── main/resources/
│       │   └── agency.properties # Configuration (URLs des hôtels, identifiant et mot de passe agence)
│       └── main/java/agence/
│           ├── client/      # HotelClient : proxy SOAP, timeouts HTTP, injection dynamique des URLs
│           ├── service/     # AgencyService : agrégation multi-hôtels, tri comparatif par prix
│           ├── cli/         # AgencyCli : interface console utilisateur (ZERO import SOAP/JAX-WS)
│           └── AgencyApp.java # Point d'entrée de l'application
│
├── captures/                # Traces réseau et messages XML du TP (WSDL, XSD, requêtes, réponses, fautes)
├── start-hotel1.sh          # Script de lancement Hôtel 1 (8080)
├── start-hotel2.sh          # Script de lancement Hôtel 2 (8081)
└── start-agence.sh          # Script de lancement Agence CLI
```

### Principes Clés d'Ingénierie Respectés :
1. **Séparation 3-Tiers Côté Hôtel** :
   - `HotelEndpoint` : Traducteur transport SOAP, délègue immédiatement au service.
   - `HotelService` : Logique métier pure, sans aucune annotation `@WebService` ni import `jakarta.xml.ws.*`. Testable unitairement en millisecondes.
   - `InMemoryHotelRepository` : Stockage des chambres, réservations, devis et hashs BCrypt.
2. **Couplage Faible Côté Agence** :
   - Le projet `agence` ne dépend **d'aucun fichier source ou binaire** du projet `hotel`. L'interopérabilité repose à 100% sur le contrat WSDL.
   - La classe console `AgencyCli` n'a aucune connaissance du protocole SOAP ou des proxies ; elle dialogue uniquement avec `AgencyService`.
3. **Sécurité et Confidentialité (PCI-DSS simplifié)** :
   - Mots de passe agence hashés et salés avec **BCrypt** (`BCrypt.checkpw`).
   - Le code de sécurité carte (**CVC**) est validé puis immédiatement détruit. Il n'est jamais stocké ni affiché.
   - Le numéro de carte est systématiquement masqué (`****-****-****-1234`).
4. **Idempotence des Réservations** :
   - Chaque réservation possède un identifiant de requête unique (`requestId` UUID).
   - En cas de renvoi de la même requête réseau, l'hôtel retourne la confirmation existante sans créer de doublon.

---

## 5. Données de Test (Jeu d'Essai)

### Hôtels et Chambres
- **Hôtel 1 (port 8080)** : *Grand Hotel Montpellier* (4*) et *Montpellier City Centre* (3*).
- **Hôtel 2 (port 8081)** : *Hôtel de la Comédie* (4*) et *Le Mas Saint-Roch* (5*).

### Agences Partenaires et Remises Négociées
Les mots de passe sont vérifiés via hash BCrypt côté serveur :
| Identifiant Agence | Mot de passe clair | Remise Hôtel 1 | Remise Hôtel 2 |
| :--- | :--- | :--- | :--- |
| `AGENCY_VOYAGE_PRO` | `secret_voyage_123` | **15%** | **5%** |
| `AGENCY_SELECTOUR` | `secret_selectour_456` | **5%** | **20%** |
| `AGENCY_BOOKING_EXP` | `secret_booking_789` | **10%** | **15%** |

*(Par défaut, l'agence utilise `AGENCY_VOYAGE_PRO` dans `agency.properties`).*

---

## 6. Scénarios de Test et Utilisation de la CLI

Au lancement de l'agence (`./start-agence.sh`), le menu principal s'affiche :

```text
1. Chercher un séjour
2. Quitter
Choix : 
```

### Scénario 1 : Recherche Multi-Hôtels et Comparaison de Prix
1. Tapez `1`.
2. Saisissez les critères :
   - **Ville** : Appuyez sur Entrée (valeur par défaut : `Montpellier`).
   - **Date d'arrivée** : `2026-11-01`
   - **Date de départ** : `2026-11-05`
   - **Nombre de personnes** : `2`
   - **Étoiles minimum** : `3`
   - **Prix maximum** : `1000`
3. **Résultat attendu** :
   Les offres disponibles provenant des **deux hôtels** s'affichent, automatiquement triées par prix total croissant (en tenant compte de la remise partenaire de 15% pour l'Hôtel 1 et 5% pour l'Hôtel 2).

---

### Scénario 2 : Réservation d'un Séjour
1. À l'invite `Entrez le numéro de l'offre à réserver`, saisissez le numéro de l'offre (ex : `1`).
2. Renseignez les coordonnées du voyageur et de paiement :
   - **Prénom** : `Alice`
   - **Nom** : `Dupont`
   - **Numéro de carte (16 chiffres)** : `4970123456789012`
   - **Mois d'expiration (1-12)** : `12`
   - **Année d'expiration** : `2028`
   - **CVC (3 chiffres)** : `789`
3. **Résultat attendu** :
   ```text
   Réservation confirmée : RES-1A2B3C4D
   Hôtel : Grand Hotel Montpellier | Chambre : 101 | Total : 374,00 EUR
   ```

---

### Scénario 3 : Test de Tolérance aux Pannes (Panne Partielle / Hôtel Défaillant)
1. Assurez-vous que l'Hôtel 1 et l'Hôtel 2 tournent.
2. Dans le terminal de l'**Hôtel 2**, stoppez le processus avec `Ctrl+C`.
3. Dans la console de l'Agence, relancez une recherche (Choix `1`).
4. **Résultat attendu** :
   - L'application client **ne plante pas**.
   - Une ligne d'avertissement claire indique l'indisponibilité de l'Hôtel 2 :  
     `Avertissement : http://localhost:8081/hotel : indisponible (...)`
   - Les offres de l'Hôtel 1 sont bien retournées et réservables sans interruption.

---

### Scénario 4 : Test de Faute Métier SOAP (`@WebFault`)
1. Dans `agence/src/main/resources/agency.properties`, modifiez le secret pour mettre un mot de passe erroné :
   ```properties
   agency.secret=mauvais_secret
   ```
2. Relancez l'agence et lancez une recherche :
   - L'hôtel rejette l'appel avec une faute SOAP `UnauthorizedAgencyFault`.
   - La CLI intercepte la faute et affiche élégamment :  
     `Erreur : Agence non autorisee : Mot de passe incorrect pour l'agence AGENCY_VOYAGE_PRO`
3. Rétablissez la valeur correcte `secret_voyage_123`.

---

## 7. Exécution des Tests Unitaires

La logique métier de l'hôtel peut être vérifiée immédiatement sans démarrer de serveur SOAP :
```bash
cd hotel
mvn test
```
*Les tests JUnit 5 valident le calcul des nuitées, l'application des pourcentages de remise selon les agences, la vérification des dates et la disponibilité.*

---

## 8. Configuration Avancée

### Fichier `agency.properties`
Le fichier `agence/src/main/resources/agency.properties` permet d'ajuster dynamiquement les paramètres sans recompiler :
```properties
agency.id=AGENCY_VOYAGE_PRO
agency.secret=secret_voyage_123
hotel.urls=http://localhost:8080/hotel,http://localhost:8081/hotel
```

### Surcharge par Propriétés Système (CLI)
Il est possible de surcharger n'importe quelle propriété directement lors de l'exécution :
```bash
cd agence
mvn exec:exec -Dagency.id=AGENCY_SELECTOUR -Dagency.secret=secret_selectour_456
```

---

## 9. Résolution des Problèmes Fréquents (FAQ / Troubleshooting)

- **Erreur : `Address already in use` au démarrage d'un hôtel** :  
  Un processus occupe déjà le port 8080 ou 8081. Libérez le port avec :
  ```bash
  kill -9 $(lsof -t -i:8080)
  kill -9 $(lsof -t -i:8081)
  ```

- **Erreur de compilation dans `agence` : `Failed to read WSDL`** :  
  Le plugin `wsimport` doit contacter l'Hôtel 1 pour générer les stubs. Démarrez d'abord l'Hôtel 1 dans un autre terminal (`./start-hotel1.sh`), puis relancez `mvn clean compile` dans `agence`.

- **Erreur `zsh: no matches found: http://localhost:8080/hotel?wsdl`** :  
  Dans le shell `zsh`, le point d'interrogation `?` est interprété comme un métacaractère. Entourez toujours l'URL de guillemets doubles lors de tests cURL :
  ```bash
  curl "http://localhost:8080/hotel?wsdl"
  ```
