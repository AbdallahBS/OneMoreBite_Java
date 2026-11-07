package tn.enicarthage.onemorebite.dto;

/**
 * Requête d'upload d'image vers Cloudinary.
 * Le champ image accepte une data URI (data:image/png;base64,...) ou une URL http(s).
 */
public record ImageUploadRequest(
        String image,
        String folder
) {}
