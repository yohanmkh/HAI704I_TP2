# HAI704I · TP2 SOAP — Feuille de route (Partie A)

> Hôtels qui publient un service SOAP/WSDL + agence de voyage qui les interroge, compare, et réserve.
> Ce document est **ton plan de travail** : quoi faire, dans quel ordre, quelles décisions justifier, quoi capturer, quoi rendre.
> ⚠️ Les « éléments de réponse » sont des pistes pour comprendre. Reformule avec tes mots et déclare ton usage de l'IA dans la section A.10 du compte-rendu (voir §10).

---

## 0. Comprendre le TP en 2 minutes

**Ce qu'on construit**

```
                       ┌──────────────┐   SOAP    ┌─────────────────────┐
 Client (CLI) ───────► │   AGENCE     │ ────────► │ Hôtel 1 (port 8081) │
                       │ (1 processus)│           └─────────────────────┘
                       │              │   SOAP    ┌─────────────────────┐
                       │              │ ────────► │ Hôtel 2 (port 8082) │
                       └──────────────┘           └─────────────────────┘
        Le client ne parle jamais aux hôtels. Chaque hôtel = 1 processus, 1 port, 1 WSDL.
```

**Les 3 idées qui sont notées (ce que le prof veut voir que tu as compris)**

1. **Le contrat (WSDL) est l'objet central.** L'agence ne connaît des hôtels que leur WSDL : *aucune* classe partagée. Le client est **engendré** (`wsimport`), pas écrit.
2. **Séparer 3 responsabilités côté hôtel** : *point d'accès* (traduit le contrat) / *service métier* (décide) / *dépôt* (stocke). Le métier ne connaît ni SOAP ni la base.
3. **Raisonner « système distribué »** : refus métier ≠ panne, rejeu sans doublon (idempotence), panne partielle (un hôtel éteint ne bloque pas), prix annoncé = prix payé, donnée sensible (CVC, secret) qui traverse un service.

**Les deux projets Maven (séparés, aucune dépendance entre eux)**

| Projet | Rôle | Contient |
|---|---|---|
| `hotel/` | Serveur SOAP | point d'accès `@WebService`, service métier, dépôts (mémoire) |
| `agence/` | Client | classes **engendrées** + accès SOAP + logique d'agence + console |

---

## 1. Vue d'ensemble de la roadmap

| Étape | Objectif | Point de contrôle | Barème |
|---|---|---|---|
| **A0** | Observer un service SOAP, lancer `date-demo`, appeler à la main | **0** | (prépare tout) |
| **A1** | Contrat de l'hôtel : opération de **recherche** à gros grain | **A** | 2 + 1,5 |
| **A2** | Séparer point d'accès / métier / dépôt | **B** | 1 |
| **A3** | **Faute métier** déclarée (`@WebFault`) : agence non partenaire | **C** | 1,5 |
| **A4** | Projet `agence` : client **engendré**, adresse configurable | **D** | 1,5 |
| **A5** | **Réservation** : auth, paiement fictif, offre = devis, idempotence | **E** | 1,5 |
| **A6** | **Plusieurs hôtels**, tarifs par agence, panne partielle, timeouts | **F** | 1 |
| **A7** | **Du contrat au fil** : captures commentées, suivre une valeur | **G** | 1 |
| **A8** | **CLI** en 3 couches, robuste | **H** | 3 |
| *Rendu* | Compte-rendu, code, README, vidéo | — | 6 |

> **Règle d'or du sujet :** n'attaque pas l'étape N+1 tant que le point de contrôle N n'est pas franchi.
> **Bonus (B, C, D)** : uniquement après le point H. Max +3, ne servent qu'à compenser des points perdus.

### Répartition conseillée en binôme
Répartissez le *travail*, pas la *compréhension* : les deux doivent savoir expliquer **tout** le rendu.
Exemple : l'un prend A1→A3 (hôtel), l'autre A4 puis A8 (agence), puis vous faites A5, A6, A7 ensemble, et vous relisez mutuellement le code de l'autre.

---

## 2. Environnement et pièges classiques

- **Java 17+**, **Maven 3.9+** → `java -version`, `mvn -v`
- Pile : **Jakarta XML Web Services 4.0**, implémentation **Eclipse Metro** : `com.sun.xml.ws:jaxws-rt:4.0.5`
- Génération du client : `com.sun.xml.ws:jaxws-maven-plugin:4.0.5` (goal `wsimport`, car `wsimport` n'est plus dans le JDK)
- ✅ Imports `jakarta.jws.*`, `jakarta.xml.ws.*`, `jakarta.xml.bind.*` — ❌ jamais `javax.jws` / `javax.xml.ws` (tutoriels pré-2019)
- (`javax.crypto` du JDK, lui, reste normal : ce n'est pas du JAX-WS.)

| Piège | Symptôme | Parade |
|---|---|---|
| DTO sans constructeur sans paramètre | erreur JAXB à la réception | toujours un constructeur vide + getters/setters |
| `LocalDate` dans un DTO | JAXB ne sait pas le sérialiser | `XMLGregorianCalendar` (→ `xs:date`) ou `XmlAdapter` |
| `double` pour un prix | erreurs d'arrondi | `BigDecimal` (→ `xs:decimal`) |
| Ajouter `hotel` en dépendance de `agence` « pour que ça compile » | **zéro point sur A4** | ne référencer que le code engendré |
| `target/` ou `generated-sources` dans l'archive | pénalité de rendu | `.gitignore` + `mvn clean` avant de zipper |
| Exception non vérifiée (`RuntimeException`) | faute générique illisible | exception **vérifiée** + `@WebFault` pour les refus métier |
| Dump des messages laissé actif en vidéo | secret/CVC visibles | désactiver les dumps hors captures A7 |

---

## 3. A0 — Mise en route

### A0.1 Un service que nous ne contrôlons pas
Service : `https://www.dataaccess.com/webservicesserver/NumberConversion.wso`

- [ ] Ouvrir `…NumberConversion.wso?WSDL` ; repérer les opérations (`NumberToWords`, `NumberToDollars`) et lire les `<documentation>`.
- [ ] Appeler `NumberToWords` (SoapUI / Postman / curl) en **SOAP 1.1**.
- [ ] Refaire en **SOAP 1.2**.
- [ ] Regarder l'en-tête `Server` / `X-Powered-By` de la réponse.

```bash
# SOAP 1.1  (-i pour voir les en-têtes de réponse)
curl -i -X POST 'https://www.dataaccess.com/webservicesserver/NumberConversion.wso' \
  -H 'Content-Type: text/xml; charset=utf-8' \
  -d '<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <NumberToWords xmlns="http://www.dataaccess.com/webservicesserver/">
      <ubiNum>42</ubiNum>
    </NumberToWords>
  </soap:Body>
</soap:Envelope>'

# SOAP 1.2 : autre namespace d'enveloppe + autre Content-Type
curl -i -X POST 'https://www.dataaccess.com/webservicesserver/NumberConversion.wso' \
  -H 'Content-Type: application/soap+xml; charset=utf-8' \
  -d '<?xml version="1.0" encoding="utf-8"?>
<soap12:Envelope xmlns:soap12="http://www.w3.org/2003/05/soap-envelope">
  <soap12:Body>
    <NumberToWords xmlns="http://www.dataaccess.com/webservicesserver/">
      <ubiNum>42</ubiNum>
    </NumberToWords>
  </soap12:Body>
</soap12:Envelope>'
```
*(Vérifie le namespace et le nom du paramètre dans le WSDL : c'est justement l'exercice.)*

**Éléments de réponse — comparaison 1.1 / 1.2**

| | SOAP 1.1 | SOAP 1.2 |
|---|---|---|
| Namespace de l'enveloppe | `http://schemas.xmlsoap.org/soap/envelope/` | `http://www.w3.org/2003/05/soap-envelope` |
| `Content-Type` | `text/xml` | `application/soap+xml` |
| Action | en-tête HTTP `SOAPAction` | paramètre `action` du Content-Type |
| Corps de l'opération | identique | identique (seule l'enveloppe change) |

**Ce que montre l'en-tête `Server`/`X-Powered-By`** : le service n'est (très probablement) pas écrit en Java (typiquement IIS / ASP.NET — *à lire sur ta capture*), et pourtant on l'appelle sans rien savoir de son implémentation → **l'interopérabilité vient du contrat**, pas du langage.

📸 **C0.1** WSDL annoté (opérations + `<documentation>`) · **C0.2** réponse SOAP 1.1 (avec en-têtes) · **C0.3** réponse SOAP 1.2 · **C0.4** en-tête `Server`/`X-Powered-By` entouré.

### A0.2 Ton environnement, sur un service contrôlé (`date-demo`)
- [ ] `java -version` (≥ 17), `mvn -v` (≥ 3.9)
- [ ] Construire et lancer `date-demo/service` (suivre son `README.md`)
- [ ] Ouvrir `?wsdl` puis `?xsd=1` : **repérer les 5 parties** (voir ci-dessous)
- [ ] Envoyer une requête à la main, puis provoquer **volontairement** les erreurs décrites dans son README
- [ ] Construire/lancer `date-demo/client` ; ouvrir `client/target/generated-sources/`

**Les 5 parties d'un WSDL — où les trouver**

| Partie | Rôle | Où (Metro) |
|---|---|---|
| `types` | schéma XSD des éléments échangés | importé depuis `?xsd=1` |
| `message` | « paquets » d'entrée/sortie/faute, pointent vers un élément du schéma | `<message>` dans le WSDL |
| `portType` | l'**interface abstraite** : opérations + messages in/out/fault | `<portType>` |
| `binding` | comment ça circule : SOAP, `document`/`literal`, HTTP | `<binding>` + `<soap:binding style="document">` |
| `service` | **où** : `<port>` + `<soap:address location="…"/>` | fin du WSDL |

Lecture « de haut en bas » : `service` (adresse) → `binding` (protocole) → `portType` (opérations) → `message` → `types` (forme des données).

**Pourquoi provoquer des erreurs ?** Pour reconnaître plus tard une faute SOAP, savoir distinguer *message mal formé*, *mauvais namespace/élément*, *valeur hors énumération*, *mauvais Content-Type*.

📸 **C0.5** WSDL `date-demo` avec les 5 parties repérées · **C0.6** une erreur provoquée + réponse `<S:Fault>` · **C0.7** arborescence `generated-sources` du client.

> ✅ **Point de contrôle 0** : service publié, WSDL+XSD lus, requête écrite à la main OK, requête non conforme → erreur comprise, client engendré qui appelle le service, et tu sais montrer *d'où viennent* ses classes.

---

## 4. A1 — Le contrat de l'hôtel : une opération de recherche

**But** : un hôtel répond à « chambres à *ville*, du *date* au *date*, pour *n* personnes, ≥ *étoiles*, ≤ *prix max* » par une liste d'offres.

- [ ] Projet Maven `hotel` (Java 17, `jaxws-rt`)
- [ ] Interface `@WebService` (SEI) avec **une** opération de recherche, tout nommé explicitement
- [ ] 2 DTO : `RechercheRequete` (ville, dateArrivee, dateDepart, nbPersonnes, etoilesMin, prixMax, **agenceId**) et `OffreDto`
- [ ] `Endpoint.publish("http://localhost:8081/hotel", impl)` dans un `main`

```java
@WebService(name = "HotelPort", targetNamespace = "http://hai704i.example.org/hotel")
public interface HotelService {

    @WebMethod(operationName = "rechercherOffres")
    @WebResult(name = "offre")
    List<OffreDto> rechercherOffres(
        @WebParam(name = "requete") RechercheRequete requete
    ) throws AgenceNonPartenaireException;
}
```

**Décisions à justifier (à mettre dans le CR)**

- **Grain** : *gros grain* — une requête complète → une réponse complète. Un contrat `getVille()`, `getPrix()`… serait « RMI par-dessus SOAP » : un aller-retour réseau par attribut.
- **Cardinalités** : par défaut JAXB génère `minOccurs="0"` pour les objets. Les champs obligatoires (`ville`, dates…) → `@XmlElement(required = true)` pour que le schéma les impose. Les types primitifs (`int`) sont `minOccurs=1` d'office. Une `List` → `maxOccurs="unbounded"`.
- **DTO « bêtes »** : transportent, ne décident pas (aucune règle métier dedans).

> ✅ **Point de contrôle A** : `?wsdl` répond ; une opération dont le message d'entrée porte ta requête et le message de sortie une liste.

📸 **C1.1** `?wsdl` (portType + messages) · **C1.2** `?xsd=1` (types : requête, offre, cardinalités).

---

## 5. A2 — Ce que le contrat expose, et ce qu'il cache

**Structure cible côté `hotel`** :

```
hotel/src/main/java/.../
├── endpoint/      ← @WebService, DTO du contrat, @WebFault      (TRADUIT)
│   ├── HotelService.java / HotelServiceImpl.java
│   ├── dto/  RechercheRequete, OffreDto, ReservationRequete, ConfirmationDto, ...
│   └── faults/ ...Exception + bean de détail
├── metier/        ← ServiceHotelier, Chambre, Devis, Reservation, exceptions métier  (DÉCIDE)
│   └── AUCUNE annotation jakarta.jws / jakarta.xml.ws ici
├── depot/         ← interfaces ChambreDepot, DevisDepot, ReservationDepot, AgenceDepot  (STOCKE)
│   └── memoire/  implémentations en mémoire
└── HotelMain.java ← assemblage + Endpoint.publish
```

**L'adaptateur (`HotelServiceImpl`) fait, dans l'ordre** : (1) DTO reçu → données métier ; (2) appel du service métier ; (3) résultat métier → DTO ; (4) exception métier → faute déclarée du contrat.

**Éléments de réponse — « Pourquoi séparer ? »**
Trois questions indépendantes : *quelle est la logique ?* (métier) / *comment un client distant appelle-t-il ?* (SOAP) / *où range-t-on les données ?* (dépôt). Une classe à la fois publiée dans le WSDL et entité en base **couple** deux choses qui doivent évoluer séparément (changer de base casserait le contrat, changer le contrat casserait la base). Conséquence testable : le métier se teste sans serveur SOAP ; remplacer le dépôt ne touche ni le métier ni le WSDL.

**Vérification (étape 5)** : le WSDL ne doit montrer ni `Chambre`, ni tarifs internes, ni dépôt, ni liste des agences/secrets — seulement requête, offre, réservation, confirmation, fautes.

> ✅ **Point de contrôle B** : tu montres la classe qui traduit, celle qui décide, celle qui accède aux données ; le métier compile **sans JAX-WS** ; changer le dépôt ne demande de toucher ni le métier ni le contrat.

📸 **C2.1** arborescence des packages (3 couches) · **C2.2** WSDL/XSD sans `Chambre`/`Tarif` · *(preuve de compilation du métier sans jaxws : voir `pom`/imports)*.

---

## 6. A3 — Faute métier dans le contrat

**But** : une agence inconnue ne reçoit **pas** une liste vide, mais un refus explicite.

```java
@WebFault(name = "AgenceNonPartenaire", targetNamespace = "http://hai704i.example.org/hotel")
public class AgenceNonPartenaireException extends Exception {   // vérifiée = apparaît dans le contrat
    private final RefusDetail faultInfo;                         // bean de détail → <detail>
    public AgenceNonPartenaireException(String message, RefusDetail faultInfo) {
        super(message); this.faultInfo = faultInfo;
    }
    public RefusDetail getFaultInfo() { return faultInfo; }
}
```
+ `throws AgenceNonPartenaireException` dans la signature de l'opération.

- [ ] Republier, relire le WSDL : un `<fault>` apparaît dans l'opération (et dans le `portType`).
- [ ] Provoquer le refus à la main (SoapUI/curl) → **garder la réponse**.

**Éléments de réponse — vérifiée vs non vérifiée** : une exception vérifiée devient un `<fault>` *dans le contrat* (le client sait à l'avance qu'elle peut arriver, `wsimport` génère l'exception correspondante). Une `RuntimeException` n'est **pas** dans le contrat : elle voyage quand même, mais en faute générique SOAP, souvent illisible. C'est une **décision de conception** : refus prévisible et actionnable → faute déclarée ; bug interne → faute générique.

> ✅ **Point de contrôle C** : le WSDL déclare le `<fault>`. Requête d'une agence non partenaire → **HTTP 500** + enveloppe contenant `<S:Fault>` avec un `<detail>` portant ton type de faute.

📸 **C3.1** WSDL avec `<fault>` · **C3.2** requête de l'agence inconnue + réponse `HTTP 500 / <S:Fault>` (avec `<detail>`) ★ *(à reprendre dans `captures/` pour A7)*.

---

## 7. A4 — L'agence : client engendré depuis le contrat

- [ ] Projet Maven `agence` **séparé** ; **aucune** dépendance vers `hotel`
- [ ] `jaxws-maven-plugin:4.0.5`, goal `wsimport`, à partir du WSDL de l'hôtel (suivre le `pom` de `date-demo/client`)
- [ ] Adresse du service **configurable à l'exécution** (propriété `-D…`, argument ou fichier `.properties`)
- [ ] Afficher les offres reçues

```java
HotelService_Service svc = new HotelService_Service();      // noms engendrés : à lire dans generated-sources
HotelPort port = svc.getHotelPortPort();
((BindingProvider) port).getRequestContext().put(
    BindingProvider.ENDPOINT_ADDRESS_PROPERTY, adresseDepuisConfig);   // ← pas en dur
```

💡 *Astuce reproductibilité* : copier le WSDL (et son XSD) dans `agence/src/main/resources/wsdl/` pour que la construction ne dépende pas d'un hôtel démarré (cohérent avec « le client dépend du **contrat**, pas du serveur »). À documenter dans le README.

**Pourquoi c'est noté** : si `agence` référence une classe de `hotel`, le client n'est plus interopérable et on ne teste plus le contrat.

> ✅ **Point de contrôle D** : depuis un autre processus, l'agence affiche les offres ; les classes manipulées sont dans `target/generated-sources/` (tu sais montrer d'où vient chaque nom) ; l'adresse se change sans recompiler.

📸 **C4.1** `pom.xml` (plugin wsimport) · **C4.2** arborescence `generated-sources` · **C4.3** sortie console avec les offres · **C4.4** même appel avec une autre adresse (preuve de configurabilité).

---

## 8. A5 — Réserver : écriture, authentification, refus, rejeu

C'est **l'étape la plus riche** (et celle où se jouent 4 des décisions à discuter dans le CR).

### 8.1 Le contrat de réservation

Une seule requête contient : `agenceId` + **`secretPresente`** · `offreId` · voyageur (nom, prénom) · paiement (numéro, moisExp, anneeExp, cvc) · **`idDemande`** (choisi par l'agence). Réponse : confirmation avec **référence de réservation**.

**Types imbriqués, pas à plat** :
```
ReservationRequete
 ├─ AuthAgence   { agenceId, secretPresente }
 ├─ offreId
 ├─ Voyageur     { nom, prenom }
 ├─ Paiement     { numeroCarte, moisExpiration, anneeExpiration, cvc }
 └─ idDemande
```
Effet visible dans le schéma : des `xs:complexType` nommés, réutilisables et lisibles, au lieu d'une requête de 10 champs plats ; chaque « tout » (identité, paiement) devient un bloc cohérent.

### 8.2 Qu'est-ce qu'une « offre » ? → **un devis**
Un devis = proposition **datée**, à un **prix précis**, pour **une agence précise** (id unique, hôtel, chambre, dates, prix total, agence, date d'expiration, état).

| Option | Problème |
|---|---|
| Offre = identifiant de chambre | rien ne garantit le prix, les dates ni l'agence → « prix annoncé ≠ prix payé » (l'erreur n°1 de l'étape) |
| **Offre = devis** ✅ | à la réservation l'hôtel retrouve le devis : prix **confirmé = prix annoncé**, bonne agence, bonnes dates, pas expiré |

→ La recherche **crée et stocke** les devis (donc `DevisDepot`) ; la réservation référence `offreId` = id du devis.

### 8.3 Pourquoi authentifier l'écriture mais pas la lecture ?
Éléments de réponse : la recherche est sans effet de bord (aucun état modifié, pas d'argent) ; elle n'exige que l'identité de l'agence (pour le tarif propre et le refus des non-partenaires). La réservation **modifie l'état**, engage un paiement et bloque une chambre : il faut prouver que l'appelant *est bien* l'agence. (Limite à citer au regard critique : sans HTTPS ni WS-Security, l'identité de la recherche est usurpable et le secret est lisible sur le réseau.)

### 8.4 Secret vs empreinte (nommer **deux valeurs différentes**)

| Valeur | Où | Nom conseillé |
|---|---|---|
| Ce que l'agence envoie sur le réseau | message SOAP | `secretPresente` |
| Ce que l'hôtel range | dépôt des agences | `sel` + `empreinteSecret` |

Hôtel : **jamais** de secret en clair → empreinte **salée** par fonction de dérivation, comparée en **temps constant**.

```java
// PBKDF2 (JDK) : sel aléatoire par agence, nombre d'itérations élevé
byte[] sel = new byte[16]; new SecureRandom().nextBytes(sel);
PBEKeySpec spec = new PBEKeySpec(secret.toCharArray(), sel, 600_000, 256);
byte[] empreinte = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                                    .generateSecret(spec).getEncoded();
// vérification : recalculer avec le sel stocké puis comparer SANS court-circuit
boolean ok = MessageDigest.isEqual(empreinteStockee, empreinteRecalculee);
```
*Pourquoi temps constant ?* `Arrays.equals`/`equals` s'arrêtent au premier octet différent : la durée de réponse fuit de l'information. *Pourquoi un sel ?* deux agences au même secret ont des empreintes différentes, et les tables précalculées deviennent inutiles. *Pourquoi l'empreinte seule ne sert à rien envoyée ?* L'hôtel la re-hacherait : la comparaison échouerait.

Côté agence : le secret ne doit **pas** être dans le code → `-Dagence.secret=…` ou fichier de config hors dépôt/exemple fictif documenté.
Côté hôtel : agences partenaires chargées depuis un fichier de config contenant `agenceId`, `sel`, `empreinte` (petit utilitaire pour les générer).
Un message SOAP nu ne protège rien : **WS-Security / HTTPS ne sont pas demandés** — mais mentionne-le dans le regard critique.

### 8.5 Données de paiement (fictives, mais traitées comme réelles)
- Numéros **fictifs uniquement** (aucun vrai numéro dans code, jeux d'essai, captures, CR, vidéo).
- **CVC** : jamais conservé (ni fichier, ni structure qui survit à la requête, ni trace), absent de la confirmation.
- **Numéro complet** : jamais conservé ; si trace du règlement → 4 derniers chiffres (`**** **** **** 1234`).
- Ne jamais journaliser la requête telle quelle ; `toString()` d'une classe ne doit **jamais** révéler secret ou numéro (le masquer ou ne pas l'afficher).

**Contrôles** : longueur du numéro, mois ∈ [1,12], année non révolue (et mois/année cohérents avec aujourd'hui), CVC numérique (3–4 chiffres).

### 8.6 Quels refus métier déclarer comme fautes ?
Règle : on déclare un `<fault>` quand le client **peut et doit réagir** différemment.

| Refus | Déclaré dans le contrat ? | Justification |
|---|---|---|
| Agence inconnue / secret erroné | ✅ `AuthentificationRefusee` | l'agence doit corriger sa config ; ne pas révéler *lequel* des deux (anti-énumération) |
| Offre inexistante / expirée / déjà prise | ✅ `OffreIndisponible` (avec un motif) | l'utilisateur doit choisir une autre offre → on continue |
| Devis appartenant à une autre agence | ✅ (même faute, motif distinct) ou refus d'accès | ne pas fuiter l'existence du devis |
| Paiement invalide (numéro, date, CVC) | ✅ `PaiementInvalide` (champ fautif) | l'utilisateur peut resaisir |
| Requête mal formée (dates incohérentes) | ✅ `DemandeInvalide` | corriger la saisie |
| Bug interne, base indisponible | ❌ non déclarée (faute générique) | ce n'est pas une règle métier ; c'est une erreur technique |

Côté agence, **trois catégories bien distinctes** (c'est une des 4 décisions à discuter) :

| Cause | Ce que voit le code client | Réaction |
|---|---|---|
| Refus métier (faute déclarée) | exception **engendrée** par `wsimport` (vérifiée, souvent suffixée `_Exception`) | message clair, l'utilisateur continue |
| Panne réseau / timeout / adresse erronée | `jakarta.xml.ws.WebServiceException` (cause `ConnectException`, `SocketTimeoutException`…) | « hôtel X injoignable », on continue avec les autres |
| Erreur serveur imprévue | `SOAPFaultException` (faute générique) | signaler « erreur technique chez l'hôtel » |

### 8.7 Idempotence : rejouer sans réserver deux fois
Situation : l'agence envoie la réservation, la réponse se perd (timeout) ; elle ne sait pas si c'est fait. Elle rejoue → risque de **double réservation**.

- [ ] L'agence **engendre** un `idDemande` (ex. UUID) **avant** l'envoi, et **le réutilise** pour tout rejeu
- [ ] L'hôtel garde `(agenceId, idDemande) → réservation` ; si la clé existe → renvoie la **même** réservation, avec un indicateur `dejaEnregistree = true`
- [ ] Distinguer : **(a) rejeu** de la même demande → sans effet supplémentaire ; **(b) nouvelle demande** sur offre désormais prise → **refus** `OffreIndisponible`

**Ordre des contrôles côté hôtel (important)** :
1. authentifier l'agence ; 2. **chercher d'abord un rejeu** `(agenceId, idDemande)` ; 3. valider le paiement ; 4. retrouver le devis (existe ? bonne agence ? non expiré ? encore libre ?) ; 5. créer la réservation, bloquer la chambre, stocker `idDemande`, **4 derniers chiffres seulement**.
→ Si on testait la disponibilité *avant* le rejeu, un rejeu légitime d'une demande réussie serait refusé (« offre indisponible », car… elle a été prise par cette même demande).
→ Rendre l'étape 4–5 **atomique** (`synchronized` ou structure concurrente) : deux demandes simultanées sur la même offre ne doivent pas réussir toutes les deux.

> ✅ **Point de contrôle E** :
> (1) Réservation OK avec paiement fictif → référence ; prix confirmé = annoncé.
> (2) Même demande + **mauvais secret** → refusée, rien de réservé.
> (3) **Rejeu** (même `idDemande`) → même réservation, aucune seconde.
> (4) **Nouvelle** demande sur l'offre prise → refus métier lisible (pas de trace d'exception).
> (5) Preuve côté hôtel que **ni CVC ni numéro complet** ne subsistent nulle part.

📸 **C5.1** XSD de la réservation (types imbriqués) · **C5.2** réservation OK (requête + réponse, **données fictives et secret de démo**) ★ · **C5.3** secret erroné → faute · **C5.4** rejeu → même référence + `dejaEnregistree` · **C5.5** offre indisponible → faute · **C5.6** preuve « pas de CVC/numéro » : classe `Reservation` (champ `quatreDerniers`, pas de `cvc`), logs serveur propres, dépôt inspecté au débogueur.

---

## 9. A6 — Plusieurs hôtels, tarifs par agence, panne partielle

- [ ] **≥ 2 hôtels** = 2 processus, 2 ports (ex. 8081 et 8082), chambres/tarifs différents (idéalement une config par hôtel)
- [ ] L'agence interroge **tous** les hôtels connus, **agrège** en une liste triée utilement (prix croissant, ou étoiles puis prix…)
- [ ] **Tarif propre à l'agence** : ex. coefficient par `(hôtel, agence)` — vérifier que `AgenceA` et `AgenceB` ne voient pas le même prix
- [ ] **Éteindre un hôtel** → la recherche aboutit quand même et signale en une ligne l'hôtel muet
- [ ] **Timeouts** sur les appels

```java
// Metro : timeouts en millisecondes sur le proxy
Map<String,Object> ctx = ((BindingProvider) port).getRequestContext();
ctx.put("com.sun.xml.ws.connect.timeout", 2000);
ctx.put("com.sun.xml.ws.request.timeout", 3000);
```
Interrogation **en parallèle** (`ExecutorService` / `CompletableFuture` + `orTimeout`) : le temps total reste borné même si un hôtel est lent, et chaque résultat est soit `offres`, soit `refus`, soit `injoignable`.

**Éléments de réponse — défaillance partielle** : un système distribué n'est pas « marche/panne » ; il est *partiellement* disponible. Faire échouer toute la recherche parce qu'un hôtel sur trois est éteint est une mauvaise conception : on dégrade (résultats partiels + avertissement) plutôt que d'échouer.

> ✅ **Point de contrôle F** : offres de plusieurs hôtels, prix dépendant de l'agence ; avec un hôtel éteint, la recherche aboutit quand même et l'incident est signalé en une ligne claire.

📸 **C6.1** deux hôtels lancés (deux ports) · **C6.2** recherche agrégée de l'agence A · **C6.3** même recherche par l'agence B (prix différents) · **C6.4** un hôtel arrêté → message « hôtel X injoignable » + offres des autres.

---

## 10. A7 — Du contrat au fil (captures obligatoires dans l'archive)

**À capturer pour ton service** (dossier `captures/`) : ★
1. le **WSDL et son schéma** ;
2. une **requête de recherche + sa réponse** ;
3. une **requête de réservation + sa réponse** ;
4. une **réponse de faute métier**.

*Comment capturer* : SoapUI/Postman/curl `-v`, ou les dumps Metro (à n'activer que pour ces captures) :
`-Dcom.sun.xml.ws.transport.http.client.HttpTransportPipe.dump=true` (client) et `-Dcom.sun.xml.ws.transport.http.HttpAdapter.dump=true` (serveur). Avec des **données fictives**.

**À identifier et expliquer sur ces captures**

| Question | Comment la repérer |
|---|---|
| **Version de SOAP** | namespace de l'enveloppe (`…/soap/envelope/` = 1.1 ; `…/2003/05/soap-envelope` = 1.2) + `Content-Type` (`text/xml` vs `application/soap+xml`). Metro publie en SOAP 1.1 par défaut. |
| **Élément enveloppe de l'opération** | l'unique enfant de `<S:Body>`, ex. `<ns2:rechercherOffres>` : c'est l'élément « wrapper » (style document/literal wrapped) ; son nom = opération. |
| **Suivre une valeur** | choisis ex. `prixTotal` : `BigDecimal prixTotal` (Java hôtel) → `<xs:element name="prixTotal" type="xs:decimal">` (XSD) → `<prixTotal>312.50</prixTotal>` (message) → `BigDecimal getPrixTotal()` (classe engendrée côté agence). |
| **Classe hôtel vs classe engendrée** | *en commun* : **uniquement la forme XML** (noms d'éléments, namespace, types, cardinalités). *Pas partagé* : le code, les méthodes métier, le package, les annotations propres, les constructeurs/`toString`, la hiérarchie Java, les classes internes (`Chambre`…). L'objet reçu par l'agence **n'est pas** l'objet de l'hôtel : c'est une *reconstruction* à partir du contrat. |
| **Décision de conception visible sur le fil** | gros grain (une seule requête englobant tout) · types imbriqués (`<paiement>` dans la requête) · devis (`offreId` stable) · refus modélisé (`<detail>` typé dans `<S:Fault>`) · cardinalité (`maxOccurs="unbounded"` des offres) |

> ✅ **Point de contrôle G** : sur **tes** captures tu désignes la version de SOAP, l'élément enveloppe, le chemin complet d'une valeur Java → classe engendrée, tu expliques pourquoi l'objet reçu n'est pas celui de l'hôtel, et tu montres une décision de conception dans le message.

📸 **Dossier `captures/` conseillé** : `A7_01_wsdl.png`, `A7_02_xsd.png`, `A7_03_recherche_requete.png`, `A7_04_recherche_reponse.png`, `A7_05_reservation_requete.png`, `A7_06_reservation_reponse.png`, `A7_07_faute_metier.png` — chacune **commentée dans le CR** (annotations/flèches bienvenues).

---

## 11. A8 — Une CLI utilisable, en 3 couches

```
console/        saisie, affichage, menu           ← AUCUN import de classe engendrée, aucune URL
   │
agence/         logique : interroger, agréger, trier, choisir, réserver
   │               (modèle propre à l'agence + interface « PasserelleHotel »)
soap/           proxies engendrés, adresses, timeouts  ← seul endroit qui connaît SOAP
   │               (adapte classes engendrées ↔ modèle agence ; fautes → exceptions de l'agence)
hôtels distants
```

- [ ] Menu : **rechercher** · **voir les offres** · **choisir** · **réserver** (identité + paiement fictif) · **afficher la référence** · **quitter**
- [ ] La console ne contient **aucun appel SOAP** ni adresse
- [ ] Refus métier → message lisible, **on continue**
- [ ] Saisies invalides (date mal formée, n° d'offre hors liste, champ vide) → message + on redemande, **sans quitter**
- [ ] Le CVC et le numéro complet ne sont **jamais réaffichés ni écrits** (ni écho de saisie, ni log)
- [ ] Secret de l'agence **jamais en dur** (`-Dagence.secret` / fichier de config)
- [ ] Hôtel éteint / adresse fausse → message compréhensible, **pas de stack trace**
- [ ] **Sortie propre** (option « quitter », fermeture de l'exécuteur, code retour 0)

💡 La frontière « console / logique / SOAP » doit pouvoir se **désigner dans le code** : prévois un schéma des paquetages pour le CR.

> ✅ **Point de contrôle H** : avec ≥ 2 hôtels, un utilisateur qui n'a pas lu le code cherche, compare, réserve et obtient sa référence ; il se trompe deux fois (saisie invalide, offre indisponible) sans que le programme s'arrête ; un hôtel est éteint et il le comprend ; il quitte proprement ; **sans recompilation** ; ses coordonnées de paiement ne réapparaissent nulle part.

📸 **C8.1** menu · **C8.2** saisie invalide gérée · **C8.3** refus métier affiché · **C8.4** hôtel injoignable · **C8.5** sortie propre · **C8.6** schéma des 3 couches (ou arborescence).

---

## 12. Récapitulatif : toutes les captures et où les utiliser

| ID | Étape | Contenu | Obligatoire ? | Place dans le CR |
|---|---|---|---|---|
| C0.1–C0.4 | A0.1 | WSDL NumberConversion, SOAP 1.1/1.2, en-tête `Server` | recommandé | §A0 (comparaison 1.1/1.2) |
| C0.5–C0.7 | A0.2 | `date-demo` : 5 parties WSDL, erreur provoquée, `generated-sources` | recommandé | §A0 |
| C1.1–C1.2 | A1 | `?wsdl`, `?xsd=1` de l'hôtel | recommandé | §A1 (grain, cardinalités) |
| C2.1–C2.2 | A2 | packages 3 couches ; WSDL sans internes | recommandé | §A2 (séparation) |
| **C3.2** | A3 | requête agence inconnue + `<S:Fault>` | ★ (A7) | §A3 + §A7 |
| C4.1–C4.4 | A4 | pom wsimport, `generated-sources`, offres, autre adresse | recommandé | §A4 |
| C5.1–C5.6 | A5 | types imbriqués, OK, secret erroné, rejeu, indisponible, preuve sans CVC | recommandé (très utile) | §A5 (4 décisions) |
| C6.1–C6.4 | A6 | 2 hôtels, tarifs A vs B, hôtel éteint | recommandé | §A6 |
| **A7_01–A7_07** | A7 | WSDL+XSD, recherche, réservation, faute | **★ obligatoires (dossier `captures/`)** | §A7 commentées |
| C8.1–C8.6 | A8 | menu, saisie invalide, refus, panne, sortie | recommandé | §A8 / synthèse |

> ⚠️ **Avant chaque capture** : données fictives uniquement ; aucun vrai numéro bancaire ; secret de démonstration (pas un vrai mot de passe).

---

## 13. Livrables finaux

Une archive **unique par binôme** : `NOM1_NOM2_HAI704I_TP2.zip` (ou `NOM_Prenom_HAI704I_TP2.zip` si seul), déposée dans **l'espace Moodle de ton parcours (GL/IASD ≠ ICO)**, avant l'échéance affichée.

```
NOM1_NOM2_HAI704I_TP2.zip
├── Compte-Rendu.pdf
├── README.pdf
├── source/
│   ├── hotel/
│   └── agence/
├── captures/            (WSDL, requêtes, réponses, faute : étape A7)
├── video/
│   └── demonstration.mp4   (ou liens.txt si vidéo externe)
└── liens.txt            (dépôt Git, vidéo : facultatif si déjà dans le CR)
```

### 13.1 `Compte-Rendu.pdf` — rapport d'ingénierie (2 pts)
- [ ] **Page de titre** : université/formation, **HAI704I — Architectures logicielles distribuées**, n° et titre du TP, enseignant (Bachar Rima), année 2026–2027, date, et **pour chaque membre** : nom, prénom, n° étudiant, parcours
- [ ] Table des matières · rappel bref des objectifs
- [ ] Compte-rendu **étape par étape** (A0 → A8), structuré en sections (pas un récit continu) : *implémenté, démarche, décisions + justification, validation*
- [ ] **Les 4 décisions obligatoirement discutées** :
  1. **grain** des opérations du contrat (A1) ;
  2. **ce qu'est une offre** → devis (A5) ;
  3. **rejeu sans effet supplémentaire** → `idDemande` + ordre des contrôles (A5) ;
  4. **refus métier vs panne** (A5, A6).
- [ ] Pourquoi **écriture authentifiée / lecture non** (A5.4)
- [ ] Schémas **utiles** : classes, séquence d'un appel SOAP, déploiement (processus + ports)
- [ ] Messages capturés (A7) **commentés**
- [ ] Extraits de code **choisis** (pas de listing)
- [ ] **Synthèse** : ce qui marche / ne marche pas et pourquoi, difficultés et résolutions
- [ ] **Regard critique** : améliorations (HTTPS/WS-Security, persistance, tests, idempotence persistante après redémarrage, expiration des devis, ergonomie…)
- [ ] Section **« Usage des outils d'IA générative »** : si utilisé ; lesquels ; pour quelles tâches ; ce qui a influencé le rendu ; comment tu as vérifié/corrigé ; ce qui est entièrement de toi. (Si non utilisé : le dire.)
- [ ] Références (style constant) · **URL du dépôt Git** si utilisé · **URL de la vidéo** si hébergée à l'extérieur

### 13.2 `source/` (code)
- [ ] Projets complets compilables et exécutables
- [ ] **Sans** `target/`, classes `wsimport`, caches IDE (`.idea`, `.settings`…), dépendances embarquées
- [ ] Si l'archive dépasse ~50 Mo (ou la limite Moodle) → lien Git stable (URL dans le CR)
- [ ] `agence` ne référence **aucune** classe de `hotel` · métier sans JAX-WS · console sans SOAP

### 13.3 `README.pdf` — un tiers doit pouvoir tout reconstituer (1 pt)
- [ ] Rôle de chaque projet + relations · structure des sources · dépendances et versions
- [ ] **JDK et Maven exacts** · commandes de construction **dans l'ordre**
- [ ] **Où et comment le client est engendré** (wsimport, `generated-sources`)
- [ ] **Ordre de démarrage** (hôtels puis agence) · **ports** de chaque hôtel · adresses configurées pour l'agence
- [ ] **Où se configure le secret** de l'agence · **jeu de données fictif** de la démo (agences, secrets de démo, offres, cartes fictives)
- [ ] Organisation du client (console / logique / SOAP) · lancement de la CLI, menu, conventions de saisie, arrêt propre
- [ ] Parties bonus faites (le cas échéant) · IDE utilisé (ou éditeur + CLI)

*(Tu peux écrire le README en Markdown puis l'exporter en PDF, ex. `pandoc README.md -o README.pdf`.)*

### 13.4 Vidéo de démonstration (2 pts) — application en cours d'exécution, pas un diaporama

**Script conseillé**
1. Démarrer **≥ 2 hôtels** puis **l'agence** (dans cet ordre)
2. CLI interactive : **recherche** → **comparaison** des offres → **choix** → **réservation** → **référence**
3. **Prix différents** selon l'agence (relancer avec l'autre agence/secret)
4. **Refus métier** (offre déjà prise, ou paiement invalide) + **secret erroné** refusé
5. **Rejeu** sans doublon (si tu peux le montrer via un mode de test ou un outil)
6. **Hôtel éteint** → message de l'agence
7. **Une saisie invalide** → le programme continue
8. **Arrêt propre**

⚠️ Un `main` qui déroule seul un scénario codé en dur n'est **pas** acceptable. Aucune donnée bancaire réelle à l'écran ; dumps de messages **désactivés**. Commentaire oral, sous-titres ou annotations : au choix. Hébergement stable (inclus, Git ou YouTube « non répertorié ») → URL dans le CR **et** le README.

### 13.5 Checklist finale avant dépôt
- [ ] Points de contrôle **0, A, B, C, D, E, F, G, H** tous franchis
- [ ] ≥ 2 hôtels + 1 agence en **processus séparés**
- [ ] Rejeu → pas de seconde réservation · secret erroné → refus, rien réservé
- [ ] Aucune donnée bancaire réelle nulle part ; CVC et numéro complet non conservés ; secret externalisé
- [ ] Hôtel éteint ≠ échec de la recherche
- [ ] Captures A7 dans `captures/` **et** commentées dans le CR
- [ ] Cohérence **CR ↔ code ↔ vidéo** (une incohérence coûte des points)
- [ ] Archive nommée `NOM1_NOM2_HAI704I_TP2.zip`, sans artefacts engendrés (`mvn clean`)
- [ ] Déposée dans le **bon** espace Moodle, avant l'échéance

---

## 14. Barème — où se jouent les points (20)

| Bloc | Pts | Ce qu'il faut absolument avoir |
|---|---|---|
| Contrat & mise en œuvre SOAP | **8** | WSDL exploitable (2) · grain/offre assumés (1,5) · fautes métier déclarées + `<S:Fault>` (1,5) · client engendré indépendant (1,5) · réservation authentifiée/cohérente/idempotente (1,5) |
| Architecture & raisonnement distribué | **3** | séparation 3 couches (1) · multi-hôtels + panne partielle (1) · vérification sur le fil A7 (1) |
| Application utilisable | **3** | CLI fonctionnelle (1,5) · robustesse + séparation console/SOAP (1,5) |
| Communication & reproductibilité | **6** | CR (2) · vidéo (2) · README (1) · déclaration IA (1) |

---

## 15. Bonus (seulement après le point H ; plafonné à +3)

| Bonus | Pts | Idée |
|---|---|---|
| **B1** Spring Boot + CXF | +0,5 | republier le **même** contrat/métier/dépôt via un `@Bean` CXF ; le client de la partie A marche **sans recompilation** (seule l'adresse change) |
| **B2** Injection / config | +0,5 | beans injectés par constructeur, `application.properties` (nom d'hôtel, ville, port) → 2 hôtels = 2 fichiers de config |
| **B3** JPA/H2 | +0,5 | 2ᵉ implémentation des dépôts, choix par config ; `@Entity` ≠ type de contrat (conversion explicite) ; WSDL **inchangé** |
| **C** Image `xs:base64Binary` + MTOM | +0,5 | mesurer l'écart de taille (≈ +33 % en Base64) ; MTOM = pièce MIME brute + référence XOP ; contrat inchangé |
| **D** GUI | +1 | Swing/JavaFX qui réutilise **la même logique d'agence** ; appels hors du fil graphique (`SwingUtilities.invokeLater`) |

---

## 16. Rappel intégrité / IA

L'usage de l'IA est **autorisé s'il reste un appui et s'il est déclaré** : expliquer une notion, comprendre une erreur, discuter des alternatives. Ce qui n'est **pas** acceptable : rendre du code que tu ne saurais pas justifier, ou un CR qui ne correspond pas au code/à la vidéo. L'évaluation porte sur la **transparence, la compréhension démontrée, la cohérence CR/code/vidéo** et la reproductibilité. Prépare-toi à expliquer **tout** le rendu à deux, à l'oral si besoin.
