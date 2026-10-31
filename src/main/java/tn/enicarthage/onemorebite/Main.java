package tn.enicarthage.onemorebite;

import java.io.IOException;
import java.sql.Connection;
import tn.enicarthage.onemorebite.config.DatabaseConnection;
import tn.enicarthage.onemorebite.controller.AuthController;

public class Main {

    private static final int PORT = 8081;

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
            System.out.println("  POST http://localhost:" + PORT + "/auth/login   (Connexion admin)");
            System.out.println("  GET  http://localhost:" + PORT + "/auth/me      (Session admin - Bearer Token)");
            System.out.println("  POST http://localhost:" + PORT + "/auth/logout  (Déconnexion admin - Invalidation JWT)");
            System.out.println("  GET  http://localhost:" + PORT + "/produits     (Liste des produits)");
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