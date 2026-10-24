# HAI704I — TP 2 : Services Web SOAP / WSDL

Projet distribué client/serveur simulant la recherche et la réservation de séjours d'hôtels via des services web SOAP.

---

## 1. Environnement technique

- **JDK** : Java 17 ou supérieur (validé sur OpenJDK 27 / Temurin 25)
- **Maven** : 3.9+
- **Pile SOAP** : Jakarta XML Web Services 4.0 (`com.sun.xml.ws:jaxws-rt:4.0.5`)
- **Génération client** : `com.sun.xml.ws:jaxws-maven-plugin:4.0.5` (`wsimport`)
- **Hashage sécurité** : `org.mindrot:jbcrypt:0.4`
- **Tests** : JUnit 5 (`org.junit.jupiter:5.11.0`)

---

## 2. Structure du projet

```text
HAI704I_TP2/
├── hotel/                   # Service web SOAP de l'hôtel
│   ├── pom.xml
│   └── src/main/java/
│       └── hotel/
│           ├── dto/         # DTOs du contrat (SearchRequest, RoomOffer, ReservationRequest, etc.)
│           ├── model/       # Modèles métier (Quote, Reservation, SearchCriteria)
│           ├── repo/        # Interface dépôt + implémentation en mémoire (tarifs, bcrypt)
│           ├── service/     # Logique métier pure (calculs, disponibilité, ZERO import JAX-WS)
│           └── ws/          # Point d'accès SOAP (@WebService, @WebFault, Endpoint.publish)
├── agence/                  # Client de l'agence de voyage (indépendant)
│   ├── pom.xml              # wsimport pointant sur le WSDL de l'hôtel
│   ├── src/main/resources/
│   │   └── agency.properties # Configuration (secret de l'agence, URLs des hôtels)
│   └── src/main/java/
│       └── agence/
│           ├── client/      # Proxy SOAP généré (wsimport) + gestion des timeouts
│           ├── service/     # Logique de l'agence (agrégation multi-hôtels, tri par prix)
│           ├── cli/         # Interface console utilisateur (ZERO appel/import SOAP)
│           └── AgencyApp.java # Point d'entrée de l'agence
└── captures/                # Captures XML du TP (WSDL, XSD, requêtes, réponses, fautes)
```

---

## 3. Données de test (jeu d'essai)

### Hôtels disponibles
- **Hôtel 1 (port 8080)** : *Grand Hotel Montpellier* (4*), *Montpellier City Centre* (3*)
- **Hôtel 2 (port 8081)** : *Hôtel de la Comédie* (4*), *Le Mas Saint-Roch* (5*)

### Agences partenaires & Tarifs négociés
Les mots de passe des agences sont stockés côté serveur sous forme de hash salé BCrypt (`BCrypt.checkpw`).
- **AGENCY_VOYAGE_PRO** (mot de passe : `secret_voyage_123`) :
  - Hôtel 1 : 15% de réduction
  - Hôtel 2 : 5% de réduction
- **AGENCY_SELECTOUR** (mot de passe : `secret_selectour_456`) :
  - Hôtel 1 : 5% de réduction
  - Hôtel 2 : 20% de réduction
- **AGENCY_BOOKING_EXP** (mot de passe : `secret_booking_789`) :
  - Hôtel 1 : 10% de réduction
  - Hôtel 2 : 15% de réduction

---

## 4. Instructions de compilation et d'exécution

### Étape 1 : Compiler et démarrer l'Hôtel 1 (port 8080)
Dans un premier terminal :
```bash
cd hotel
mvn clean compile
java -Dport=8080 -Dprofile=hotel1 -classpath target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout) hotel.ws.HotelPublisher
```
Le service est accessible sur `http://localhost:8080/hotel` et son contrat sur `http://localhost:8080/hotel?wsdl`.

### Étape 2 : Démarrer l'Hôtel 2 (port 8081)
Dans un deuxième terminal :
```bash
cd hotel
java -Dport=8081 -Dprofile=hotel2 -classpath target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout) hotel.ws.HotelPublisher
```
Le service est accessible sur `http://localhost:8081/hotel` et son contrat sur `http://localhost:8081/hotel?wsdl`.

### Étape 3 : Engendrer les stubs et lancer l'Agence
Dans un troisième terminal :
```bash
cd agence
mvn clean compile
mvn exec:exec
```
*Note : la commande `mvn clean compile` dans `agence` lance automatiquement le greffon `jaxws:wsimport` qui télécharge le WSDL depuis `http://localhost:8080/hotel?wsdl` et engendre les classes Java clientes dans `target/generated-sources/wsimport`.*

---

## 5. Utilisation de la CLI

Au lancement, le menu principal s'affiche :
```text
1. Chercher un séjour
2. Quitter
Choix : 1
```

1. **Recherche** :
   - Renseigner la ville (défaut : Montpellier), dates d'arrivée/départ, nombre de personnes, étoiles minimum et budget max.
   - Les offres des deux hôtels sont agrégées et triées par prix total croissant.
2. **Réservation** :
   - Entrer le numéro d'offre souhaité.
   - Saisir le prénom, nom et des coordonnées bancaires fictives (16 chiffres, mois, année, CVC).
   - Une référence de confirmation unique s'affiche (ex. `RES-FF5CEC93`).
3. **Sécurité & Protection des données** :
   - Le CVC est validé à la volée puis immédiatement détruit. Il n'est stocké nulle part.
   - Le numéro de carte est systématiquement masqué (`****-****-****-4321`).
4. **Idempotence** :
   - Chaque demande génère un `requestId` (UUID). Si une même requête est renvoyée, l'hôtel renvoie la réservation existante sans créer de doublon.

---

## 6. Vérification de la tolérance aux pannes (Checkpoint F)

1. Lancer les deux hôtels et l'agence.
2. Couper le processus de l'Hôtel 2 (port 8081) avec `Ctrl+C`.
3. Relancer une recherche depuis l'agence :
   - L'agence ne plante pas et n'affiche aucune trace d'exception.
   - Les offres de l'Hôtel 1 sont bien renvoyées.
   - Une ligne d'avertissement claire signale que l'Hôtel 2 est indisponible :
     `Avertissement : http://localhost:8081/hotel : indisponible (...)`

---

## 7. Configuration externe

Le fichier `agence/src/main/resources/agency.properties` permet de modifier la configuration sans recompiler :
```properties
agency.id=AGENCY_VOYAGE_PRO
agency.secret=secret_voyage_123
hotel.urls=http://localhost:8080/hotel,http://localhost:8081/hotel
```

On peut également surcharger ces valeurs au lancement via des propriétés système :
```bash
java -Dhotel.urls=http://localhost:8080/hotel -Dagency.id=AGENCY_SELECTOUR -Dagency.secret=secret_selectour_456 -classpath target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout) agence.AgencyApp
```

---

## 8. Lancer les tests unitaires du service hôtel

La logique métier de l'hôtel (`HotelService`) peut être testée sans lancer de serveur SOAP :
```bash
cd hotel
mvn test
```
