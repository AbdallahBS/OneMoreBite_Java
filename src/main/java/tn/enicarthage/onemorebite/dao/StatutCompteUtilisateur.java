package tn.enicarthage.onemorebite.dao;

import tn.enicarthage.onemorebite.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Traduction entre les statuts de compte exposés par l'API (ACTIF, EN_ATTENTE,
 * REJETE, DESACTIVE) et les valeurs stockées dans utilisateur.statut.
 *
 * Certaines bases créées avant la supervision administrateur utilisent encore
 * l'ancien enum (INACTIF, ACTIF, REFUSE). Cette classe détecte le schéma réel
 * et effectue la conversion afin de ne pas casser une base existante.
 */
public final class StatutCompteUtilisateur {

    public static final String ACTIF = "ACTIF";
    public static final String EN_ATTENTE = "EN_ATTENTE";
    public static final String REJETE = "REJETE";
    public static final String DESACTIVE = "DESACTIVE";

    private static final String INACTIF = "INACTIF";
    private static final String REFUSE = "REFUSE";

    private static final Set<String> VALEURS_PAR_DEFAUT =
            Set.of(ACTIF, EN_ATTENTE, REJETE, DESACTIVE);

    private static final Set<String> TOUTES_LES_VALEURS =
            Set.of(ACTIF, EN_ATTENTE, REJETE, DESACTIVE, INACTIF, REFUSE);

    private static final Pattern VALEUR_ENUM = Pattern.compile("'((?:[^']|'')*)'");

    private static volatile Set<String> valeursDisponibles;

    private StatutCompteUtilisateur() {
    }

    /**
     * Convertit un statut API vers une valeur acceptée par la colonne
     * utilisateur.statut de la base courante.
     */
    public static String versBase(String statut) {
        if (statut == null) {
            return null;
        }
        String valeur = normaliser(statut);
        Set<String> disponibles = valeursDisponibles();

        return switch (valeur) {
            case ACTIF -> ACTIF;
            case EN_ATTENTE -> disponibles.contains(EN_ATTENTE) ? EN_ATTENTE : INACTIF;
            case REJETE, REFUSE -> disponibles.contains(REJETE) ? REJETE : REFUSE;
            case DESACTIVE, INACTIF -> disponibles.contains(DESACTIVE) ? DESACTIVE : INACTIF;
            default -> throw new IllegalArgumentException("Statut de compte invalide: " + statut);
        };
    }

    /**
     * Convertit une valeur stockée en base vers le statut API.
     */
    public static String versApi(String statut) {
        if (statut == null) {
            return null;
        }
        return switch (normaliser(statut)) {
            case INACTIF -> DESACTIVE;
            case REFUSE -> REJETE;
            default -> normaliser(statut);
        };
    }

    /**
     * Un compte est considéré désactivé s'il porte le nouveau statut
     * DESACTIVE ou l'ancien statut INACTIF.
     */
    public static boolean estDesactive(String statut) {
        if (statut == null || statut.isBlank()) {
            return false;
        }
        String valeur = normaliser(statut);
        return DESACTIVE.equals(valeur) || INACTIF.equals(valeur);
    }

    /**
     * Pour un commerçant, le statut métier (commercant.statut) fait foi tant que
     * le compte n'a pas été explicitement désactivé. Cela permet de gérer les
     * bases historiques où utilisateur.statut valait INACTIF avant même la
     * validation du compte.
     */
    public static String resoudreStatutCommercant(String statutCompte, String statutMetier) {
        String compte = versApi(statutCompte);
        String metier = versApi(statutMetier);

        if (metier == null || metier.isBlank()) {
            return compte;
        }
        if (DESACTIVE.equals(metier)) {
            return DESACTIVE;
        }
        if (DESACTIVE.equals(compte)) {
            return metier;
        }
        return metier;
    }

    private static String normaliser(String statut) {
        return statut.trim().toUpperCase(Locale.ROOT);
    }

    private static Set<String> valeursDisponibles() {
        Set<String> cache = valeursDisponibles;
        if (cache != null) {
            return cache;
        }
        synchronized (StatutCompteUtilisateur.class) {
            if (valeursDisponibles == null) {
                valeursDisponibles = detecterValeurs();
            }
            return valeursDisponibles;
        }
    }

    private static Set<String> detecterValeurs() {
        String sql = "SELECT COLUMN_TYPE FROM information_schema.COLUMNS " +
                     "WHERE TABLE_SCHEMA = DATABASE() " +
                     "AND TABLE_NAME = 'utilisateur' AND COLUMN_NAME = 'statut'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                String type = rs.getString(1);
                if (type == null || type.isBlank()) {
                    return VALEURS_PAR_DEFAUT;
                }
                if (!type.toLowerCase(Locale.ROOT).startsWith("enum(")) {
                    // Colonne libre (VARCHAR) : toutes les valeurs sont acceptées.
                    return TOUTES_LES_VALEURS;
                }
                Set<String> valeurs = new LinkedHashSet<>();
                Matcher matcher = VALEUR_ENUM.matcher(type);
                while (matcher.find()) {
                    valeurs.add(normaliser(matcher.group(1).replace("''", "'")));
                }
                if (!valeurs.isEmpty()) {
                    return Collections.unmodifiableSet(valeurs);
                }
            }
        } catch (SQLException e) {
            // Table/colonne absente ou droits insuffisants : on retombe sur le
            // schéma de référence fourni par database.sql.
        }
        return VALEURS_PAR_DEFAUT;
    }
}
