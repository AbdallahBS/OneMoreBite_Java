package tn.enicarthage.onemorebite.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EmailConfig {

    private static final Map<String, String> ENV_FILE_VARS = chargerFichierEnv();

    private static final String HOST = resoudre("SMTP_HOST", "smtp.host", "smtp.gmail.com");
    private static final int PORT = resoudreInt("SMTP_PORT", "smtp.port", 465);
    private static final String USER = resoudre("SMTP_USER", "smtp.user", resoudre("SMTP_EMAIL", "smtp.email", ""));
    private static final String PASSWORD = resoudre("SMTP_PASSWORD", "smtp.password", resoudre("SMTP_APP_PASSWORD", "smtp.app_password", ""));
    private static final String FROM_NAME = resoudre("SMTP_FROM_NAME", "smtp.from_name", "One More Bite");

    private EmailConfig() {}

    public static String host() {
        return resoudre("SMTP_HOST", "smtp.host", HOST);
    }

    public static int port() {
        return resoudreInt("SMTP_PORT", "smtp.port", PORT);
    }

    public static String user() {
        return resoudre("SMTP_USER", "smtp.user", resoudre("SMTP_EMAIL", "smtp.email", USER));
    }

    public static String password() {
        return resoudre("SMTP_PASSWORD", "smtp.password", resoudre("SMTP_APP_PASSWORD", "smtp.app_password", PASSWORD));
    }

    public static String fromName() {
        return resoudre("SMTP_FROM_NAME", "smtp.from_name", FROM_NAME);
    }

    public static boolean estConfigure() {
        String u = user();
        String p = password();
        return u != null && !u.isBlank() && p != null && !p.isBlank();
    }

    private static String resoudre(String nomEnv, String clePropriete, String defaut) {
        // Recharger le fichier .env au besoin si modifié à chaud
        Map<String, String> envMap = chargerFichierEnv();
        String vFichier = envMap.get(nomEnv);
        if (vFichier != null && !vFichier.isBlank()) {
            return vFichier.trim();
        }

        String vEnv = System.getenv(nomEnv);
        if (vEnv != null && !vEnv.isBlank()) {
            return vEnv.trim();
        }

        String vProp = System.getProperty(clePropriete);
        if (vProp != null && !vProp.isBlank()) {
            return vProp.trim();
        }

        return defaut;
    }

    private static int resoudreInt(String nomEnv, String clePropriete, int defaut) {
        String val = resoudre(nomEnv, clePropriete, null);
        if (val == null || val.isBlank()) {
            return defaut;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaut;
        }
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
                } catch (IOException ignored) {}
            }
        }
        return map;
    }
}
