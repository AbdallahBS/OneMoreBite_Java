package tn.enicarthage.onemorebite.dao;

import tn.enicarthage.onemorebite.config.DatabaseConnection;
import tn.enicarthage.onemorebite.dto.ProduitStatistiques;
import tn.enicarthage.onemorebite.model.Produit;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProduitDAO {

    public List<Produit> listerTous() {
        return listerAvecFiltres(null, null, null);
    }

    /**
     * Liste les produits avec filtres optionnels : commerçant, disponibilité et recherche textuelle.
     */
    public List<Produit> listerAvecFiltres(Integer commercantId, Boolean disponibilite, String recherche) {
        List<Produit> produits = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM produit WHERE 1 = 1");
        List<Object> parametres = new ArrayList<>();

        if (commercantId != null) {
            sql.append(" AND commercant_id = ?");
            parametres.add(commercantId);
        }
        if (disponibilite != null) {
            sql.append(" AND disponibilite = ?");
            parametres.add(disponibilite);
        }
        if (recherche != null && !recherche.isBlank()) {
            sql.append(" AND (nom LIKE ? OR description LIKE ?)");
            String motif = "%" + recherche.trim() + "%";
            parametres.add(motif);
            parametres.add(motif);
        }
        sql.append(" ORDER BY id DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < parametres.size(); i++) {
                stmt.setObject(i + 1, parametres.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    produits.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return produits;
    }

    public List<Produit> listerParCommercant(int commercantId) {
        return listerAvecFiltres(commercantId, null, null);
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
            return p;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la création du produit : " + e.getMessage(), e);
        }
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

    public boolean modifierDisponibilite(int id, boolean disponibilite) {
        String sql = "UPDATE produit SET disponibilite = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBoolean(1, disponibilite);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean modifierStock(int id, int quantite) {
        String sql = "UPDATE produit SET quantite = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, quantite);
            stmt.setInt(2, id);
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

    public ProduitStatistiques statistiques() {
        String sql = "SELECT COUNT(*) AS total, " +
                "COALESCE(SUM(CASE WHEN disponibilite = TRUE THEN 1 ELSE 0 END), 0) AS disponibles, " +
                "COALESCE(SUM(CASE WHEN disponibilite = FALSE THEN 1 ELSE 0 END), 0) AS indisponibles, " +
                "COALESCE(SUM(CASE WHEN quantite = 0 THEN 1 ELSE 0 END), 0) AS rupture, " +
                "COALESCE(SUM(quantite), 0) AS quantite_totale, " +
                "COALESCE(SUM(prix * quantite), 0) AS valeur_stock " +
                "FROM produit";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return new ProduitStatistiques(
                        rs.getLong("total"),
                        rs.getLong("disponibles"),
                        rs.getLong("indisponibles"),
                        rs.getLong("rupture"),
                        rs.getLong("quantite_totale"),
                        rs.getDouble("valeur_stock")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ProduitStatistiques(0, 0, 0, 0, 0, 0);
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
