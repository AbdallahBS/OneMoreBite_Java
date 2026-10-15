package tn.enicarthage.onemorebite;

import java.io.IOException;
import java.sql.Connection;
import tn.enicarthage.onemorebite.config.DatabaseConnection;

public class Main {

    public static void main(String[] args) {
        if (!testerConnexion()) {
            System.out.println("Vérifiez que MySQL est démarré et que database.sql est exécuté.");
            return;
        }

        try {
            ApiServer serveur = new ApiServer(8080);
            serveur.demarrer();
            System.out.println("Serveur HTTP démarré sur http://localhost:8080");
            System.out.println("Testez avec Postman : GET http://localhost:8080/produits");
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