package tn.enicarthage.onemorebite.model;

import java.math.BigDecimal;
import java.util.Date;

public class Produit {
    private int id;
    private String nom;
    private String description;
    private BigDecimal prix;
    private int quantite;
    private String imageUrl;
    private Date dateExpiration;
    private boolean disponibilite;
    private int commercantId;

    public Produit() {}

    public Produit(int id, String nom, String description, BigDecimal prix, int quantite, String imageUrl, Date dateExpiration, boolean disponibilite, int commercantId) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.quantite = quantite;
        this.imageUrl = imageUrl;
        this.dateExpiration = dateExpiration;
        this.disponibilite = disponibilite;
        this.commercantId = commercantId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrix() { return prix; }
    public void setPrix(BigDecimal prix) { this.prix = prix; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Date getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(Date dateExpiration) { this.dateExpiration = dateExpiration; }

    public boolean isDisponibilite() { return disponibilite; }
    public void setDisponibilite(boolean disponibilite) { this.disponibilite = disponibilite; }

    public int getCommercantId() { return commercantId; }
    public void setCommercantId(int commercantId) { this.commercantId = commercantId; }
}