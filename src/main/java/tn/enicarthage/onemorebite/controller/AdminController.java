package tn.enicarthage.onemorebite.controller;

import tn.enicarthage.onemorebite.dto.ReservationResponse;
import tn.enicarthage.onemorebite.dto.UtilisateurResponse;
import tn.enicarthage.onemorebite.service.AdminService;

import java.util.List;

public class AdminController {

    private final AdminService adminService = new AdminService();

    public List<UtilisateurResponse> listerUtilisateurs(String role, String statut) {
        return adminService.listerUtilisateurs(role, statut);
    }

    public UtilisateurResponse activerUtilisateur(int id) {
        return adminService.activerUtilisateur(id);
    }

    public UtilisateurResponse desactiverUtilisateur(int id, int adminConnecteId) {
        return adminService.desactiverUtilisateur(id, adminConnecteId);
    }

    public List<ReservationResponse> listerReservations(String statut, Integer clientId, Integer commercantId) {
        return adminService.listerReservations(statut, clientId, commercantId);
    }

    public ReservationResponse obtenirReservation(int id) {
        return adminService.obtenirReservation(id);
    }
}
