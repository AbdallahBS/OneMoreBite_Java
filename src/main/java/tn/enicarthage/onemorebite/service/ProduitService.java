package tn.enicarthage.onemorebite.service;

import tn.enicarthage.onemorebite.dao.ProduitDAO;
import tn.enicarthage.onemorebite.dto.ProduitRequest;
import tn.enicarthage.onemorebite.exception.ResourceNotFoundException;
import tn.enicarthage.onemorebite.model.Produit;

import java.util.List;

public class ProduitService {

    private final ProduitDAO produitDAO = new ProduitDAO();

    public List<Produit> lister() {
        return produitDAO.listerTous();
    }

    public Produit obtenir(int id) {
        return produitDAO.trouverParId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé avec id: " + id));
    }

    public Produit creer(ProduitRequest req) {
        Produit p = new Produit();
        p.setNom(req.nom());
        p.setDescription(req.description());
        p.setPrix(req.prix());
        p.setQuantite(req.quantite());
        p.setImageUrl(req.imageUrl());
        p.setDateExpiration(req.dateExpiration());
        p.setDisponibilite(req.disponibilite());
        p.setCommercantId(req.commercantId());

        return produitDAO.creer(p);
    }

    public Produit modifier(int id, ProduitRequest req) {
        Produit p = obtenir(id); // Vériﬁe l'existence (lève 404 si introuvable)
        p.setNom(req.nom());
        p.setDescription(req.description());
        p.setPrix(req.prix());
        p.setQuantite(req.quantite());
        p.setImageUrl(req.imageUrl());
        p.setDateExpiration(req.dateExpiration());
        p.setDisponibilite(req.disponibilite());
        p.setCommercantId(req.commercantId());

        produitDAO.modifier(p);
        return p;
    }

    public void supprimer(int id) {
        if (!produitDAO.supprimer(id)) {
            throw new ResourceNotFoundException("Produit non trouvé avec id: " + id);
        }
    }
}