package tn.enicarthage.onemorebite.model;

public class Utilisateur {
    private int id;
    private String nom;
    private String email;
    private String motDePasse;
    private String telephone;
    // Statut du compte (ACTIF, EN_ATTENTE, REJETE, DESACTIVE). Distinct du
    // statut métier d'un commerçant, même si les deux sont synchronisés.
    private String statutCompte = "ACTIF";

    public Utilisateur() {}

    public Utilisateur(int id, String nom, String email, String motDePasse, String telephone) {
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.telephone = telephone;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getStatutCompte() { return statutCompte; }
    public void setStatutCompte(String statutCompte) { this.statutCompte = statutCompte; }
}
