package tn.enicarthage.onemorebite.dao;

import tn.enicarthage.onemorebite.config.DatabaseConnection;
import tn.enicarthage.onemorebite.model.Client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class ClientDAO {

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

    public Optional<Client> trouverParEmail(String email) {
        String sql = "SELECT u.id, u.nom, u.email, u.mot_de_passe, u.telephone " +
                     "FROM utilisateur u " +
                     "INNER JOIN client c ON u.id = c.id " +
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

    public Optional<Client> trouverParId(int id) {
        String sql = "SELECT u.id, u.nom, u.email, u.mot_de_passe, u.telephone " +
                     "FROM utilisateur u " +
                     "INNER JOIN client c ON u.id = c.id " +
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

    public Client creer(Client client) {
        String sqlUser = "INSERT INTO utilisateur (nom, email, mot_de_passe, telephone) VALUES (?, ?, ?, ?)";
        String sqlClient = "INSERT INTO client (id) VALUES (?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            int userId;
            try (PreparedStatement stmtUser = conn.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)) {
                stmtUser.setString(1, client.getNom());
                stmtUser.setString(2, client.getEmail());
                stmtUser.setString(3, client.getMotDePasse());
                stmtUser.setString(4, client.getTelephone());
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

            try (PreparedStatement stmtClient = conn.prepareStatement(sqlClient)) {
                stmtClient.setInt(1, userId);
                stmtClient.executeUpdate();
            }

            conn.commit();
            client.setId(userId);
            return client;
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
            throw new RuntimeException("Erreur lors de la création du client.", e);
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

    public boolean mettreAJourMotDePasse(int id, String motDePasseHache) {
        String sql = "UPDATE utilisateur SET mot_de_passe = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, motDePasseHache);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Client mapResultSet(ResultSet rs) throws SQLException {
        return new Client(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("email"),
                rs.getString("mot_de_passe"),
                rs.getString("telephone")
        );
    }
}
