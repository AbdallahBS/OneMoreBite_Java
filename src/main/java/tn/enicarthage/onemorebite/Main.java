package tn.enicarthage.onemorebite;

import java.io.IOException;
import java.sql.Connection;
import tn.enicarthage.onemorebite.config.DatabaseConnection;
import tn.enicarthage.onemorebite.controller.AuthController;

public class Main {

    private static final int PORT = resoudrePort();

    private static int resoudrePort() {
        String port = System.getenv("PORT");
        if (port == null || port.isBlank()) {
            port = System.getProperty("server.port");
        }
        if (port == null || port.isBlank()) {
            return 8081;
        }
        try {
            return Integer.parseInt(port.trim());
        } catch (NumberFormatException e) {
            System.out.println("Port invalide '" + port + "', utilisation du port 8081.");
            return 8081;
        }
    }

    public static void main(String[] args) {
        if (!testerConnexion()) {
            System.out.println("Vérifiez que MySQL est démarré et que database.sql est exécuté.");
            return;
        }

        // Initialisation de l'administrateur par défaut (admin@enicar.com / admin)
        try {
            new AuthController().initialiserDefaultAdmin();
            System.out.println("Administrateur par défaut vérifié et initialisé (admin@enicar.com).");
        } catch (Exception e) {
            System.out.println("Avertissement initialisation admin : " + e.getMessage());
        }

        try {
            ApiServer serveur = new ApiServer(PORT);
            serveur.demarrer();
            System.out.println("Serveur HTTP démarré sur http://localhost:" + PORT);
            System.out.println("Endpoints disponibles :");
            System.out.println("  POST http://localhost:" + PORT + "/auth/login                (Connexion Admin / Commerçant / Client)");
            System.out.println("  GET  http://localhost:" + PORT + "/auth/me                   (Session profil - Bearer Token)");
            System.out.println("  PUT  http://localhost:" + PORT + "/auth/me                   (Modifier son profil - Bearer Token)");
            System.out.println("  DELETE http://localhost:" + PORT + "/auth/me                 (Supprimer son compte - Bearer Token)");
            System.out.println("  POST http://localhost:" + PORT + "/auth/logout               (Déconnexion - Invalidation JWT)");
            System.out.println("  POST http://localhost:" + PORT + "/auth/client/register       (Inscription client)");
            System.out.println("  POST http://localhost:" + PORT + "/auth/client/login          (Connexion client)");
            System.out.println("  POST http://localhost:" + PORT + "/auth/commercant/register   (Inscription commerçant - EN_ATTENTE)");
            System.out.println("  POST http://localhost:" + PORT + "/auth/commercant/login      (Connexion commerçant)");
            System.out.println("  POST http://localhost:" + PORT + "/reservations              (Client: créer une réservation)");
            System.out.println("  GET  http://localhost:" + PORT + "/reservations/mes-reservations (Client: mes réservations)");
            System.out.println("  GET  http://localhost:" + PORT + "/reservations/recues       (Commerçant: réservations reçues)");
            System.out.println("  GET  http://localhost:" + PORT + "/reservations/:id          (Client propriétaire / Commerçant du produit)");
            System.out.println("  PUT  http://localhost:" + PORT + "/reservations/:id          (Client: modifier une réservation EN_ATTENTE)");
            System.out.println("  PUT  http://localhost:" + PORT + "/reservations/:id/annuler  (Client: annuler une réservation EN_ATTENTE)");
            System.out.println("  PUT  http://localhost:" + PORT + "/reservations/:id/confirmer (Commerçant: confirmer une réservation EN_ATTENTE)");
            System.out.println("  PUT  http://localhost:" + PORT + "/reservations/:id/refuser  (Commerçant: refuser une réservation EN_ATTENTE)");
            System.out.println("  GET  http://localhost:" + PORT + "/avis                      (Public: liste des avis ?commercantId=2)");
            System.out.println("  GET  http://localhost:" + PORT + "/commercants/:id/avis      (Public: avis d'un commerçant + note moyenne)");
            System.out.println("  POST http://localhost:" + PORT + "/avis                      (Client: créer un avis)");
            System.out.println("  PUT  http://localhost:" + PORT + "/avis/:id                  (Client propriétaire: modifier son avis)");
            System.out.println("  DELETE http://localhost:" + PORT + "/avis/:id                (Client propriétaire / Admin: supprimer un avis)");
            System.out.println("  GET  http://localhost:" + PORT + "/admin/commercants         (Admin: Liste des commerçants ?statut=EN_ATTENTE)");
            System.out.println("  PUT  http://localhost:" + PORT + "/admin/commercants/:id/accepter (Admin: Valider compte -> ACTIF)");
            System.out.println("  PUT  http://localhost:" + PORT + "/admin/commercants/:id/rejeter  (Admin: Rejeter compte -> REJETE)");
            System.out.println("  GET  http://localhost:" + PORT + "/admin/utilisateurs        (Admin: Supervision utilisateurs ?role=CLIENT&statut=ACTIF)");
            System.out.println("  PUT  http://localhost:" + PORT + "/admin/utilisateurs/:id/activer   (Admin: Activer un compte)");
            System.out.println("  PUT  http://localhost:" + PORT + "/admin/utilisateurs/:id/desactiver (Admin: Désactiver un compte)");
            System.out.println("  GET  http://localhost:" + PORT + "/admin/reservations       (Admin: Toutes les réservations + filtres)");
            System.out.println("  GET  http://localhost:" + PORT + "/admin/reservations/:id   (Admin: Détail d'une réservation)");
            System.out.println("  GET  http://localhost:" + PORT + "/commercants              (Public: commerçants ACTIF ?recherche=...)");
            System.out.println("  GET  http://localhost:" + PORT + "/produits                  (Liste publique des produits ?disponible=true&commercantId=2&recherche=...)");
            System.out.println("  GET  http://localhost:" + PORT + "/produits/:id              (Détail d'un produit)");
            System.out.println("  POST http://localhost:" + PORT + "/produits                  (Commerçant: créer un produit - Bearer Token)");
            System.out.println("  GET  http://localhost:" + PORT + "/produits/mes-produits     (Commerçant: mes produits - Bearer Token)");
            System.out.println("  PUT  http://localhost:" + PORT + "/produits/:id              (Commerçant propriétaire / Admin: modifier)");
            System.out.println("  PUT  http://localhost:" + PORT + "/produits/:id/disponibilite (Commerçant / Admin: activer/désactiver)");
            System.out.println("  PUT  http://localhost:" + PORT + "/produits/:id/stock        (Commerçant / Admin: mettre à jour le stock)");
            System.out.println("  DELETE http://localhost:" + PORT + "/produits/:id            (Commerçant propriétaire / Admin: supprimer)");
            System.out.println("  POST http://localhost:" + PORT + "/produits/images           (Upload Cloudinary - retourne imageUrl)");
            System.out.println("  DELETE http://localhost:" + PORT + "/produits/images?publicId=... (Suppression image Cloudinary)");
            System.out.println("  GET  http://localhost:" + PORT + "/admin/produits            (Admin: tous les produits + filtres)");
            System.out.println("  GET  http://localhost:" + PORT + "/admin/produits/statistiques (Admin: statistiques produits)");
            System.out.println("  POST http://localhost:" + PORT + "/admin/produits            (Admin: créer un produit pour un commerçant)");
        } catch (IOException e) {
            System.out.println("Impossible de démarrer le serveur : " + e.getMessage());
        }
    }

    private static boolean testerConnexion() {
        try (Connection connection = DatabaseConnection.getConnection()) {
            System.out.println("Connexion réussie à MySQL.");
            return true;
        } catch (Exception e) {
            System.out.println("Erreur de connexion : " + e.getMessage());
            return false;
        }
    }
}