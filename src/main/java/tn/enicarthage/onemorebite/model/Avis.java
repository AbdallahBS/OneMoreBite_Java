package tn.enicarthage.onemorebite.model;

import java.time.LocalDateTime;

/**
 * Avis laissé par un client sur un commerçant.
 * Le client et le commerçant sont identifiés par leur identifiant utilisateur.
 */
public class Avis {
    private int id;
    private int note;
    private String commentaire;
    private LocalDateTime dateAvis;
    private int clientId;
    private int commercantId;

    public Avis() {}

    public Avis(int id, int note, String commentaire, LocalDateTime dateAvis, int clientId, int commercantId) {
        this.id = id;
        this.note = note;
        this.commentaire = commentaire;
        this.dateAvis = dateAvis;
        this.clientId = clientId;
        this.commercantId = commercantId;
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

    public int getCommercantId() { return commercantId; }
    public void setCommercantId(int commercantId) { this.commercantId = commercantId; }
}
