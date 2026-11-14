package tn.enicarthage.onemorebite.dao;

import tn.enicarthage.onemorebite.config.DatabaseConnection;
import tn.enicarthage.onemorebite.dto.UtilisateurResponse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Accès transverse à la table utilisateur pour la supervision administrateur.
 * Le rôle est déduit des tables spécialisées (administrateur, commercant,
 * client) et le statut provient de utilisateur.statut.
 */
public class UtilisateurDAO {

    private static final String SELECT_UTILISATEUR =
            "SELECT u.id, u.nom, u.email, u.telephone, u.statut, " +
            "       CASE WHEN a.id IS NOT NULL THEN 'ADMIN' " +
            "            WHEN c.id IS NOT NULL THEN 'COMMERCANT' " +
            "            WHEN cl.id IS NOT NULL THEN 'CLIENT' END AS role, " +
            "       c.localisation, c.statut AS statut_commercant " +
            "FROM utilisateur u " +
            "LEFT JOIN administrateur a ON a.id = u.id " +
            "LEFT JOIN commercant c ON c.id = u.id " +
            "LEFT JOIN client cl ON cl.id = u.id ";

    public List<UtilisateurResponse> lister(String role, String statut) {
        List<UtilisateurResponse> utilisateurs = new ArrayList<>();
        StringBuilder sql = new StringBuilder(SELECT_UTILISATEUR).append("WHERE 1 = 1");
        List<Object> parametres = new ArrayList<>();

        if (role != null && !role.isBlank()) {
            switch (role.trim().toUpperCase()) {
                case "ADMIN" -> sql.append(" AND a.id IS NOT NULL");
                case "COMMERCANT" -> sql.append(" AND c.id IS NOT NULL");
                case "CLIENT" -> sql.append(" AND cl.id IS NOT NULL");
                default -> throw new IllegalArgumentException("Rôle invalide: " + role);
            }
        }

        if (statut != null && !statut.isBlank()) {
            sql.append(" AND u.statut = ?");
            parametres.add(StatutCompteUtilisateur.versBase(statut));
        }
        sql.append(" ORDER BY u.id ASC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < parametres.size(); i++) {
                stmt.setObject(i + 1, parametres.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    utilisateurs.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return utilisateurs;
    }

    public Optional<UtilisateurResponse> trouverParId(int id) {
        String sql = SELECT_UTILISATEUR + "WHERE u.id = ?";
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

    /**
     * Met à jour le statut du compte. L'activation d'un commerçant réactive
     * également son statut métier afin qu'il puisse se reconnecter.
     */
    public boolean mettreAJourStatut(int id, String nouveauStatut) {
        String statutBase = StatutCompteUtilisateur.versBase(nouveauStatut);
        String sqlUtilisateur = "UPDATE utilisateur SET statut = ? WHERE id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            boolean misAJour;
            try (PreparedStatement stmt = conn.prepareStatement(sqlUtilisateur)) {
                stmt.setString(1, statutBase);
                stmt.setInt(2, id);
                misAJour = stmt.executeUpdate() > 0;
            }

            if (!misAJour) {
                conn.rollback();
                return false;
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
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

        // Synchronisation best-effort du statut métier du commerçant. Une base
        // historique peut avoir un enum commercant.statut plus restrictif :
        // l'échec de cette mise à jour ne doit pas annuler la désactivation.
        synchroniserStatutCommercant(id, StatutCompteUtilisateur.versApi(statutBase));
        return true;
    }

    private void synchroniserStatutCommercant(int id, String statutApi) {
        if (!StatutCompteUtilisateur.ACTIF.equals(statutApi)
                && !StatutCompteUtilisateur.DESACTIVE.equals(statutApi)) {
            return;
        }
        String sql = "UPDATE commercant SET statut = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, statutApi);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            // Schéma historique sans valeur DESACTIVE : le statut du compte
            // (utilisateur.statut) reste la source de vérité pour la connexion.
        }
    }

    private UtilisateurResponse mapResultSet(ResultSet rs) throws SQLException {
        String role = rs.getString("role");
        String statut = StatutCompteUtilisateur.versApi(rs.getString("statut"));

        if ("COMMERCANT".equalsIgnoreCase(role)) {
            statut = StatutCompteUtilisateur.resoudreStatutCommercant(
                    statut, rs.getString("statut_commercant"));
        }

        return new UtilisateurResponse(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("email"),
                rs.getString("telephone"),
                role,
                statut,
                rs.getString("localisation")
        );
    }
}
