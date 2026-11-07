package tn.enicarthage.onemorebite.config;

/**
 * Configuration Cloudinary.
 *
 * Les valeurs peuvent être fournies :
 *  - par variable d'environnement : CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET, CLOUDINARY_FOLDER
 *  - ou par propriété système : -Dcloudinary.cloud.name=..., -Dcloudinary.api.key=..., -Dcloudinary.api.secret=...
 */
public final class CloudinaryConfig {

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
        return (valeur == null || valeur.isBlank()) ? valeurDefaut : valeur.trim();
    }
}
