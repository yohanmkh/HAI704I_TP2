# Guide de tournage — Démonstration HAI704I TP2 SOAP

> Ce document décrit les étapes exactes à suivre pour enregistrer une vidéo de démonstration cohérente avec le code.  
> Ouvrez **trois terminaux** côte à côte (Hotel 1, Hotel 2, Agency CLI) afin que la vidéo montre bien l'architecture distribuée.

---

## 1. Pré-requis (une seule fois)

```powershell
# Dans la racine du projet
cd hotel
mvn clean compile dependency:copy-dependencies -DskipTests
cd ..\agence
mvn clean compile dependency:copy-dependencies -DskipTests
cd ..
```

---

## 2. Lancement des services (trois terminaux)

| Terminal | Commande | Ce qu'on doit voir |
|----------|----------|-------------------|
| **T1 – Hotel 1** | `cd hotel; java -cp "target/classes;target/dependency/*" hotel.ws.HotelPublisher 18080 hotel1` | `Hôtel (hotel1) démarré sur http://localhost:18080/hotel` |
| **T2 – Hotel 2** | `cd hotel; java -cp "target/classes;target/dependency/*" hotel.ws.HotelPublisher 18081 hotel2` | `Hôtel (hotel2) démarré sur http://localhost:18081/hotel` |
| **T3 – Agency CLI** | `cd agence; $env:HOTEL_URLS="http://localhost:18080/hotel,http://localhost:18081/hotel"; java -cp "target/classes;target/dependency/*" agence.AgencyApp` | Menu CLI :<br>`1. Chercher un séjour`<br>`2. Quitter`<br>`Choix :` |

> ⚠️ Ne fermez **aucun** terminal avant la fin de la démo.

---

## 3. Scénarios à filmer (dans l'ordre)

### 3.1 Recherche & comparaison des prix
1. Dans **T3**, tapez `1` → *Entrée*  
2. Acceptez les défauts (Montpellier, 31/10 → 03/11, 2 pers., 3★, prix max 0) → *Entrée* à chaque prompt  
3. **Résultat attendu** : liste d'offres des **deux hôtels** avec prix **différents**  
   - Hotel 1 (Grand Hotel / Montpellier City Centre) – remise 15 % pour AGENCY_VOYAGE_PRO  
   - Hotel 2 (Hôtel de la Comédie / Le Mas Saint-Roch) – remise 5 % pour AGENCY_VOYAGE_PRO  
4. **Commentaire oral** : « On voit que les prix varient selon l'hôtel **et** selon le taux négocié par l'agence. »

---

### 3.2 Réservation normale
1. Choisissez une offre (ex. `1`) → *Entrée*  
2. Renseignez :  
   - Prénom / Nom : `Jean` / `Dupont`  
   - Carte : `4532 0150 9876 4321` (défaut)  
   - Expiration : `12` / `2028` (défauts)  
   - CVC : `123` (défaut)  
3. **Résultat attendu** :  
   `Réservation confirmée : REF-XXXXXX (XXX.XX€, payé avec carte 4532 **** **** 4321)`

---

### 3.3 Refus métier – secret d'agence erroné
1. Relancez une recherche (`1` + défauts)  
2. Choisissez une offre  
3. **Avant de payer**, dans **T3**, changez la variable d'env (ou modifiez `agency.properties`) :  
   ```powershell
   $env:AGENCY_SECRET = "mauvais_secret"
   # ou éditez agency.properties puis relancez l'AgencyApp
   ```
4. Re-tentez la réservation même offre  
5. **Résultat attendu** :  
   `Erreur : Agence non autorisée (secret invalide)`

---

### 3.4 Refus métier – offre indisponible / règles métier
1. Remettez le bon secret (`secret_voyage_123`) et relancez l'AgencyApp si besoin  
2. Faites une réservation → notez la référence  
3. **Sans quitter**, retentez de réserver **la même offre** (même référence idempotente)  
4. **Résultat attendu** :  
   `Note : rejeu de demande, réservation déjà existante.`  
   *(ou erreur `OfferUnavailable` si l'offre n'est plus dispo)*

---

### 3.5 Panne – hôtel éteint
1. Dans **T2** (Hotel 2), faites **Ctrl+C** pour l'arrêter  
2. Dans **T3**, refaites une recherche (`1` + défauts)  
3. **Résultat attendu** :  
   - Liste d'offres **uniquement depuis Hotel 1**  
   - Message d'avertissement affiché :  
     `Avertissement : Hotel at http://localhost:18081/hotel unreachable`

---

### 3.6 Arrêt propre
1. Dans **T3** : tapez `2` → *Entrée* (quitte le CLI)  
2. Dans **T1** : **Ctrl+C** (arrêt Hotel 1)  
3. Dans **T2** : déjà arrêté  

---

## 4. Check-list « prêt pour tournage »

- [ ] Trois terminaux visibles à l'écran (ou bascule rapide)  
- [ ] Police lisible (taille ≥ 14 pt)  
- [ ] Pas de notifications parasites (mode « Ne pas déranger »)  
- [ ] Script des commentaires oraux à portée de main  
- [ ] Enregistrement démarre **avant** le premier `1` dans le CLI  

---

## 5. Commandes « copier-coller » rapides

```powershell
# Terminal 1
cd hotel; java -cp "target/classes;target/dependency/*" hotel.ws.HotelPublisher 18080 hotel1

# Terminal 2
cd hotel; java -cp "target/classes;target/dependency/*" hotel.ws.HotelPublisher 18081 hotel2

# Terminal 3
cd agence; $env:HOTEL_URLS="http://localhost:18080/hotel,http://localhost:18081/hotel"; java -cp "target/classes;target/dependency/*" agence.AgencyApp
```

> Pour le refus « secret erroné » :  
> ```powershell
> $env:AGENCY_SECRET="mauvais_secret"
> # relancer l'AgencyApp dans T3
> ```

---

*Durée cible de la vidéo : 3–5 min.  Coupez les temps d'attente (prompts) au montage.*