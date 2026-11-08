package tn.enicarthage.onemorebite.controller;

import tn.enicarthage.onemorebite.dto.ReservationRequest;
import tn.enicarthage.onemorebite.dto.ReservationResponse;
import tn.enicarthage.onemorebite.dto.ReservationUpdateRequest;
import tn.enicarthage.onemorebite.security.JwtUtil;
import tn.enicarthage.onemorebite.service.ReservationService;

import java.util.List;

public class ReservationController {

    private final ReservationService reservationService = new ReservationService();

    public ReservationResponse creer(ReservationRequest req, JwtUtil.JwtClaims claims) {
        return reservationService.creer(req, claims);
    }

    public List<ReservationResponse> listerMesReservations(JwtUtil.JwtClaims claims) {
        return reservationService.listerMesReservations(claims);
    }

    public List<ReservationResponse> listerRecues(JwtUtil.JwtClaims claims) {
        return reservationService.listerRecues(claims);
    }

    public ReservationResponse obtenir(int id, JwtUtil.JwtClaims claims) {
        return reservationService.obtenir(id, claims);
    }

    public ReservationResponse modifier(int id, ReservationUpdateRequest req, JwtUtil.JwtClaims claims) {
        return reservationService.modifier(id, req, claims);
    }

    public ReservationResponse annuler(int id, JwtUtil.JwtClaims claims) {
        return reservationService.annuler(id, claims);
    }

    public ReservationResponse confirmer(int id, JwtUtil.JwtClaims claims) {
        return reservationService.confirmer(id, claims);
    }

    public ReservationResponse refuser(int id, JwtUtil.JwtClaims claims) {
        return reservationService.refuser(id, claims);
    }
}
