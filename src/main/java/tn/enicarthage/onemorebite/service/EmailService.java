package tn.enicarthage.onemorebite.service;

import tn.enicarthage.onemorebite.dto.ReservationResponse;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmailService {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4);

    public void envoyerEmailAsync(String destinataire, String sujet, String contenuHtml) {
        EXECUTOR.submit(() -> {
            try {
                envoyerEmail(destinataire, sujet, contenuHtml);
            } catch (Exception e) {
                System.err.println("[EmailService] Échec de l'envoi d'email à " + destinataire + " : " + e.getMessage());
            }
        });
    }

    public void envoyerOtpAsync(String destinataire, String nomDestinataire, String codeOtp) {
        String sujet = "One More Bite - Votre code de vérification : " + codeOtp;
        String html = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #FAF7F2; padding: 24px; color: #231F20; line-height: 1.6;">
                  <div style="max-width: 560px; margin: 0 auto; background: #ffffff; border-radius: 16px; border: 1px solid #F0E6DE; padding: 32px; box-shadow: 0 4px 12px rgba(0,0,0,0.04);">
                    <div style="text-align: center; margin-bottom: 24px;">
                      <h1 style="color: #631D24; font-size: 24px; font-weight: 800; margin: 0;">One More Bite</h1>
                      <p style="color: #4A4546; font-size: 13px; margin-top: 4px;">Le réseau anti-gaspillage alimentaire en Tunisie</p>
                    </div>
                    
                    <h2 style="font-size: 18px; color: #231F20; margin-bottom: 12px;">Bonjour %s,</h2>
                    <p style="font-size: 14px; color: #4A4546; margin-bottom: 20px;">
                      Bienvenue sur One More Bite ! Pour confirmer votre adresse email et sécuriser votre compte, veuillez saisir le code de vérification à 6 chiffres suivant :
                    </p>
                    
                    <div style="background-color: #FBF4EC; border: 2px dashed #631D24; border-radius: 12px; padding: 18px; text-align: center; margin: 24px 0;">
                      <span style="font-family: monospace; font-size: 34px; font-weight: 800; letter-spacing: 8px; color: #631D24;">%s</span>
                    </div>
                    
                    <p style="font-size: 12px; color: #7A7576; text-align: center; margin-bottom: 24px;">
                      Ce code est valable pendant <strong>15 minutes</strong>. Ne le partagez avec personne.
                    </p>
                    
                    <hr style="border: none; border-top: 1px solid #F0E6DE; margin: 20px 0;" />
                    <p style="font-size: 11px; color: #9E9E9E; text-align: center; margin: 0;">
                      Si vous n'avez pas demandé cette inscription, veuillez ignorer cet email en toute sécurité.
                    </p>
                  </div>
                </body>
                </html>
                """.formatted(
                nomDestinataire != null && !nomDestinataire.isBlank() ? nomDestinataire : "Gourmand",
                codeOtp
        );

        // Toujours afficher le code dans la console du serveur pour un confort de développement total
        System.out.println("=================================================");
        System.out.println("[OTP CODE] Pour: " + destinataire + " | CODE: " + codeOtp);
        System.out.println("=================================================");

        envoyerEmailAsync(destinataire, sujet, html);
    }

    public void notifierConfirmationReservationAsync(ReservationResponse res) {
        String clientEmail = res.clientEmail() != null ? res.clientEmail() : res.emailInvite();
        if (clientEmail == null || clientEmail.isBlank()) return;

        String sujet = "Réservation #" + res.id() + " CONFIRMÉE chez " + res.commercantNom();
        String html = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #FAF7F2; padding: 24px; color: #231F20; line-height: 1.6;">
                  <div style="max-width: 560px; margin: 0 auto; background: #ffffff; border-radius: 16px; border: 1px solid #F0E6DE; padding: 32px;">
                    <div style="text-align: center; margin-bottom: 20px;">
                      <h1 style="color: #631D24; font-size: 22px; margin: 0;">One More Bite</h1>
                    </div>
                    <div style="padding: 12px 16px; background-color: #EAF5E9; border-radius: 10px; margin-bottom: 20px; border-left: 4px solid #346330;">
                      <h2 style="color: #346330; font-size: 16px; margin: 0;">✓ Votre réservation #%d est confirmée !</h2>
                    </div>
                    <p style="font-size: 14px; color: #4A4546;">
                      Bonjour <strong>%s</strong>, votre panier anti-gaspi a été préparé et validé par <strong>%s</strong>.
                    </p>
                    <div style="background-color: #FAF7F2; border-radius: 12px; padding: 16px; margin: 20px 0; font-size: 13px;">
                      <p style="margin: 4px 0;"><strong>Panier :</strong> %s</p>
                      <p style="margin: 4px 0;"><strong>Quantité :</strong> x%d</p>
                      <p style="margin: 4px 0;"><strong>Montant total à régler en espèces :</strong> %.3f DT</p>
                      <p style="margin: 4px 0;"><strong>Adresse de retrait :</strong> %s</p>
                    </div>
                    <p style="font-size: 13px; color: #346330; font-weight: bold;">
                      🌱 Pensez à apporter votre sac réutilisable lors du retrait de votre commande !
                    </p>
                  </div>
                </body>
                </html>
                """.formatted(
                res.id(),
                res.clientNom() != null ? res.clientNom() : "Client",
                res.commercantNom(),
                res.produitNom(),
                res.quantite(),
                res.produitPrix() * res.quantite(),
                res.commercantLocalisation() != null ? res.commercantLocalisation() : "Voir l'adresse du commerçant"
        );

        envoyerEmailAsync(clientEmail, sujet, html);
    }

    public void notifierRefusReservationAsync(ReservationResponse res) {
        String clientEmail = res.clientEmail() != null ? res.clientEmail() : res.emailInvite();
        if (clientEmail == null || clientEmail.isBlank()) return;

        String sujet = "Mise à jour : Réservation #" + res.id() + " non disponible";
        String html = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #FAF7F2; padding: 24px; color: #231F20; line-height: 1.6;">
                  <div style="max-width: 560px; margin: 0 auto; background: #ffffff; border-radius: 16px; border: 1px solid #F0E6DE; padding: 32px;">
                    <div style="text-align: center; margin-bottom: 20px;">
                      <h1 style="color: #631D24; font-size: 22px; margin: 0;">One More Bite</h1>
                    </div>
                    <div style="padding: 12px 16px; background-color: #FDEDED; border-radius: 10px; margin-bottom: 20px; border-left: 4px solid #B3261E;">
                      <h2 style="color: #B3261E; font-size: 16px; margin: 0;">Réservation non acceptée</h2>
                    </div>
                    <p style="font-size: 14px; color: #4A4546;">
                      Bonjour <strong>%s</strong>, l'établissement <strong>%s</strong> n'a pas pu valider votre réservation #%d pour le panier <em>%s</em> (rupture d'invendus du jour).
                    </p>
                    <p style="font-size: 13px; color: #4A4546; margin-top: 16px;">
                      Rassurez-vous : aucun montant n'a été prélevé. Vous pouvez dès maintenant choisir un autre panier gourmand disponible près de chez vous sur One More Bite.
                    </p>
                  </div>
                </body>
                </html>
                """.formatted(
                res.clientNom() != null ? res.clientNom() : "Client",
                res.commercantNom(),
                res.id(),
                res.produitNom()
        );

        envoyerEmailAsync(clientEmail, sujet, html);
    }

    public void notifierAnnulationReservationAsync(ReservationResponse res, String emailCommercant) {
        if (emailCommercant == null || emailCommercant.isBlank()) return;

        String sujet = "Réservation #" + res.id() + " annulée par le client";
        String html = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #FAF7F2; padding: 24px; color: #231F20; line-height: 1.6;">
                  <div style="max-width: 560px; margin: 0 auto; background: #ffffff; border-radius: 16px; border: 1px solid #F0E6DE; padding: 32px;">
                    <div style="text-align: center; margin-bottom: 20px;">
                      <h1 style="color: #631D24; font-size: 22px; margin: 0;">One More Bite</h1>
                    </div>
                    <div style="padding: 12px 16px; background-color: #FFF5E6; border-radius: 10px; margin-bottom: 20px; border-left: 4px solid #E65100;">
                      <h2 style="color: #E65100; font-size: 16px; margin: 0;">Notification d'annulation</h2>
                    </div>
                    <p style="font-size: 14px; color: #4A4546;">
                      Le client <strong>%s</strong> a annulé sa réservation <strong>#%d</strong> pour votre panier <strong>%s</strong> (quantité : x%d).
                    </p>
                    <p style="font-size: 13px; color: #346330; margin-top: 16px;">
                      ✓ La quantité correspondante a été automatiquement réintégrée à votre stock en ligne.
                    </p>
                  </div>
                </body>
                </html>
                """.formatted(
                res.clientNom() != null ? res.clientNom() : "Un client",
                res.id(),
                res.produitNom(),
                res.quantite()
        );

        envoyerEmailAsync(emailCommercant, sujet, html);
    }


    public void envoyerEmail(String destinataire, String sujet, String contenuHtml)
            throws IOException {

        String apiKey = System.getenv("RESEND_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IOException("RESEND_API_KEY non configurée.");
        }

        String from = System.getenv().getOrDefault(
                "RESEND_FROM_EMAIL",
                "One More Bite <onboarding@resend.dev>"
        );

        try {
            Resend resend = new Resend(apiKey);

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(from)
                    .to(destinataire)
                    .subject(sujet)
                    .html(contenuHtml)
                    .build();

            CreateEmailResponse response = resend.emails().send(params);

            System.out.println(
                    "[EmailService] Email accepté par Resend. ID: "
                            + response.getId()
            );

        } catch (ResendException e) {
            throw new IOException(
                    "Erreur lors de l'envoi via Resend: " + e.getMessage(), e
            );
        }
    }
    
}
