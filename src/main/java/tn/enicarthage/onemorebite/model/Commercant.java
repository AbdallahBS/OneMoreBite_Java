package tn.enicarthage.onemorebite.model;

public class Commercant extends Utilisateur {
    private String localisation;
    private String statut; // "EN_ATTENTE", "ACTIF", "REJETE"

    public Commercant() {
        this.statut = "EN_ATTENTE";
    }

    public Commercant(int id, String nom, String email, String motDePasse, String telephone, String localisation) {
        super(id, nom, email, motDePasse, telephone);
        this.localisation = localisation;
        this.statut = "EN_ATTENTE";
    }

    public Commercant(int id, String nom, String email, String motDePasse, String telephone, String localisation, String statut) {
        super(id, nom, email, motDePasse, telephone);
        this.localisation = localisation;
        this.statut = statut;
    }

    public String getLocalisation() { return localisation; }
    public void setLocalisation(String localisation) { this.localisation = localisation; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
}