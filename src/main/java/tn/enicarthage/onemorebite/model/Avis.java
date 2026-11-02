package tn.enicarthage.onemorebite.model;

import java.time.LocalDateTime;

public class Avis {
    private int id;
    private int note;
    private String commentaire;
    private LocalDateTime dateAvis;
    private int clientId;
    private int produitId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Avis() {}

    public Avis(int id, int note, String commentaire, LocalDateTime dateAvis, int clientId, int produitId) {
        this(id, note, commentaire, dateAvis, clientId, produitId, null, null);
    }

    public Avis(int id, int note, String commentaire, LocalDateTime dateAvis, int clientId, int produitId,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.note = note;
        this.commentaire = commentaire;
        this.dateAvis = dateAvis;
        this.clientId = clientId;
        this.produitId = produitId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getNote() { return note; }
    public void setNote(int note) { this.note = note; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public LocalDateTime getDateAvis() { return dateAvis; }
    public void setDateAvis(LocalDateTime dateAvis) { this.dateAvis = dateAvis; }

    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }

    public int getProduitId() { return produitId; }
    public void setProduitId(int produitId) { this.produitId = produitId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
