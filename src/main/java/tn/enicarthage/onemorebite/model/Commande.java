package tn.enicarthage.onemorebite.model;

import java.util.Date;

public class Commande {
    private int id;
    private Date dateCommande;
    private int quantite;
    private StatutCommande statut;
    private int produitId;
    private Integer clientId; // Integer d'objet (nullable pour les invités)
    private String nomInvite;
    private String emailInvite;
    private String telephoneInvite;

    public Commande() {}

    public Commande(int id, Date dateCommande, int quantite, StatutCommande statut, int produitId, Integer clientId, String nomInvite, String emailInvite, String telephoneInvite) {
        this.id = id;
        this.dateCommande = dateCommande;
        this.quantite = quantite;
        this.statut = statut;
        this.produitId = produitId;
        this.clientId = clientId;
        this.nomInvite = nomInvite;
        this.emailInvite = emailInvite;
        this.telephoneInvite = telephoneInvite;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Date getDateCommande() { return dateCommande; }
    public void setDateCommande(Date dateCommande) { this.dateCommande = dateCommande; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    public StatutCommande getStatut() { return statut; }
    public void setStatut(StatutCommande statut) { this.statut = statut; }

    public int getProduitId() { return produitId; }
    public void setProduitId(int produitId) { this.produitId = produitId; }

    public Integer getClientId() { return clientId; }
    public void setClientId(Integer clientId) { this.clientId = clientId; }

    public String getNomInvite() { return nomInvite; }
    public void setNomInvite(String nomInvite) { this.nomInvite = nomInvite; }

    public String getEmailInvite() { return emailInvite; }
    public void setEmailInvite(String emailInvite) { this.emailInvite = emailInvite; }

    public String getTelephoneInvite() { return telephoneInvite; }
    public void setTelephoneInvite(String telephoneInvite) { this.telephoneInvite = telephoneInvite; }
}