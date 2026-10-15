package tn.enicarthage.onemorebite.dao;

import tn.enicarthage.onemorebite.config.DatabaseConnection;
import tn.enicarthage.onemorebite.model.Produit;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProduitDAO {


    public boolean create(Produit produit) {
        String sql = "INSERT INTO produit (nom, description, prix, quantite, image_url, date_expiration, disponibilite, commercant_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, produit.getNom());
            stmt.setString(2, produit.getDescription());
            stmt.setBigDecimal(3, produit.getPrix());
            stmt.setInt(4, produit.getQuantite());
            stmt.setString(5, produit.getImageUrl());
            stmt.setDate(6, produit.getDateExpiration() != null ? new java.sql.Date(produit.getDateExpiration().getTime()) : null);
            stmt.setBoolean(7, produit.isDisponibilite());
            stmt.setInt(8, produit.getCommercantId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        produit.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Erreur dans ProduitDAO.create : " + e.getMessage());
        }
        return false;
    }

    public List<Produit> findAll() {
        List<Produit> produits = new ArrayList<>();
        String sql = "SELECT * FROM produit";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                produits.add(mapResultSetToProduit(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur dans ProduitDAO.findAll : " + e.getMessage());
        }
        return produits;
    }

    public Produit findById(int id) {
        String sql = "SELECT * FROM produit WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProduit(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println(" Erreur dans ProduitDAO.findById : " + e.getMessage());
        }
        return null;
    }


    private Produit mapResultSetToProduit(ResultSet rs) throws SQLException {
        Produit p = new Produit();
        p.setId(rs.getInt("id"));
        p.setNom(rs.getString("nom"));
        p.setDescription(rs.getString("description"));
        p.setPrix(rs.getBigDecimal("prix"));
        p.setQuantite(rs.getInt("quantite"));
        p.setImageUrl(rs.getString("image_url"));
        p.setDateExpiration(rs.getDate("date_expiration"));
        p.setDisponibilite(rs.getBoolean("disponibilite"));
        p.setCommercantId(rs.getInt("commercant_id"));
        return p;
    }
}