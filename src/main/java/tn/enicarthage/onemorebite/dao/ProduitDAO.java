package tn.enicarthage.onemorebite.dao;

import tn.enicarthage.onemorebite.config.DatabaseConnection;
import tn.enicarthage.onemorebite.model.Produit;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProduitDAO {

    public List<Produit> listerTous() {
        List<Produit> produits = new ArrayList<>();
        String sql = "SELECT * FROM produit ORDER BY id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                produits.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return produits;
    }

    public Optional<Produit> trouverParId(int id) {
        String sql = "SELECT * FROM produit WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Produit creer(Produit p) {
        String sql = "INSERT INTO produit (nom, description, prix, quantite, image_url, date_expiration, disponibilite, commercant_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, p.getNom());
            stmt.setString(2, p.getDescription());
            stmt.setDouble(3, p.getPrix());
            stmt.setInt(4, p.getQuantite());
            stmt.setString(5, p.getImageUrl());
            stmt.setDate(6, p.getDateExpiration() != null ? Date.valueOf(p.getDateExpiration()) : null);
            stmt.setBoolean(7, p.isDisponibilite());
            stmt.setInt(8, p.getCommercantId());

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    p.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return p;
    }

    public boolean modifier(Produit p) {
        String sql = "UPDATE produit SET nom = ?, description = ?, prix = ?, quantite = ?, " +
                "image_url = ?, date_expiration = ?, disponibilite = ?, commercant_id = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, p.getNom());
            stmt.setString(2, p.getDescription());
            stmt.setDouble(3, p.getPrix());
            stmt.setInt(4, p.getQuantite());
            stmt.setString(5, p.getImageUrl());
            stmt.setDate(6, p.getDateExpiration() != null ? Date.valueOf(p.getDateExpiration()) : null);
            stmt.setBoolean(7, p.isDisponibilite());
            stmt.setInt(8, p.getCommercantId());
            stmt.setInt(9, p.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean supprimer(int id) {
        String sql = "DELETE FROM produit WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Produit mapResultSet(ResultSet rs) throws SQLException {
        Date expDate = rs.getDate("date_expiration");
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");

        return new Produit(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("description"),
                rs.getDouble("prix"),
                rs.getInt("quantite"),
                rs.getString("image_url"),
                expDate != null ? expDate.toLocalDate() : null,
                rs.getBoolean("disponibilite"),
                rs.getInt("commercant_id"),
                createdAt != null ? createdAt.toLocalDateTime() : null,
                updatedAt != null ? updatedAt.toLocalDateTime() : null
        );
    }
}