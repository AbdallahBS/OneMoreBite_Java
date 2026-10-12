package tn.enicarthage.onemorebite.model;

public class Commercant extends Utilisateur {
    private String localisation;

    public Commercant() {}

    public Commercant(int id, String nom, String email, String motDePasse, String telephone, String localisation) {
        super(id, nom, email, motDePasse, telephone);
        this.localisation = localisation;
    }

    public String getLocalisation() { return localisation; }
    public void setLocalisation(String localisation) { this.localisation = localisation; }
}