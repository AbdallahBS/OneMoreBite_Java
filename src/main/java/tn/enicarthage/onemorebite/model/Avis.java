package tn.enicarthage.onemorebite.model;

import java.util.Date;

public class Avis {
    private int id;
    private int note;
    private String commentaire;
    private Date dateAvis;
    private int clientId;
    private int produitId;

    public Avis() {}

    public Avis(int id, int note, String commentaire, Date dateAvis, int clientId, int produitId) {
        this.id = id;
        this.note = note;
        this.commentaire = commentaire;
        this.dateAvis = dateAvis;
        this.clientId = clientId;
        this.produitId = produitId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getNote() { return note; }
    public void setNote(int note) { this.note = note; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public Date getDateAvis() { return dateAvis; }
    public void setDateAvis(Date dateAvis) { this.dateAvis = dateAvis; }

    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }

    public int getProduitId() { return produitId; }
    public void setProduitId(int produitId) { this.produitId = produitId; }
}