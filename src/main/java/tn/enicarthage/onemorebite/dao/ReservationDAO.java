package tn.enicarthage.onemorebite.dao;

import tn.enicarthage.onemorebite.config.DatabaseConnection;
import tn.enicarthage.onemorebite.dto.ReservationResponse;
import tn.enicarthage.onemorebite.exception.ResourceNotFoundException;
import tn.enicarthage.onemorebite.model.Commande;
import tn.enicarthage.onemorebite.model.StatutCommande;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Accès aux réservations. Une réservation est stockée dans la table commande
 * existante ; le commerçant est retrouvé via produit.commercant_id.
 */
public class ReservationDAO {

    private static final String SELECT_RESERVATION =
            "SELECT cmd.id, cmd.date_commande, cmd.quantite, cmd.statut, cmd.produit_id, " +
            "       cmd.client_id, cmd.nom_invite, cmd.email_invite, cmd.telephone_invite, " +
            "       prod.nom AS produit_nom, prod.description AS produit_description, " +
            "       prod.prix AS produit_prix, prod.image_url AS produit_image_url, " +
            "       prod.date_expiration AS produit_date_expiration, " +
            "       prod.quantite AS produit_quantite, prod.disponibilite AS produit_disponibilite, " +
            "       prod.commercant_id AS commercant_id, " +
            "       comm_u.nom AS commercant_nom, comm.localisation AS commercant_localisation, " +
            "       cli_u.nom AS client_nom, cli_u.email AS client_email, cli_u.telephone AS client_telephone " +
            "FROM commande cmd " +
            "INNER JOIN produit prod ON prod.id = cmd.produit_id " +
            "INNER JOIN commercant comm ON comm.id = prod.commercant_id " +
            "INNER JOIN utilisateur comm_u ON comm_u.id = comm.id " +
            "LEFT JOIN client cli ON cli.id = cmd.client_id " +
            "LEFT JOIN utilisateur cli_u ON cli_u.id = cli.id ";

    public Optional<ReservationResponse> trouverParId(int id) {
        String sql = SELECT_RESERVATION + "WHERE cmd.id = ?";
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

    public List<ReservationResponse> listerParClient(int clientId) {
        return lister(SELECT_RESERVATION + "WHERE cmd.client_id = ? ORDER BY cmd.id DESC", clientId);
    }

    public List<ReservationResponse> listerParCommercant(int commercantId) {
        return lister(SELECT_RESERVATION + "WHERE prod.commercant_id = ? ORDER BY cmd.id DESC", commercantId);
    }

    private List<ReservationResponse> lister(String sql, int parametre) {
        List<ReservationResponse> reservations = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, parametre);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reservations;
    }

    /**
     * Crée une réservation dans une transaction. Le produit est verrouillé
     * (SELECT ... FOR UPDATE) afin de valider sa disponibilité et son stock de
     * manière cohérente avec les autres opérations.
     */
    public ReservationResponse creer(Commande commande) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            verifierProduitDisponible(conn, commande.getProduitId(), commande.getQuantite());

            String sql = "INSERT INTO commande (quantite, statut, produit_id, client_id) VALUES (?, ?, ?, ?)";
            int id;
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, commande.getQuantite());
                stmt.setString(2, StatutCommande.EN_ATTENTE.name());
                stmt.setInt(3, commande.getProduitId());
                stmt.setInt(4, commande.getClientId());
                stmt.executeUpdate();

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Échec de récupération de l'ID de la réservation générée.");
                    }
                    id = keys.getInt(1);
                }
            }

            conn.commit();
            return trouverParId(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Réservation non trouvée avec l'id : " + id));
        } catch (SQLException e) {
            rollback(conn);
            throw new RuntimeException("Erreur lors de la création de la réservation.", e);
        } catch (RuntimeException e) {
            rollback(conn);
            throw e;
        } finally {
            fermer(conn);
        }
    }

    /**
     * Met à jour la quantité uniquement si la réservation appartient au client
     * et est encore EN_ATTENTE. Le UPDATE conditionnel évite les doubles mises
     * à jour concurrentes.
     */
    public boolean modifierQuantite(int id, int clientId, int quantite) {
        String sql = "UPDATE commande SET quantite = ? " +
                     "WHERE id = ? AND client_id = ? AND statut = 'EN_ATTENTE'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, quantite);
            stmt.setInt(2, id);
            stmt.setInt(3, clientId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Annule une réservation uniquement si elle appartient au client et est
     * encore EN_ATTENTE.
     */
    public boolean annuler(int id, int clientId) {
        String sql = "UPDATE commande SET statut = 'ANNULEE' " +
                     "WHERE id = ? AND client_id = ? AND statut = 'EN_ATTENTE'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.setInt(2, clientId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Change le statut d'une réservation uniquement si le produit associé
     * appartient au commerçant et si la réservation est encore EN_ATTENTE.
     */
    public boolean changerStatutParCommercant(int id, int commercantId, StatutCommande nouveauStatut) {
        String sql = "UPDATE commande SET statut = ? " +
                     "WHERE id = ? AND statut = 'EN_ATTENTE' " +
                     "AND produit_id IN (SELECT id FROM produit WHERE commercant_id = ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nouveauStatut.name());
            stmt.setInt(2, id);
            stmt.setInt(3, commercantId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void verifierProduitDisponible(Connection conn, int produitId, int quantite) throws SQLException {
        String sql = "SELECT quantite, disponibilite FROM produit WHERE id = ? FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, produitId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new ResourceNotFoundException("Produit non trouvé avec id: " + produitId);
                }
                if (!rs.getBoolean("disponibilite")) {
                    throw new IllegalArgumentException("Ce produit n'est pas disponible à la réservation.");
                }
                int stock = rs.getInt("quantite");
                if (stock < quantite) {
                    throw new IllegalArgumentException(
                            "Stock insuffisant : " + stock + " unité(s) disponible(s).");
                }
            }
        }
    }

    private ReservationResponse mapResultSet(ResultSet rs) throws SQLException {
        Timestamp dateCommande = rs.getTimestamp("date_commande");
        Date dateExpiration = rs.getDate("produit_date_expiration");
        int clientId = rs.getInt("client_id");
        Integer clientIdNullable = rs.wasNull() ? null : clientId;

        return new ReservationResponse(
                rs.getInt("id"),
                dateCommande != null ? dateCommande.toLocalDateTime().toString() : null,
                rs.getInt("quantite"),
                rs.getString("statut"),
                rs.getInt("produit_id"),
                rs.getString("produit_nom"),
                rs.getString("produit_description"),
                rs.getDouble("produit_prix"),
                rs.getString("produit_image_url"),
                dateExpiration != null ? dateExpiration.toLocalDate() : null,
                rs.getInt("produit_quantite"),
                rs.getBoolean("produit_disponibilite"),
                rs.getInt("commercant_id"),
                rs.getString("commercant_nom"),
                rs.getString("commercant_localisation"),
                clientIdNullable,
                rs.getString("client_nom"),
                rs.getString("client_email"),
                rs.getString("client_telephone"),
                rs.getString("nom_invite"),
                rs.getString("email_invite"),
                rs.getString("telephone_invite")
        );
    }

    private void rollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void fermer(Connection conn) {
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
