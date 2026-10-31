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
