package tn.enicarthage.onemorebite.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration Cloudinary.
 *
 * Les valeurs peuvent être fournies :
 *  - par variable d'environnement : CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET, CLOUDINARY_FOLDER
 *  - par fichier .env (détecté automatiquement à la racine ou dans BackEnd/.env)
 *  - ou par propriété système : -Dcloudinary.cloud.name=..., -Dcloudinary.api.key=..., -Dcloudinary.api.secret=...
 */
public final class CloudinaryConfig {

    private static final Map<String, String> ENV_FILE_VARS = chargerFichierEnv();

    private static final String CLOUD_NAME = resoudre("CLOUDINARY_CLOUD_NAME", "cloudinary.cloud.name", null);
    private static final String API_KEY = resoudre("CLOUDINARY_API_KEY", "cloudinary.api.key", null);
    private static final String API_SECRET = resoudre("CLOUDINARY_API_SECRET", "cloudinary.api.secret", null);
    private static final String FOLDER = resoudre("CLOUDINARY_FOLDER", "cloudinary.folder", "onemorebite/products");

    private CloudinaryConfig() {
    }

    public static String cloudName() {
        return CLOUD_NAME;
    }

    public static String apiKey() {
        return API_KEY;
    }

    public static String apiSecret() {
        return API_SECRET;
    }

    public static String folderParDefaut() {
        return FOLDER;
    }

    public static boolean estConfigure() {
        return nEstPasVide(CLOUD_NAME) && nEstPasVide(API_KEY) && nEstPasVide(API_SECRET);
    }

    private static boolean nEstPasVide(String value) {
        return value != null && !value.isBlank();
    }

    private static String resoudre(String variableEnv, String proprieteSysteme, String valeurDefaut) {
        String valeur = System.getProperty(proprieteSysteme);
        if (valeur == null || valeur.isBlank()) {
            valeur = System.getenv(variableEnv);
        }
        if (valeur == null || valeur.isBlank()) {
            valeur = ENV_FILE_VARS.get(variableEnv);
        }
        return (valeur == null || valeur.isBlank()) ? valeurDefaut : valeur.trim();
    }

    private static Map<String, String> chargerFichierEnv() {
        Map<String, String> map = new HashMap<>();
        List<Path> chemins = List.of(
                Paths.get(".env"),
                Paths.get("BackEnd", ".env"),
                Paths.get("..", "BackEnd", ".env"),
                Paths.get("..", ".env")
        );

        for (Path chemin : chemins) {
            if (Files.isRegularFile(chemin)) {
                try {
                    List<String> lignes = Files.readAllLines(chemin, StandardCharsets.UTF_8);
                    for (String ligne : lignes) {
                        String t = ligne.trim();
                        if (t.isEmpty() || t.startsWith("#")) continue;
                        int idx = t.indexOf('=');
                        if (idx > 0) {
                            String k = t.substring(0, idx).trim();
                            String v = t.substring(idx + 1).trim();
                            if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                                v = v.substring(1, v.length() - 1);
                            }
                            map.putIfAbsent(k, v);
                        }
                    }
                    System.out.println("CloudinaryConfig : variables chargées depuis " + chemin.toAbsolutePath());
                    break;
                } catch (Exception ignored) {
                }
            }
        }
        return map;
    }
}
