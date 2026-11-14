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
        String sql = "SELECT u.id, u.nom, u.email, u.mot_de_passe, u.telephone, u.statut " +
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
        String sql = "SELECT u.id, u.nom, u.email, u.mot_de_passe, u.telephone, u.statut " +
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
        String sqlUser = "INSERT INTO utilisateur (nom, email, mot_de_passe, telephone, statut) " +
                         "VALUES (?, ?, ?, ?, ?)";
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
                // Un client est actif dès son inscription. On ne dépend pas du
                // DEFAULT de la colonne : certaines bases historiques utilisent
                // INACTIF par défaut pour le workflow commerçant.
                stmtUser.setString(5, StatutCompteUtilisateur.ACTIF);
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

    /**
     * Met à jour les champs communs du profil (table utilisateur).
     * Les valeurs transmises sont déjà validées et normalisées par le service.
     */
    public boolean mettreAJourProfil(int id, String nom, String email, String telephone, String motDePasseHache) {
        String sql = "UPDATE utilisateur SET nom = ?, email = ?, telephone = ?, mot_de_passe = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nom);
            stmt.setString(2, email);
            stmt.setString(3, telephone);
            stmt.setString(4, motDePasseHache);
            stmt.setInt(5, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
                throw new IllegalArgumentException("Un compte avec cet email existe déjà.");
            }
            throw new RuntimeException("Erreur lors de la mise à jour du profil client.", e);
        }
    }

    /**
     * Supprime le compte et ses réservations. Le client, ses avis et ses
     * commandes sont nettoyés dans une transaction : suppression explicite des
     * réservations (la contrainte du schéma est ON DELETE SET NULL) puis de la
     * ligne utilisateur (cascade sur client et avis).
     */
    public boolean supprimer(int id) {
        String sqlReservations = "DELETE FROM commande WHERE client_id = ?";
        String sqlUtilisateur = "DELETE FROM utilisateur WHERE id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmtReservations = conn.prepareStatement(sqlReservations)) {
                stmtReservations.setInt(1, id);
                stmtReservations.executeUpdate();
            }

            boolean supprime;
            try (PreparedStatement stmtUtilisateur = conn.prepareStatement(sqlUtilisateur)) {
                stmtUtilisateur.setInt(1, id);
                supprime = stmtUtilisateur.executeUpdate() > 0;
            }

            conn.commit();
            return supprime;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw new RuntimeException("Erreur lors de la suppression du compte client.", e);
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

    private Client mapResultSet(ResultSet rs) throws SQLException {
        Client client = new Client(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("email"),
                rs.getString("mot_de_passe"),
                rs.getString("telephone")
        );
        client.setStatutCompte(StatutCompteUtilisateur.versApi(rs.getString("statut")));
        return client;
    }
}
