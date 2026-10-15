package tn.enicarthage.onemorebite.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Produit {
    private int id;
    private String nom;
    private String description;
    private double prix;
    private int quantite;
    private String imageUrl;
    private LocalDate dateExpiration;
    private boolean disponibilite;
    private int commercantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Produit() {}

    public Produit(int id, String nom, String description, double prix, int quantite,
                   String imageUrl, LocalDate dateExpiration, boolean disponibilite,
                   int commercantId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.quantite = quantite;
        this.imageUrl = imageUrl;
        this.dateExpiration = dateExpiration;
        this.disponibilite = disponibilite;
        this.commercantId = commercantId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

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