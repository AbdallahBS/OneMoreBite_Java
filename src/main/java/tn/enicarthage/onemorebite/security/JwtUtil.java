package tn.enicarthage.onemorebite.security;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import tn.enicarthage.onemorebite.exception.AuthenticationException;
import tn.enicarthage.onemorebite.model.Administrateur;
import tn.enicarthage.onemorebite.model.Commercant;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class JwtUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SECRET_KEY = "OneMoreBite_Enicar_Admin_Secure_Secret_Key_2026_JWT_Token_Auth";
    private static final long EXPIRATION_SECONDS = 24 * 60 * 60; // 24 hours

    private final Gson gson = new Gson();

    // Liste des tokens révoqués (Blacklist thread-safe pour le logout)
    private final Set<String> tokensRevoques = ConcurrentHashMap.newKeySet();

    public record JwtClaims(
            int id,
            String sub,
            String nom,
            String role,
            long iat,
            long exp
    ) {}

    public String genererToken(int id, String email, String nom, String role) {
        try {
            long now = Instant.now().getEpochSecond();
            long exp = now + EXPIRATION_SECONDS;
            JsonObject header = new JsonObject();
            header.addProperty("alg", "HS256");
            header.addProperty("typ", "JWT");
            String encodedHeader = base64UrlEncode(gson.toJson(header).getBytes(StandardCharsets.UTF_8));
            JsonObject payload = new JsonObject();
            payload.addProperty("sub", email);
            payload.addProperty("id", id);
            payload.addProperty("nom", nom);
            payload.addProperty("role", role);
            payload.addProperty("iat", now);
            payload.addProperty("exp", exp);
            String encodedPayload = base64UrlEncode(gson.toJson(payload).getBytes(StandardCharsets.UTF_8));
            String dataToSign = encodedHeader + "." + encodedPayload;
            String signature = sign(dataToSign, SECRET_KEY);

            return dataToSign + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du token JWT", e);
        }
    }

    public String genererToken(Administrateur admin) {
        return genererToken(admin.getId(), admin.getEmail(), admin.getNom(), "ADMIN");
    }

    public String genererToken(Commercant commercant) {
        return genererToken(commercant.getId(), commercant.getEmail(), commercant.getNom(), "COMMERCANT");
    }

    public JwtClaims validerToken(String token) {
        if (token == null || token.isBlank()) {
            throw new AuthenticationException("Token manquant ou vide.");
        }

        String cleanedToken = token.trim();

        if (estRevoque(cleanedToken)) {
            throw new AuthenticationException("Session révoquée suite à une déconnexion. Veuillez vous reconnecter.");
        }

        String[] parts = cleanedToken.split("\\.");
        if (parts.length != 3) {
            throw new AuthenticationException("Format de token JWT invalide.");
        }

        String encodedHeader = parts[0];
        String encodedPayload = parts[1];
        String signature = parts[2];

        String dataToSign = encodedHeader + "." + encodedPayload;
        String expectedSignature = sign(dataToSign, SECRET_KEY);

        if (!MessageDigest.isEqual(
                signature.getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new AuthenticationException("Signature du token JWT invalide.");
        }

        // 2. Décodage du payload
        String payloadJson = new String(base64UrlDecode(encodedPayload), StandardCharsets.UTF_8);
        JwtClaims claims = gson.fromJson(payloadJson, JwtClaims.class);

        // 3. Vérification de l'expiration
        long now = Instant.now().getEpochSecond();
        if (claims.exp() < now) {
            throw new AuthenticationException("Session expirée. Veuillez vous reconnecter.");
        }

        return claims;
    }

    public JwtClaims validerTokenAdmin(String token) {
        JwtClaims claims = validerToken(token);
        if (!"ADMIN".equalsIgnoreCase(claims.role())) {
            throw new AuthenticationException("Accès non autorisé : rôle administrateur requis.");
        }
        return claims;
    }

    public JwtClaims validerTokenCommercant(String token) {
        JwtClaims claims = validerToken(token);
        if (!"COMMERCANT".equalsIgnoreCase(claims.role()) && !"ADMIN".equalsIgnoreCase(claims.role())) {
            throw new AuthenticationException("Accès non autorisé : compte commerçant requis.");
        }
        return claims;
    }

    public void revoquerToken(String token) {
        if (token != null && !token.isBlank()) {
            tokensRevoques.add(token.trim());
        }
    }

    public boolean estRevoque(String token) {
        return token != null && tokensRevoques.contains(token.trim());
    }

    private String sign(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] signatureBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return base64UrlEncode(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Erreur de signature JWT", e);
        }
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] base64UrlDecode(String str) {
        return Base64.getUrlDecoder().decode(str);
    }
}
