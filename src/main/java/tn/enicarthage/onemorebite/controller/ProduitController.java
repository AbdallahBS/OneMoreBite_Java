package tn.enicarthage.onemorebite.controller;

import tn.enicarthage.onemorebite.dto.ProduitRequest;
import tn.enicarthage.onemorebite.model.Produit;
import tn.enicarthage.onemorebite.service.ProduitService;

import java.util.List;

public class ProduitController {

    private final ProduitService produitService = new ProduitService();

    public List<Produit> lister() {
        return produitService.lister();
    }

    public Produit obtenir(int id) {
        return produitService.obtenir(id);
    }

    public Produit creer(ProduitRequest req) {
        return produitService.creer(req);
    }

    public Produit modifier(int id, ProduitRequest req) {
        return produitService.modifier(id, req);
    }

    public void supprimer(int id) {
        produitService.supprimer(id);
    }
}