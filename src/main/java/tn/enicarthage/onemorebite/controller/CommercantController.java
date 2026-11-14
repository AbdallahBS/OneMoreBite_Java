package tn.enicarthage.onemorebite.controller;

import tn.enicarthage.onemorebite.dto.CommercantPublicResponse;
import tn.enicarthage.onemorebite.dto.CommercantRegisterRequest;
import tn.enicarthage.onemorebite.dto.CommercantResponse;
import tn.enicarthage.onemorebite.dto.LoginRequest;
import tn.enicarthage.onemorebite.dto.LoginResponse;
import tn.enicarthage.onemorebite.service.CommercantService;

import java.util.List;

public class CommercantController {

    private final CommercantService commercantService = new CommercantService();

    public CommercantResponse inscrire(CommercantRegisterRequest req) {
        return commercantService.inscrire(req);
    }

    public LoginResponse login(LoginRequest req) {
        return commercantService.login(req);
    }

    public List<CommercantResponse> lister(String statutFiltre) {
        return commercantService.lister(statutFiltre);
    }

    public List<CommercantPublicResponse> listerActifsPublics(String recherche) {
        return commercantService.listerActifsPublics(recherche);
    }

    public CommercantResponse obtenir(int id) {
        return commercantService.obtenir(id);
    }

    public CommercantResponse accepter(int id) {
        return commercantService.accepter(id);
    }

    public CommercantResponse rejeter(int id) {
        return commercantService.rejeter(id);
    }
}
