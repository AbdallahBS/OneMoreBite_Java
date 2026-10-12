package tn.enicarthage.onemorebite.model;

public class Administrateur extends Utilisateur {
    public Administrateur() {}

    public Administrateur(int id, String nom, String email, String motDePasse, String telephone) {
        super(id, nom, email, motDePasse, telephone);
    }
}