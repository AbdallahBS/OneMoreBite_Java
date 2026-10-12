package tn.enicarthage.onemorebite.model;

public class Client extends Utilisateur {
    public Client() {}

    public Client(int id, String nom, String email, String motDePasse, String telephone) {
        super(id, nom, email, motDePasse, telephone);
    }
}