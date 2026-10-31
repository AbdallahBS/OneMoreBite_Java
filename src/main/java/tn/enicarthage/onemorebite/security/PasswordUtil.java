package tn.enicarthage.onemorebite.security;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtil {

    private PasswordUtil() {}

    public static String hash(String motDePasseClair) {
        if (motDePasseClair == null) {
            throw new IllegalArgumentException("Le mot de passe ne peut pas être null.");
        }
        return BCrypt.hashpw(motDePasseClair, BCrypt.gensalt(12));
    }

    public static boolean verifier(String motDePasseClair, String motDePasseStocke) {
        if (motDePasseClair == null || motDePasseStocke == null) {
            return false;
        }

        if (estHashBCrypt(motDePasseStocke)) {
            try {
                return BCrypt.checkpw(motDePasseClair, motDePasseStocke);
            } catch (Exception e) {
                return false;
            }
        }
        return motDePasseStocke.equals(motDePasseClair);
    }
    public static boolean estHashBCrypt(String motDePasse) {
        return motDePasse != null && (
                motDePasse.startsWith("$2a$") ||
                motDePasse.startsWith("$2b$") ||
                motDePasse.startsWith("$2y$")
        );
    }
}
