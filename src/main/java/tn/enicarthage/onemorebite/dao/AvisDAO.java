package tn.enicarthage.onemorebite.dao;

import tn.enicarthage.onemorebite.config.DatabaseConnection;
import tn.enicarthage.onemorebite.dto.AvisResponse;
import tn.enicarthage.onemorebite.exception.ResourceNotFoundException;
import tn.enicarthage.onemorebite.model.Avis;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Accès aux avis. Les noms du client et du commerçant sont récupérés par
 * jointure SQL pour alimenter directement les réponses du frontend.
 */
public class AvisDAO {

    private static final String SELECT_AVIS =
            "SELECT av.id, av.note, av.commentaire, av.date_avis, " +
            "       av.client_id, av.commercant_id, " +
            "       cli_u.nom AS client_nom, comm_u.nom AS commercant_nom " +
            "FROM avis av " +
            "INNER JOIN client cli ON cli.id = av.client_id " +
            "INNER JOIN utilisateur cli_u ON cli_u.id = cli.id " +
            "INNER JOIN commercant comm ON comm.id = av.commercant_id " +
            "INNER JOIN utilisateur comm_u ON comm_u.id = comm.id ";

    public Optional<AvisResponse> trouverParId(int id) {
        String sql = SELECT_AVIS + "WHERE av.id = ?";
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

    public List<AvisResponse> listerTous() {
        return lister(SELECT_AVIS + "ORDER BY av.date_avis DESC, av.id DESC", null);
    }

    public List<AvisResponse> listerParCommercant(int commercantId) {
        return lister(SELECT_AVIS + "WHERE av.commercant_id = ? ORDER BY av.date_avis DESC, av.id DESC", commercantId);
    }

    private List<AvisResponse> lister(String sql, Integer parametre) {
        List<AvisResponse> avis = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (parametre != null) {
                stmt.setInt(1, parametre);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    avis.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return avis;
    }

    public AvisResponse creer(Avis avis) {
        String sql = "INSERT INTO avis (note, commentaire, date_avis, client_id, commercant_id) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, avis.getNote());
            stmt.setString(2, avis.getCommentaire());
            LocalDateTime dateAvis = avis.getDateAvis() != null ? avis.getDateAvis() : LocalDateTime.now();
            stmt.setTimestamp(3, Timestamp.valueOf(dateAvis));
            stmt.setInt(4, avis.getClientId());
            stmt.setInt(5, avis.getCommercantId());

            stmt.executeUpdate();

            int id;
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Échec de récupération de l'ID de l'avis généré.");
                }
                id = keys.getInt(1);
            }

            return trouverParId(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Avis non trouvé avec l'id : " + id));
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la création de l'avis.", e);
        }
    }

    /**
     * Met à jour un avis uniquement s'il appartient au client donné.
     * Le UPDATE conditionnel évite qu'un client modifie l'avis d'un autre
     * en cas d'appel concurrent.
     */
    public boolean modifier(int id, int clientId, int note, String commentaire) {
        String sql = "UPDATE avis SET note = ?, commentaire = ? WHERE id = ? AND client_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, note);
            stmt.setString(2, commentaire);
            stmt.setInt(3, id);
            stmt.setInt(4, clientId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean supprimer(int id) {
        String sql = "DELETE FROM avis WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private AvisResponse mapResultSet(ResultSet rs) throws SQLException {
        Timestamp timestamp = rs.getTimestamp("date_avis");
        LocalDateTime dateAvis = timestamp != null ? timestamp.toLocalDateTime() : null;

        return new AvisResponse(
                rs.getInt("id"),
                rs.getInt("note"),
                rs.getString("commentaire"),
                dateAvis,
                rs.getInt("client_id"),
                rs.getString("client_nom"),
                rs.getInt("commercant_id"),
                rs.getString("commercant_nom")
        );
    }
}
