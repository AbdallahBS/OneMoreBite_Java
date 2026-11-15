package tn.enicarthage.onemorebite.service;

import tn.enicarthage.onemorebite.config.EmailConfig;
import tn.enicarthage.onemorebite.dto.ReservationResponse;

import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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

    public void envoyerEmail(String destinataire, String sujet, String contenuHtml) throws IOException {
        String host = EmailConfig.host();
        int port = EmailConfig.port();
        String user = EmailConfig.user();
        String password = EmailConfig.password();
        String fromName = EmailConfig.fromName();

        if (user == null || user.isBlank() || password == null || password.isBlank()) {
            System.out.println("[EmailService] SMTP_USER ou SMTP_PASSWORD non renseigné dans .env. Email non envoyé sur le réseau (simulé en console pour: " + destinataire + ")");
            return;
        }

        Socket socket;
        if (port == 465) {
            SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
            socket = sf.createSocket(host, port);
        } else {
            socket = new Socket(host, port);
        }

        socket.setSoTimeout(15000);

        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

        try {
            // Lecture de la bannière initiale SMTP
            lireReponse(reader, 220);

            // EHLO
            ecrireLigne(writer, "EHLO localhost");
            lireReponse(reader, 250);

            // Mise à niveau STARTTLS si port 587
            if (port != 465) {
                ecrireLigne(writer, "STARTTLS");
                lireReponse(reader, 220);
                SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
                socket = sf.createSocket(socket, host, port, true);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                ecrireLigne(writer, "EHLO localhost");
                lireReponse(reader, 250);
            }

            // AUTH LOGIN
            ecrireLigne(writer, "AUTH LOGIN");
            lireReponse(reader, 334);

            String userB64 = Base64.getEncoder().encodeToString(user.getBytes(StandardCharsets.UTF_8));
            ecrireLigne(writer, userB64);
            lireReponse(reader, 334);

            String cleanPass = password.replace(" ", "");
            String passB64 = Base64.getEncoder().encodeToString(cleanPass.getBytes(StandardCharsets.UTF_8));
            ecrireLigne(writer, passB64);
            lireReponse(reader, 235);

            // MAIL FROM
            ecrireLigne(writer, "MAIL FROM:<" + user + ">");
            lireReponse(reader, 250);

            // RCPT TO
            ecrireLigne(writer, "RCPT TO:<" + destinataire + ">");
            lireReponse(reader, 250);

            // DATA
            ecrireLigne(writer, "DATA");
            lireReponse(reader, 354);

            String encodedSubject = "=?UTF-8?B?" + Base64.getEncoder().encodeToString(sujet.getBytes(StandardCharsets.UTF_8)) + "?=";
            String encodedFromName = "=?UTF-8?B?" + Base64.getEncoder().encodeToString(fromName.getBytes(StandardCharsets.UTF_8)) + "?=";

            writer.write("From: " + encodedFromName + " <" + user + ">\r\n");
            writer.write("To: <" + destinataire + ">\r\n");
            writer.write("Subject: " + encodedSubject + "\r\n");
            writer.write("MIME-Version: 1.0\r\n");
            writer.write("Content-Type: text/html; charset=UTF-8\r\n");
            writer.write("Content-Transfer-Encoding: 8bit\r\n");
            writer.write("\r\n");
            writer.write(contenuHtml);
            writer.write("\r\n.\r\n");
            writer.flush();
            lireReponse(reader, 250);

            // QUIT
            ecrireLigne(writer, "QUIT");
            try {
                lireReponse(reader, 221);
            } catch (Exception ignored) {}

            System.out.println("[EmailService] Email envoyé avec succès via SMTP à " + destinataire + " (« " + sujet + " »)");
        } finally {
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

    private void ecrireLigne(BufferedWriter writer, String ligne) throws IOException {
        writer.write(ligne);
        writer.write("\r\n");
        writer.flush();
    }

    private String lireReponse(BufferedReader reader, int codeAttendu) throws IOException {
        String ligne;
        StringBuilder reponse = new StringBuilder();
        int dernierCode = -1;
        while ((ligne = reader.readLine()) != null) {
            reponse.append(ligne).append("\n");
            if (ligne.length() >= 3) {
                try {
                    dernierCode = Integer.parseInt(ligne.substring(0, 3));
                } catch (NumberFormatException ignored) {}
                if (ligne.length() == 3 || ligne.charAt(3) == ' ') {
                    break;
                }
            }
        }
        if (dernierCode != codeAttendu) {
            throw new IOException("Réponse SMTP inattendue : attendu " + codeAttendu + " mais reçu " + dernierCode + " : " + reponse.toString().trim());
        }
        return reponse.toString();
    }
}
