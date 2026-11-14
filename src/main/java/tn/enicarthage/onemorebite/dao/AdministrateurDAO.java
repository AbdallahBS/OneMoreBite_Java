package tn.enicarthage.onemorebite.dao;

import tn.enicarthage.onemorebite.config.DatabaseConnection;
import tn.enicarthage.onemorebite.model.Administrateur;
import tn.enicarthage.onemorebite.security.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class AdministrateurDAO {

    public Optional<Administrateur> trouverParEmail(String email) {
        String sql = "SELECT u.id, u.nom, u.email, u.mot_de_passe, u.telephone " +
                     "FROM utilisateur u " +
                     "INNER JOIN administrateur a ON u.id = a.id " +
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

    public Optional<Administrateur> trouverParId(int id) {
        String sql = "SELECT u.id, u.nom, u.email, u.mot_de_passe, u.telephone " +
                     "FROM utilisateur u " +
                     "INNER JOIN administrateur a ON u.id = a.id " +
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
     * Initialise l'administrateur unique par défaut avec un mot de passe haché par BCrypt.
     */
    public void initialiserAdminParDefaut(String defaultEmail, String defaultPassword) {
        String motDePasseHache = PasswordUtil.hash(defaultPassword);

        try (Connection conn = DatabaseConnection.getConnection()) {
            // 1. Migration de l'ancien admin si présent
            String sqlMigrateOld = "UPDATE utilisateur SET email = ?, nom = ?, mot_de_passe = ? " +
                                  "WHERE email = 'admin@onemorebite.tn'";
            try (PreparedStatement stmt = conn.prepareStatement(sqlMigrateOld)) {
                stmt.setString(1, defaultEmail);
                stmt.setString(2, "Admin Enicar");
                stmt.setString(3, motDePasseHache);
                int migrated = stmt.executeUpdate();
                if (migrated > 0) {
                    return;
                }
            }

            // 2. Vérifier si l'admin existe déjà dans administrateur + utilisateur
            String sqlCheckAdmin = "SELECT u.id, u.mot_de_passe FROM utilisateur u " +
                                   "INNER JOIN administrateur a ON u.id = a.id " +
                                   "WHERE u.email = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlCheckAdmin)) {
                stmt.setString(1, defaultEmail);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int adminId = rs.getInt("id");
                        String currentPassword = rs.getString("mot_de_passe");
                        // Si le mot de passe ne correspond pas ou n'est pas encore hashé en BCrypt, on le met à jour
                        if (!PasswordUtil.verifier(defaultPassword, currentPassword) || !PasswordUtil.estHashBCrypt(currentPassword)) {
                            mettreAJourMotDePasse(adminId, motDePasseHache);
                        }
                        return;
                    }
                }
            }

            // 3. Vérifier si l'utilisateur existe déjà avec cet email
            String sqlCheckUser = "SELECT id FROM utilisateur WHERE email = ?";
            Integer userId = null;
            try (PreparedStatement stmt = conn.prepareStatement(sqlCheckUser)) {
                stmt.setString(1, defaultEmail);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        userId = rs.getInt("id");
                    }
                }
            }

            if (userId != null) {
                // Mettre à jour avec le mot de passe haché et lier à administrateur
                mettreAJourMotDePasse(userId, motDePasseHache);

                String sqlInsertAdmin = "INSERT IGNORE INTO administrateur (id) VALUES (?)";
                try (PreparedStatement stmt = conn.prepareStatement(sqlInsertAdmin)) {
                    stmt.setInt(1, userId);
                    stmt.executeUpdate();
                }
            } else {
                // 4. Insérer le nouvel utilisateur avec son mot de passe haché en BCrypt
                String sqlInsertUser = "INSERT INTO utilisateur (nom, email, mot_de_passe, telephone) VALUES (?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sqlInsertUser, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, "Admin Enicar");
                    stmt.setString(2, defaultEmail);
                    stmt.setString(3, motDePasseHache);
                    stmt.setString(4, "71000000");
                    stmt.executeUpdate();

                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            int newId = keys.getInt(1);
                            String sqlInsertAdmin = "INSERT INTO administrateur (id) VALUES (?)";
                            try (PreparedStatement adminStmt = conn.prepareStatement(sqlInsertAdmin)) {
                                adminStmt.setInt(1, newId);
                                adminStmt.executeUpdate();
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Met à jour les champs communs du profil administrateur (table utilisateur).
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
            throw new RuntimeException("Erreur lors de la mise à jour du profil administrateur.", e);
        }
    }

    /**
     * Supprime le compte administrateur. La ligne administrateur associée est
     * supprimée par la contrainte ON DELETE CASCADE.
     */
    public boolean supprimer(int id) {
        String sql = "DELETE FROM utilisateur WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du compte administrateur.", e);
        }
    }

    /**
     * Nombre d'administrateurs existants, utilisé pour garantir qu'il en reste
     * toujours au moins un sur la plateforme.
     */
    public int compter() {
        String sql = "SELECT COUNT(*) FROM administrateur";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du comptage des administrateurs.", e);
        }
    }

    private Administrateur mapResultSet(ResultSet rs) throws SQLException {
        return new Administrateur(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("email"),
                rs.getString("mot_de_passe"),
                rs.getString("telephone")
        );
    }
}
