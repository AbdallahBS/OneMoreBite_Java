package tn.enicarthage.onemorebite.controller;

import tn.enicarthage.onemorebite.dto.AvisRequest;
import tn.enicarthage.onemorebite.model.Avis;
import tn.enicarthage.onemorebite.service.AvisService;

import java.util.List;
import java.util.Map;

public class AvisController {

    private final AvisService avisService = new AvisService();

    public List<Avis> lister() {
        return avisService.lister();
    }

    public List<Avis> listerParProduit(int produitId) {
        return avisService.listerParProduit(produitId);
    }

    public List<Avis> listerParClient(int clientId) {
        return avisService.listerParClient(clientId);
    }

    public Avis obtenir(int id) {
        return avisService.obtenir(id);
    }

    public Avis creer(AvisRequest req) {
        return avisService.creer(req);
    }

    public Avis creerPourProduit(int produitId, AvisRequest req) {
        return avisService.creerPourProduit(produitId, req);
    }

    public Avis modifier(int id, AvisRequest req) {
        return avisService.modifier(id, req);
    }

    public void supprimer(int id) {
        avisService.supprimer(id);
    }

    public Map<String, Object> obtenirStatistiquesProduit(int produitId) {
        return avisService.obtenirStatistiquesProduit(produitId);
    }
}
