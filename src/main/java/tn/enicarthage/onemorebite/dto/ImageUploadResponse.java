package tn.enicarthage.onemorebite.dto;

public record ImageUploadResponse(
        String imageUrl,
        String publicId,
        String format,
        int width,
        int height,
        long bytes
) {}
