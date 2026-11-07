package tn.enicarthage.onemorebite.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import tn.enicarthage.onemorebite.config.CloudinaryConfig;
import tn.enicarthage.onemorebite.dto.ImageUploadRequest;
import tn.enicarthage.onemorebite.dto.ImageUploadResponse;
import tn.enicarthage.onemorebite.exception.ServiceUnavailableException;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Client Cloudinary minimal (upload + suppression) basé sur l'API HTTP officielle.
 * Aucune dépendance externe n'est nécessaire.
 */
public class CloudinaryService {

    private static final String API_BASE = "https://api.cloudinary.com/v1_1/%s/%s";
    private static final int TAILLE_MAX_IMAGE = 15_000_000; // ~10 Mo en base64

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public ImageUploadResponse uploader(ImageUploadRequest req) {
        exigerConfiguration();

        if (req == null || req.image() == null || req.image().isBlank()) {
            throw new IllegalArgumentException("Le champ 'image' est obligatoire (data URI base64 ou URL http(s)).");
        }

        String image = req.image().trim();
        if (!image.startsWith("data:image/") && !image.startsWith("http://") && !image.startsWith("https://")) {
            throw new IllegalArgumentException(
                    "Format d'image invalide : utilisez une data URI (data:image/png;base64,...) ou une URL http(s).");
        }
        if (image.length() > TAILLE_MAX_IMAGE) {
            throw new IllegalArgumentException("L'image est trop volumineuse (maximum ~10 Mo).");
        }

        String folder = (req.folder() == null || req.folder().isBlank())
                ? CloudinaryConfig.folderParDefaut()
                : req.folder().trim();

        long timestamp = Instant.now().getEpochSecond();

        // Les paramètres signés doivent être triés par ordre alphabétique.
        TreeMap<String, String> parametresSignes = new TreeMap<>();
        parametresSignes.put("folder", folder);
        parametresSignes.put("timestamp", String.valueOf(timestamp));
        String signature = signer(parametresSignes);

        Map<String, String> formulaire = new LinkedHashMap<>();
        formulaire.put("file", image);
        formulaire.put("api_key", CloudinaryConfig.apiKey());
        formulaire.put("timestamp", String.valueOf(timestamp));
        formulaire.put("folder", folder);
        formulaire.put("signature", signature);

        JsonObject json = appeler("image/upload", formulaire);

        return new ImageUploadResponse(
                obtenirChaine(json, "secure_url"),
                obtenirChaine(json, "public_id"),
                obtenirChaine(json, "format"),
                obtenirEntier(json, "width"),
                obtenirEntier(json, "height"),
                obtenirEntier(json, "bytes")
        );
    }

    public Map<String, Object> supprimer(String publicId) {
        exigerConfiguration();

        if (publicId == null || publicId.isBlank()) {
            throw new IllegalArgumentException("Le paramètre 'publicId' est obligatoire.");
        }

        String id = publicId.trim();
        long timestamp = Instant.now().getEpochSecond();

        TreeMap<String, String> parametresSignes = new TreeMap<>();
        parametresSignes.put("public_id", id);
        parametresSignes.put("timestamp", String.valueOf(timestamp));
        String signature = signer(parametresSignes);

        Map<String, String> formulaire = new LinkedHashMap<>();
        formulaire.put("public_id", id);
        formulaire.put("api_key", CloudinaryConfig.apiKey());
        formulaire.put("timestamp", String.valueOf(timestamp));
        formulaire.put("signature", signature);

        JsonObject json = appeler("image/destroy", formulaire);

        String resultat = obtenirChaine(json, "result");
        return Map.of(
                "publicId", id,
                "resultat", resultat != null ? resultat : "inconnu"
        );
    }

    private JsonObject appeler(String chemin, Map<String, String> formulaire) {
        String body = formulaire.entrySet().stream()
                .map(entry -> encoderFormulaire(entry.getKey()) + "=" + encoderFormulaire(entry.getValue()))
                .collect(Collectors.joining("&"));

        String url = String.format(API_BASE, CloudinaryConfig.cloudName(), chemin);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String message = response.body();
                if (json.has("error") && json.get("error").isJsonObject()
                        && json.getAsJsonObject("error").has("message")) {
                    message = json.getAsJsonObject("error").get("message").getAsString();
                }
                throw new IllegalArgumentException("Erreur Cloudinary : " + message);
            }

            return json;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceUnavailableException("Appel Cloudinary interrompu.");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceUnavailableException("Impossible de contacter Cloudinary : " + e.getMessage());
        }
    }

    private void exigerConfiguration() {
        if (!CloudinaryConfig.estConfigure()) {
            throw new ServiceUnavailableException(
                    "Cloudinary n'est pas configuré. Définissez CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY et CLOUDINARY_API_SECRET.");
        }
    }

    private String signer(TreeMap<String, String> parametres) {
        StringBuilder data = new StringBuilder();
        for (Map.Entry<String, String> entry : parametres.entrySet()) {
            if (data.length() > 0) {
                data.append('&');
            }
            data.append(entry.getKey()).append('=').append(entry.getValue());
        }
        data.append(CloudinaryConfig.apiSecret());
        return sha1Hex(data.toString());
    }

    private String sha1Hex(String valeur) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(valeur.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de calculer la signature Cloudinary.", e);
        }
    }

    private String encoderFormulaire(String valeur) {
        return URLEncoder.encode(valeur, StandardCharsets.UTF_8);
    }

    private String obtenirChaine(JsonObject json, String cle) {
        return json.has(cle) && !json.get(cle).isJsonNull() ? json.get(cle).getAsString() : null;
    }

    private int obtenirEntier(JsonObject json, String cle) {
        return json.has(cle) && !json.get(cle).isJsonNull() ? json.get(cle).getAsInt() : 0;
    }
}
