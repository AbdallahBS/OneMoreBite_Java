package tn.enicarthage.onemorebite;

import tn.enicarthage.onemorebite.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {

        try {
            Connection connection = DatabaseConnection.getConnection();
             System.out.println("Connexion reussie a MySQL.");
             connection.close();
             } catch (Exception e) {
             System.out.println("Erreur de connexion : " + e.getMessage());
            }
    }
}