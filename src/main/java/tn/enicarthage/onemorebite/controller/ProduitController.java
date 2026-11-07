package tn.enicarthage.onemorebite.controller;

import tn.enicarthage.onemorebite.dto.ImageUploadRequest;
import tn.enicarthage.onemorebite.dto.ImageUploadResponse;
import tn.enicarthage.onemorebite.dto.ProduitRequest;
import tn.enicarthage.onemorebite.dto.ProduitStatistiques;
import tn.enicarthage.onemorebite.model.Produit;
import tn.enicarthage.onemorebite.security.JwtUtil;
import tn.enicarthage.onemorebite.service.CloudinaryService;
import tn.enicarthage.onemorebite.service.ProduitService;

import java.util.List;
import java.util.Map;

public class ProduitController {

    private final ProduitService produitService = new ProduitService();
    private final CloudinaryService cloudinaryService = new CloudinaryService();

    public List<Produit> lister(Integer commercantId, Boolean disponibilite, String recherche) {
        return produitService.lister(commercantId, disponibilite, recherche);
    }

    public List<Produit> listerMesProduits(JwtUtil.JwtClaims claims) {
        return produitService.listerMesProduits(claims);
    }

    public Produit obtenir(int id) {
        return produitService.obtenir(id);
    }

    public Produit creer(ProduitRequest req, JwtUtil.JwtClaims claims) {
        return produitService.creer(req, claims);
    }

    public Produit modifier(int id, ProduitRequest req, JwtUtil.JwtClaims claims) {
        return produitService.modifier(id, req, claims);
    }

    public Produit changerDisponibilite(int id, Boolean disponibilite, JwtUtil.JwtClaims claims) {
        return produitService.changerDisponibilite(id, disponibilite, claims);
    }

    public Produit changerStock(int id, Integer quantite, JwtUtil.JwtClaims claims) {
        return produitService.changerStock(id, quantite, claims);
    }

    public void supprimer(int id, JwtUtil.JwtClaims claims) {
        produitService.supprimer(id, claims);
    }

    public ProduitStatistiques statistiques() {
        return produitService.statistiques();
    }

    public ImageUploadResponse televerserImage(ImageUploadRequest req) {
        return cloudinaryService.uploader(req);
    }

    public Map<String, Object> supprimerImage(String publicId) {
        return cloudinaryService.supprimer(publicId);
    }
}
