package tn.enicarthage.onemorebite.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Produit {
    private int id;
    private String nom;
    private String description;
    private double prix;
    private int quantite;
    private String imageUrl;
    private List<String> images;
    private LocalDate dateExpiration;
    private boolean disponibilite;
    private int commercantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Produit() {
        this.images = new ArrayList<>();
    }

    public Produit(int id, String nom, String description, double prix, int quantite,
                   String imageUrl, LocalDate dateExpiration, boolean disponibilite,
                   int commercantId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.quantite = quantite;
        initImages(imageUrl);
        this.dateExpiration = dateExpiration;
        this.disponibilite = disponibilite;
        this.commercantId = commercantId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    private void initImages(String raw) {
        if (raw == null || raw.isBlank()) {
            this.imageUrl = null;
            this.images = new ArrayList<>();
            return;
        }
        String[] parts = raw.split(",");
        List<String> list = new ArrayList<>();
        for (String p : parts) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        this.images = list;
        this.imageUrl = list.isEmpty() ? null : list.get(0);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrix() { return prix; }
    public void setPrix(double prix) { this.prix = prix; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    /**
     * Retourne l'URL de l'image principale (la 1ère de la liste).
     */
    public String getImageUrl() {
        if (imageUrl != null && imageUrl.contains(",")) {
            return imageUrl.split(",")[0].trim();
        }
        return imageUrl;
    }

    /**
     * Retourne la chaîne brute avec toutes les URLs concaténées par des virgules pour la persistance SQL.
     */
    public String getRawImageUrl() {
        if (images != null && !images.isEmpty()) {
            return String.join(",", images);
        }
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        initImages(imageUrl);
    }

    public List<String> getImages() {
        if (images != null && !images.isEmpty()) {
            return images;
        }
        if (imageUrl != null && !imageUrl.isBlank()) {
            return List.of(imageUrl);
        }
        return new ArrayList<>();
    }

    public void setImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            this.images = new ArrayList<>();
            this.imageUrl = null;
        } else {
            this.images = new ArrayList<>(images);
            this.imageUrl = images.get(0);
        }
    }

    public LocalDate getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(LocalDate dateExpiration) { this.dateExpiration = dateExpiration; }

    public boolean isDisponibilite() { return disponibilite; }
    public void setDisponibilite(boolean disponibilite) { this.disponibilite = disponibilite; }

    public int getCommercantId() { return commercantId; }
    public void setCommercantId(int commercantId) { this.commercantId = commercantId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}