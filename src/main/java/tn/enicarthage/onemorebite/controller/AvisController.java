package tn.enicarthage.onemorebite.controller;

import tn.enicarthage.onemorebite.dto.AvisCommercantResponse;
import tn.enicarthage.onemorebite.dto.AvisRequest;
import tn.enicarthage.onemorebite.dto.AvisResponse;
import tn.enicarthage.onemorebite.dto.AvisUpdateRequest;
import tn.enicarthage.onemorebite.security.JwtUtil;
import tn.enicarthage.onemorebite.service.AvisService;

import java.util.List;

public class AvisController {

    private final AvisService avisService = new AvisService();

    public AvisResponse creer(AvisRequest req, JwtUtil.JwtClaims claims) {
        return avisService.creer(req, claims);
    }

    public List<AvisResponse> lister(Integer commercantId) {
        return avisService.lister(commercantId);
    }

    public AvisCommercantResponse listerParCommercant(int commercantId) {
        return avisService.listerParCommercant(commercantId);
    }

    public AvisResponse modifier(int id, AvisUpdateRequest req, JwtUtil.JwtClaims claims) {
        return avisService.modifier(id, req, claims);
    }

    public void supprimer(int id, JwtUtil.JwtClaims claims) {
        avisService.supprimer(id, claims);
    }
}
