# OneMoreBite

API Java 17 (HTTP server natif) pour la gestion d'une plateforme anti-gaspillage alimentaire.

## Fonctionnalités

- Authentification Admin, Commerçant & Client avec JWT et BCrypt
- Inscription, connexion, session et déconnexion des clients
- Cycle de vie des commerçants : inscription `EN_ATTENTE` → validation admin `ACTIF` / `REJETE`
- Gestion des produits par les commerçants : CRUD, stock, disponibilité, images Cloudinary
- Gestion et modération des produits par l'administrateur : filtres, CRUD, statistiques
- Upload / suppression d'images via l'API Cloudinary

## Prérequis

- Java 17
- Maven 3.9+
- MySQL 8+ sur `localhost:3306` (utilisateur `root`, mot de passe vide par défaut)

## Initialisation de la base

1. Démarrer MySQL.
2. Exécuter `src/main/resources/database/database.sql`, ou lancer la classe `DatabaseConnection` qui crée la base et charge le script.
3. Lancer `Main` (serveur sur `http://localhost:8081`).

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
| POST | `/auth/logout` | Déconnexion et révocation du token JWT |

### Public

| Méthode | Route | Description |
| --- | --- | --- |
| GET | `/produits` | Liste des produits (`?commercantId=2&disponible=true&recherche=panier`) |
| GET | `/produits/{id}` | Détail d'un produit |
| GET | `/commercants` | Liste des commerçants actifs |

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

Les scripts de test enregistrent automatiquement les tokens et les identifiants de produits dans les variables de collection (`admin_token`, `commercant_token`, `client_token`, `produit_id`, `admin_produit_id`, `image_url`, `image_public_id`).
