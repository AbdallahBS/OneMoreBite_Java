package tn.enicarthage.onemorebite;

import tn.enicarthage.onemorebite.config.DatabaseConnection;
import java.sql.Connection;
import tn.enicarthage.onemorebite.dao.ProduitDAO;
import tn.enicarthage.onemorebite.model.Produit;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        try {
            Connection connection = DatabaseConnection.getConnection();
             System.out.println("Connexion reussie a MySQL.");
             connection.close();
             } catch (Exception e) {
             System.out.println("Erreur de connexion : " + e.getMessage());
            }









        ProduitDAO produitDAO = new ProduitDAO();

        List<Produit> produitsInitiaux = produitDAO.findAll();
        for (Produit p : produitsInitiaux) {
            System.out.println(" - [" + p.getId() + "] " + p.getNom() + " | Prix: " + p.getPrix() + " DT | Image: " + p.getImageUrl());
        }

        Produit nouveauProduit = new Produit(
                0,
                "Panier Fruits & Légumes",
                "Panier varié de saison anti-gaspillage",
                new BigDecimal("8.500"),
                5,
                "https://example.com/images/fruits.jpg",
                new Date(),
                true,
                2
        );

        boolean succes = produitDAO.create(nouveauProduit);
        if (succes) {
            System.out.println("Produit créé : " + nouveauProduit.getId());
        }

        List<Produit> produitsApresAjout = produitDAO.findAll();
        for (Produit p : produitsApresAjout) {
            System.out.println(" - [" + p.getId() + "] " + p.getNom() + " | Prix: " + p.getPrix() + " DT");
        }
    }

}