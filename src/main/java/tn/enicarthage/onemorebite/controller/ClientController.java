package tn.enicarthage.onemorebite.controller;

import tn.enicarthage.onemorebite.dto.ClientRegisterRequest;
import tn.enicarthage.onemorebite.dto.ClientResponse;
import tn.enicarthage.onemorebite.dto.LoginRequest;
import tn.enicarthage.onemorebite.dto.LoginResponse;
import tn.enicarthage.onemorebite.service.ClientService;

public class ClientController {

    private final ClientService clientService = new ClientService();

    public ClientResponse inscrire(ClientRegisterRequest req) {
        return clientService.inscrire(req);
    }

    public LoginResponse login(LoginRequest req) {
        return clientService.login(req);
    }
}
