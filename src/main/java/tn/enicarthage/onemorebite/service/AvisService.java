package tn.enicarthage.onemorebite.service;

import tn.enicarthage.onemorebite.dao.AvisDAO;
import tn.enicarthage.onemorebite.dao.ProduitDAO;
import tn.enicarthage.onemorebite.dto.AvisRequest;
import tn.enicarthage.onemorebite.exception.ResourceNotFoundException;
import tn.enicarthage.onemorebite.model.Avis;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AvisService {

    private final AvisDAO avisDAO = new AvisDAO();
    private final ProduitDAO produitDAO = new ProduitDAO();

    public List<Avis> lister() {
        return avisDAO.listerTous();
    }

    public List<Avis> listerParProduit(int produitId) {
        verifierProduitExiste(produitId);
        return avisDAO.listerParProduit(produitId);
    }

    public List<Avis> listerParClient(int clientId) {
        return avisDAO.listerParClient(clientId);
    }

    public Avis obtenir(int id) {
        return avisDAO.trouverParId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Avis non trouvé avec id: " + id));
    }

    public Avis creer(AvisRequest req) {
        validerRequete(req);
        int produitId = req.produitId();
        verifierProduitExiste(produitId);

        Avis a = new Avis();
        a.setNote(req.note());
        a.setCommentaire(req.commentaire());
        a.setDateAvis(LocalDateTime.now());
        a.setClientId(req.clientId());
        a.setProduitId(produitId);

        return avisDAO.creer(a);
    }

    public Avis creerPourProduit(int produitId, AvisRequest req) {
        AvisRequest normalise = new AvisRequest(
                req.note(),
                req.commentaire(),
                req.clientId(),
                produitId
        );
        return creer(normalise);
    }

    public Avis modifier(int id, AvisRequest req) {
        if (req.note() < 1 || req.note() > 5) {
            throw new IllegalArgumentException("La note doit être comprise entre 1 et 5.");
        }

        Avis a = obtenir(id);
        a.setNote(req.note());
        if (req.commentaire() != null) {
            a.setCommentaire(req.commentaire());
        }

        avisDAO.modifier(a);
        return a;
    }

    public void supprimer(int id) {
        if (!avisDAO.supprimer(id)) {
            throw new ResourceNotFoundException("Avis non trouvé avec id: " + id);
        }
    }

    public Map<String, Object> obtenirStatistiquesProduit(int produitId) {
        verifierProduitExiste(produitId);
        double moyenne = avisDAO.calculerMoyenneProduit(produitId);
        int total = avisDAO.compterAvisProduit(produitId);
        return Map.of(
                "produitId", produitId,
                "noteMoyenne", Math.round(moyenne * 100.0) / 100.0,
                "nombreAvis", total
        );
    }

    private void validerRequete(AvisRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Le corps de la requête ne peut pas être vide.");
        }
        if (req.note() < 1 || req.note() > 5) {
            throw new IllegalArgumentException("La note doit être comprise entre 1 et 5.");
        }
        if (req.clientId() <= 0) {
            throw new IllegalArgumentException("L'identifiant du client est obligatoire.");
        }
        if (req.produitId() == null || req.produitId() <= 0) {
            throw new IllegalArgumentException("L'identifiant du produit est obligatoire.");
        }
    }

    private void verifierProduitExiste(int produitId) {
        if (produitDAO.trouverParId(produitId).isEmpty()) {
            throw new ResourceNotFoundException("Produit non trouvé avec id: " + produitId);
        }
    }
}
