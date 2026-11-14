# OneMoreBite

API Java 17 (HTTP server natif) pour la gestion d'une plateforme anti-gaspillage alimentaire.

## Fonctionnalités

- Authentification Admin, Commerçant & Client avec JWT et BCrypt
- Inscription, connexion, session et déconnexion des clients
- Gestion du profil de tout utilisateur connecté : modification partielle (`PUT /auth/me`) et suppression du compte (`DELETE /auth/me`) avec révocation immédiate du token
- Cycle de vie des commerçants : inscription `EN_ATTENTE` → validation admin `ACTIF` / `REJETE`
- Gestion des produits par les commerçants : CRUD, stock, disponibilité, images Cloudinary
- Gestion et modération des produits par l'administrateur : filtres, CRUD, statistiques
- Upload / suppression d'images via l'API Cloudinary
- Réservations clients : création, consultation, modification et annulation (`EN_ATTENTE` uniquement)
- Réservations commerçants : consultation des réservations reçues, confirmation et refus (`EN_ATTENTE` uniquement)
- Gestion des avis clients sur les commerçants : création, liste publique, filtre, note moyenne, modification et suppression modérée
- Supervision administrateur des utilisateurs : liste filtrée par rôle/statut, activation et désactivation sécurisées
- Supervision administrateur de toutes les réservations de la plateforme avec filtres
- Endpoint public des commerçants actifs pour le frontend React
- CORS complet (y compris preflight `OPTIONS`) et port HTTP configurable

## Prérequis

- Java 17
- Maven 3.9+
- MySQL 8+ sur `localhost:3306` (utilisateur `root`, mot de passe vide par défaut)

## Initialisation de la base

1. Démarrer MySQL.
2. Exécuter `src/main/resources/database/database.sql`, ou lancer la classe `DatabaseConnection` qui crée la base et charge le script.
3. Lancer `Main` (serveur sur `http://localhost:8081` par défaut).

Le port HTTP peut être changé avec la variable d'environnement `PORT` (ou la
propriété système `-Dserver.port=...`). La valeur par défaut `8081` correspond à
la variable `base_url` de la collection Postman.

Comptes de démonstration (script SQL) :

| Compte | Email | Mot de passe |
| --- | --- | --- |
| Admin | `admin@enicar.com` | `admin` |
| Commerçant | `contact@benali.tn` | `pass123` |
| Client | `sami@gmail.com` | `client123` |

## Configuration Cloudinary

Les images produits sont stockées sur Cloudinary ; la base de données ne conserve que l'URL (`produit.image_url`).

Définir les variables d'environnement suivantes avant de lancer l'application :

```powershell
$env:CLOUDINARY_CLOUD_NAME="votre_cloud_name"
$env:CLOUDINARY_API_KEY="votre_api_key"
$env:CLOUDINARY_API_SECRET="votre_api_secret"
$env:CLOUDINARY_FOLDER="onemorebite/produits"   # optionnel
```

Ou via des propriétés système :

```text
-Dcloudinary.cloud.name=...
-Dcloudinary.api.key=...
-Dcloudinary.api.secret=...
-Dcloudinary.folder=onemorebite/produits
```

Sans configuration, l'upload renvoie une erreur `503` avec un message explicite. Il reste possible de renseigner directement une URL Cloudinary (ou toute URL `https://`) dans le champ `imageUrl` d'un produit.

## Routes principales

### Authentification

| Méthode | Route | Description |
| --- | --- | --- |
| POST | `/auth/login` | Connexion Admin / Commerçant / Client |
| POST | `/auth/client/register` | Inscription d'un client (rôle `CLIENT` imposé) |
| POST | `/auth/client/login` | Connexion d'un client |
| GET | `/auth/me` | Profil de la session courante (Bearer Token) |
| PUT / PATCH | `/auth/me` | Modifier partiellement son profil (`nom`, `email`, `telephone`, `motDePasse`, `ancienMotDePasse`, `localisation` commerçant) |
| DELETE | `/auth/me` | Supprimer son compte et révoquer immédiatement le token |
| POST | `/auth/logout` | Déconnexion et révocation du token JWT |

`PUT /auth/me` est ouvert à tous les rôles (`CLIENT`, `COMMERCANT`, `ADMIN`).
Seuls les champs présents (non-null) dans le JSON sont modifiés. Le nouveau mot
de passe est haché avec BCrypt et `ancienMotDePasse` est vérifié lorsqu'il est
fourni. `localisation` n'est appliquée que pour un `COMMERCANT`.

`DELETE /auth/me` supprime la ligne `utilisateur` : la ligne `client`,
`commercant` ou `administrateur` et les avis sont nettoyés par les contraintes
`ON DELETE CASCADE` du schéma. Les réservations du client sont supprimées
explicitement dans le DAO (la contrainte `commande.client_id` étant
`ON DELETE SET NULL`), et la suppression d'un commerçant cascade sur ses
produits puis sur les réservations associées. Le compte administrateur principal
ne peut pas être supprimé (`403`) afin de toujours conserver un administrateur
actif. Le token JWT utilisé est ajouté à la liste de révocation serveur, comme
pour `POST /auth/logout`.

### Public

| Méthode | Route | Description |
| --- | --- | --- |
| GET | `/produits` | Liste des produits (`?commercantId=2&disponible=true&recherche=panier`) |
| GET | `/produits/{id}` | Détail d'un produit |
| GET | `/commercants` | Commerçants `ACTIF` (`?recherche=...` sur le nom ou la localisation) |
| GET | `/avis` | Liste publique des avis (`?commercantId=2`) |
| GET | `/commercants/{id}/avis` | Avis d'un commerçant, note moyenne et nombre total |

`GET /commercants` est public et retourne uniquement des données non sensibles :
`id`, `nom`, `localisation`, `telephone`. L'email, le mot de passe et les statuts
internes ne sont jamais exposés.

### Avis (Client, Bearer Token `CLIENT`)

Le client auteur d'un avis est toujours déduit du token JWT ; aucun `clientId`
n'est accepté dans le corps de la requête. Les avis sont rattachés à un
commerçant (`commercant_id`) et non à un produit.

| Méthode | Route | Description |
| --- | --- | --- |
| POST | `/avis` | Créer un avis (`commercantId`, `note` 1-5, `commentaire`) |
| PUT | `/avis/{id}` | Modifier son avis (`note`, `commentaire`) |
| DELETE | `/avis/{id}` | Supprimer son avis (le propriétaire ou un `ADMIN`) |

`GET /commercants/{id}/avis` retourne un objet
`{ commercantId, commercantNom, noteMoyenne, nombreAvis, avis: [...] }`.

### Commerçant (Bearer Token `COMMERCANT`)

| Méthode | Route | Description |
| --- | --- | --- |
| GET | `/produits/mes-produits` | Liste de ses propres produits |
| POST | `/produits` | Créer un produit (commerçant déduit du token) |
| PUT / PATCH | `/produits/{id}` | Modifier son produit |
| PUT / PATCH | `/produits/{id}/disponibilite` | Activer / désactiver un produit |
| PUT / PATCH | `/produits/{id}/stock` | Mettre à jour le stock |
| DELETE | `/produits/{id}` | Supprimer son produit |
| POST | `/produits/images` | Uploader une image Cloudinary |
| DELETE | `/produits/images?publicId=...` | Supprimer une image Cloudinary |

### Réservations (Client, Bearer Token `CLIENT`)

Une réservation est stockée dans la table `commande` existante. Le client et le
commerçant sont toujours déduits du token JWT et de la relation
`commande.produit_id → produit.commercant_id` ; aucun identifiant
d'appartenance envoyé par le client n'est accepté.

| Méthode | Route | Description |
| --- | --- | --- |
| POST | `/reservations` | Créer une réservation (`produitId`, `quantite`) |
| GET | `/reservations/mes-reservations` | Lister uniquement ses propres réservations |
| GET | `/reservations/{id}` | Détail (client propriétaire ou commerçant du produit) |
| PUT / PATCH | `/reservations/{id}` | Modifier la quantité d'une réservation `EN_ATTENTE` |
| PUT / PATCH | `/reservations/{id}/annuler` | Annuler une réservation `EN_ATTENTE` |

Statuts : `EN_ATTENTE` → `CONFIRMEE`, `EN_ATTENTE` → `REFUSEE`,
`EN_ATTENTE` → `ANNULEE` (client propriétaire uniquement).

### Réservations (Commerçant, Bearer Token `COMMERCANT`)

| Méthode | Route | Description |
| --- | --- | --- |
| GET | `/reservations/recues` | Lister les réservations des produits du commerçant connecté |
| GET | `/reservations/{id}` | Détail d'une réservation de ses propres produits |
| PUT / PATCH | `/reservations/{id}/confirmer` | Confirmer une réservation `EN_ATTENTE` |
| PUT / PATCH | `/reservations/{id}/refuser` | Refuser une réservation `EN_ATTENTE` |

Le paiement reste effectué directement auprès du commerçant lors du retrait ;
aucun paiement en ligne n'est géré par l'API.

### Administrateur (Bearer Token `ADMIN`)

| Méthode | Route | Description |
| --- | --- | --- |
| GET | `/admin/produits` | Tous les produits avec filtres |
| GET | `/admin/produits/{id}` | Détail d'un produit |
| POST | `/admin/produits` | Créer un produit pour un commerçant (`commercantId` obligatoire) |
| PUT / PATCH | `/admin/produits/{id}` | Modifier / modérer un produit |
| PUT / PATCH | `/admin/produits/{id}/disponibilite` | Activer / désactiver un produit |
| PUT / PATCH | `/admin/produits/{id}/stock` | Corriger le stock |
| DELETE | `/admin/produits/{id}` | Supprimer un produit |
| GET | `/admin/produits/statistiques` | Indicateurs de gestion des produits |
| GET | `/admin/utilisateurs` | Supervision des comptes (`?role=CLIENT|COMMERCANT|ADMIN&statut=ACTIF|EN_ATTENTE|REJETE|DESACTIVE`) |
| PUT / PATCH | `/admin/utilisateurs/{id}/activer` | Passer le compte à `ACTIF` |
| PUT / PATCH | `/admin/utilisateurs/{id}/desactiver` | Passer le compte à `DESACTIVE` |
| GET | `/admin/reservations` | Toutes les réservations (`?statut=...&clientId=...&commercantId=...`) |
| GET | `/admin/reservations/{id}` | Détail complet d'une réservation |

Les réponses de `/admin/utilisateurs` utilisent un DTO sécurisé sans
`motDePasse`. Un administrateur ne peut pas désactiver son propre compte ni le
compte `ADMIN` principal (`admin@enicar.com`) : la réponse est `403` avec un
message explicite. La connexion des comptes `DESACTIVE` est refusée sur
`/auth/client/login`, `/auth/commercant/login` et `/auth/login`.

Les routes `/reservations/*` restent interdites à l'administrateur (`403`) ;
la supervision passe exclusivement par `/admin/reservations`.

## CORS et frontend React

Toutes les réponses HTTP (succès et erreurs `400/401/403/404/500`) contiennent :

```text
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Authorization
```

Les requêtes `OPTIONS` (preflight) sont interceptées avant toute vérification
JWT et retournent `204 No Content`. L'origine peut être restreinte avec la
variable d'environnement `CORS_ALLOWED_ORIGIN` (ou `-Dcors.allowed.origin=...`).

## Upload d'image Cloudinary

`POST /produits/images` avec un token `COMMERCANT` ou `ADMIN` :

```json
{
  "image": "data:image/png;base64,iVBORw0KGgo...",
  "folder": "onemorebite/produits"
}
```

Le champ `image` accepte une data URI `data:image/...;base64,...` ou une URL `http(s)` distante.
La réponse contient `imageUrl` et `publicId` ; l'`imageUrl` doit ensuite être envoyée dans le champ `imageUrl` du produit.

## Tests Postman

Importer `OneMoreBite.postman_collection.json`. La collection contient :

1. Authentification & Sessions (Admin, Commerçant, Client)
2. Workflow Commerçants (inscription & validation admin)
3. Produits (public)
4. Gestion Produits (Commerçant)
5. Gestion Produits (Admin)
6. Réservations (Client)
7. Réservations (Commerçant)
8. Réservations — Sécurité & transitions
9. Gestion des Avis
10. Administration, Commerçants publics & CORS

Les scripts de test enregistrent automatiquement les tokens et les identifiants de produits dans les variables de collection (`admin_token`, `commercant_token`, `client_token`, `produit_id`, `admin_produit_id`, `image_url`, `image_public_id`, `reservation_id`, `avis_id`, etc.).

## Migration base de données

Pour une base existante créée avant l'ajout des réservations commerçant, exécuter
`src/main/resources/database/migration_reservation_refusee.sql`. Cette migration
ajoute uniquement la valeur `REFUSEE` à l'enum `commande.statut`, nécessaire pour
distinguer un refus commerçant d'une annulation client. Le script complet
`src/main/resources/database/database.sql` contient déjà ce statut.

Pour une base existante créée avant le module de gestion des avis, exécuter
`src/main/resources/database/migration_avis_commercant.sql`. Cette migration
rattache les avis existants au commerçant (via `produit.commercant_id`) et
remplace `avis.produit_id` par `avis.commercant_id`.

Pour une base existante créée avant la supervision des utilisateurs, exécuter
`src/main/resources/database/migration_utilisateur_statut.sql`. Cette migration
ajoute `utilisateur.statut` (`ACTIF`, `EN_ATTENTE`, `REJETE`, `DESACTIVE`) et
aligne le statut des commerçants existants sur leur statut métier.
