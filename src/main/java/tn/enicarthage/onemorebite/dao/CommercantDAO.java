package tn.enicarthage.onemorebite.dao;

import tn.enicarthage.onemorebite.config.DatabaseConnection;
import tn.enicarthage.onemorebite.model.Commercant;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CommercantDAO {


    public boolean emailExiste(String email) {
        String sql = "SELECT id FROM utilisateur WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Optional<Commercant> trouverParEmail(String email) {
        String sql = "SELECT u.id, u.nom, u.email, u.mot_de_passe, u.telephone, c.localisation, c.statut " +
                     "FROM utilisateur u " +
                     "INNER JOIN commercant c ON u.id = c.id " +
                     "WHERE u.email = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
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

    public Optional<Commercant> trouverParId(int id) {
        String sql = "SELECT u.id, u.nom, u.email, u.mot_de_passe, u.telephone, c.localisation, c.statut " +
                     "FROM utilisateur u " +
                     "INNER JOIN commercant c ON u.id = c.id " +
                     "WHERE u.id = ?";

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

    public Commercant creer(Commercant c) {
        String sqlUser = "INSERT INTO utilisateur (nom, email, mot_de_passe, telephone) VALUES (?, ?, ?, ?)";
        String sqlCommercant = "INSERT INTO commercant (id, localisation, statut) VALUES (?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            int userId;
            try (PreparedStatement stmtUser = conn.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)) {
                stmtUser.setString(1, c.getNom());
                stmtUser.setString(2, c.getEmail());
                stmtUser.setString(3, c.getMotDePasse());
                stmtUser.setString(4, c.getTelephone());
                stmtUser.executeUpdate();

                try (ResultSet keys = stmtUser.getGeneratedKeys()) {
                    if (keys.next()) {
                        userId = keys.getInt(1);
                    } else {
                        conn.rollback();
                        throw new SQLException("Échec de récupération de l'ID utilisateur généré.");
                    }
                }
            }

            try (PreparedStatement stmtComm = conn.prepareStatement(sqlCommercant)) {
                stmtComm.setInt(1, userId);
                stmtComm.setString(2, c.getLocalisation());
                stmtComm.setString(3, c.getStatut() != null ? c.getStatut() : "EN_ATTENTE");
                stmtComm.executeUpdate();
            }

            conn.commit();
            c.setId(userId);
            return c;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw new RuntimeException("Erreur lors de la création du commerçant : " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public List<Commercant> listerTous() {
        List<Commercant> commercants = new ArrayList<>();
        String sql = "SELECT u.id, u.nom, u.email, u.mot_de_passe, u.telephone, c.localisation, c.statut " +
                     "FROM utilisateur u " +
                     "INNER JOIN commercant c ON u.id = c.id " +
                     "ORDER BY u.id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                commercants.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return commercants;
    }

    public List<Commercant> listerParStatut(String statut) {
        List<Commercant> commercants = new ArrayList<>();
        String sql = "SELECT u.id, u.nom, u.email, u.mot_de_passe, u.telephone, c.localisation, c.statut " +
                     "FROM utilisateur u " +
                     "INNER JOIN commercant c ON u.id = c.id " +
                     "WHERE c.statut = ? " +
                     "ORDER BY u.id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, statut);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    commercants.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return commercants;
    }

    public boolean mettreAJourStatut(int id, String nouveauStatut) {
        String sql = "UPDATE commercant SET statut = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nouveauStatut);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Met à jour le profil du commerçant dans une transaction : champs communs
     * (table utilisateur) et localisation (table commercant).
     */
    public boolean mettreAJourProfil(int id, String nom, String email, String telephone,
                                     String motDePasseHache, String localisation) {
        String sqlUser = "UPDATE utilisateur SET nom = ?, email = ?, telephone = ?, mot_de_passe = ? WHERE id = ?";
        String sqlCommercant = "UPDATE commercant SET localisation = ? WHERE id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            int updated;
            try (PreparedStatement stmtUser = conn.prepareStatement(sqlUser)) {
                stmtUser.setString(1, nom);
                stmtUser.setString(2, email);
                stmtUser.setString(3, telephone);
                stmtUser.setString(4, motDePasseHache);
                stmtUser.setInt(5, id);
                updated = stmtUser.executeUpdate();
            }

            if (updated == 0) {
                conn.rollback();
                return false;
            }

            try (PreparedStatement stmtCommercant = conn.prepareStatement(sqlCommercant)) {
                stmtCommercant.setString(1, localisation);
                stmtCommercant.setInt(2, id);
                stmtCommercant.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
                throw new IllegalArgumentException("Un compte avec cet email existe déjà.");
            }
            throw new RuntimeException("Erreur lors de la mise à jour du profil commerçant.", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Supprime le compte. Les produits, réservations et avis liés sont supprimés
     * par les contraintes ON DELETE CASCADE du schéma.
     */
    public boolean supprimer(int id) {
        String sql = "DELETE FROM utilisateur WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du compte commerçant.", e);
        }
    }

    private Commercant mapResultSet(ResultSet rs) throws SQLException {
        return new Commercant(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("email"),
                rs.getString("mot_de_passe"),
                rs.getString("telephone"),
                rs.getString("localisation"),
                rs.getString("statut")
        );
    }
}
