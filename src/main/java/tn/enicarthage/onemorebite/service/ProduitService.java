package tn.enicarthage.onemorebite.service;

import tn.enicarthage.onemorebite.dao.CommercantDAO;
import tn.enicarthage.onemorebite.dao.ProduitDAO;
import tn.enicarthage.onemorebite.dto.ProduitRequest;
import tn.enicarthage.onemorebite.dto.ProduitStatistiques;
import tn.enicarthage.onemorebite.exception.AuthenticationException;
import tn.enicarthage.onemorebite.exception.ForbiddenException;
import tn.enicarthage.onemorebite.exception.ResourceNotFoundException;
import tn.enicarthage.onemorebite.model.Commercant;
import tn.enicarthage.onemorebite.model.Produit;
import tn.enicarthage.onemorebite.security.JwtUtil;

import java.util.List;

public class ProduitService {

    private static final int LONGUEUR_MAX_IMAGE_URL = 5000;

    private final ProduitDAO produitDAO = new ProduitDAO();
    private final CommercantDAO commercantDAO = new CommercantDAO();

    public List<Produit> lister(Integer commercantId, Boolean disponibilite, String recherche) {
        return produitDAO.listerAvecFiltres(commercantId, disponibilite, recherche);
    }

    public List<Produit> listerPourCommercant(int commercantId) {
        return produitDAO.listerParCommercant(commercantId);
    }

    public List<Produit> listerMesProduits(JwtUtil.JwtClaims claims) {
        JwtUtil.JwtClaims utilisateur = exigerRoleGestion(claims);
        if (!estAdmin(utilisateur)) {
            return produitDAO.listerParCommercant(utilisateur.id());
        }
        // Un administrateur peut consulter tous les produits via /admin/produits
        return produitDAO.listerTous();
    }

    public Produit obtenir(int id) {
        return produitDAO.trouverParId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé avec id: " + id));
    }

    public Produit creer(ProduitRequest req, JwtUtil.JwtClaims claims) {
        JwtUtil.JwtClaims utilisateur = exigerRoleGestion(claims);
        if (req == null) {
            throw new IllegalArgumentException("La requête de création du produit est obligatoire.");
        }

        int commercantId = resoudreCommercantPourCreation(req, utilisateur);
        verifierCommercantActif(commercantId);

        String nom = req.nom();
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom du produit est obligatoire.");
        }
        if (req.prix() == null) {
            throw new IllegalArgumentException("Le prix du produit est obligatoire.");
        }
        if (req.prix() <= 0) {
            throw new IllegalArgumentException("Le prix du produit doit être strictement positif.");
        }

        int quantite = req.quantite() != null ? req.quantite() : 0;
        if (quantite < 0) {
            throw new IllegalArgumentException("La quantité ne peut pas être négative.");
        }

        Produit p = new Produit();
        p.setNom(nom.trim());
        p.setDescription(req.description());
        p.setPrix(req.prix());
        p.setQuantite(quantite);
        p.setImageUrl(resoudreImages(req.imageUrl(), req.images()));
        p.setDateExpiration(req.dateExpiration());
        p.setDisponibilite(req.disponibilite() != null ? req.disponibilite() : true);
        p.setCommercantId(commercantId);

        Produit cree = produitDAO.creer(p);
        return produitDAO.trouverParId(cree.getId()).orElse(cree);
    }

    public Produit modifier(int id, ProduitRequest req, JwtUtil.JwtClaims claims) {
        JwtUtil.JwtClaims utilisateur = exigerRoleGestion(claims);
        if (req == null) {
            throw new IllegalArgumentException("La requête de modification du produit est obligatoire.");
        }

        Produit p = obtenir(id);
        verifierProprietaire(p, utilisateur);

        if (req.nom() != null) {
            if (req.nom().isBlank()) {
                throw new IllegalArgumentException("Le nom du produit ne peut pas être vide.");
            }
            p.setNom(req.nom().trim());
        }
        if (req.description() != null) {
            p.setDescription(req.description());
        }
        if (req.prix() != null) {
            if (req.prix() <= 0) {
                throw new IllegalArgumentException("Le prix du produit doit être strictement positif.");
            }
            p.setPrix(req.prix());
        }
        if (req.quantite() != null) {
            if (req.quantite() < 0) {
                throw new IllegalArgumentException("La quantité ne peut pas être négative.");
            }
            p.setQuantite(req.quantite());
        }
        if (req.images() != null || req.imageUrl() != null) {
            p.setImageUrl(resoudreImages(req.imageUrl(), req.images()));
        }
        if (req.dateExpiration() != null) {
            p.setDateExpiration(req.dateExpiration());
        }
        if (req.disponibilite() != null) {
            p.setDisponibilite(req.disponibilite());
        }

        // Seul un administrateur peut réaffecter un produit à un autre commerçant.
        if (estAdmin(utilisateur) && req.commercantId() != null) {
            verifierCommercantActif(req.commercantId());
            p.setCommercantId(req.commercantId());
        }

        if (!produitDAO.modifier(p)) {
            throw new ResourceNotFoundException("Produit non trouvé avec id: " + id);
        }
        return produitDAO.trouverParId(id).orElse(p);
    }

    public Produit changerDisponibilite(int id, Boolean disponibilite, JwtUtil.JwtClaims claims) {
        JwtUtil.JwtClaims utilisateur = exigerRoleGestion(claims);
        if (disponibilite == null) {
            throw new IllegalArgumentException("Le champ 'disponibilite' est obligatoire.");
        }

        Produit p = obtenir(id);
        verifierProprietaire(p, utilisateur);

        if (!produitDAO.modifierDisponibilite(id, disponibilite)) {
            throw new ResourceNotFoundException("Produit non trouvé avec id: " + id);
        }
        p.setDisponibilite(disponibilite);
        return produitDAO.trouverParId(id).orElse(p);
    }

    public Produit changerStock(int id, Integer quantite, JwtUtil.JwtClaims claims) {
        JwtUtil.JwtClaims utilisateur = exigerRoleGestion(claims);
        if (quantite == null) {
            throw new IllegalArgumentException("Le champ 'quantite' est obligatoire.");
        }
        if (quantite < 0) {
            throw new IllegalArgumentException("La quantité ne peut pas être négative.");
        }

        Produit p = obtenir(id);
        verifierProprietaire(p, utilisateur);

        if (!produitDAO.modifierStock(id, quantite)) {
            throw new ResourceNotFoundException("Produit non trouvé avec id: " + id);
        }
        p.setQuantite(quantite);
        return produitDAO.trouverParId(id).orElse(p);
    }

    public void supprimer(int id, JwtUtil.JwtClaims claims) {
        JwtUtil.JwtClaims utilisateur = exigerRoleGestion(claims);
        Produit p = obtenir(id);
        verifierProprietaire(p, utilisateur);

        if (!produitDAO.supprimer(id)) {
            throw new ResourceNotFoundException("Produit non trouvé avec id: " + id);
        }
    }

    public ProduitStatistiques statistiques() {
        return produitDAO.statistiques();
    }

    private int resoudreCommercantPourCreation(ProduitRequest req, JwtUtil.JwtClaims utilisateur) {
        if (estAdmin(utilisateur)) {
            if (req.commercantId() == null || req.commercantId() <= 0) {
                throw new IllegalArgumentException(
                        "Le champ 'commercantId' est obligatoire lorsqu'un administrateur crée un produit.");
            }
            return req.commercantId();
        }
        // Un commerçant crée toujours des produits pour son propre compte.
        return utilisateur.id();
    }

    private void verifierCommercantActif(int commercantId) {
        Commercant commercant = commercantDAO.trouverParId(commercantId)
                .orElseThrow(() -> new ResourceNotFoundException("Commerçant non trouvé avec l'id : " + commercantId));
        if (!"ACTIF".equalsIgnoreCase(commercant.getStatut())) {
            throw new IllegalArgumentException("Le compte commerçant " + commercantId + " n'est pas actif.");
        }
    }

    private void verifierProprietaire(Produit produit, JwtUtil.JwtClaims utilisateur) {
        if (!estAdmin(utilisateur) && produit.getCommercantId() != utilisateur.id()) {
            throw new ForbiddenException("Vous ne pouvez gérer que les produits de votre propre commerce.");
        }
    }

    private JwtUtil.JwtClaims exigerRoleGestion(JwtUtil.JwtClaims claims) {
        if (claims == null) {
            throw new AuthenticationException("Authentification requise.");
        }
        if (!estAdmin(claims) && !"COMMERCANT".equalsIgnoreCase(claims.role())) {
            throw new ForbiddenException("Seuls les commerçants et les administrateurs peuvent gérer les produits.");
        }
        return claims;
    }

    private boolean estAdmin(JwtUtil.JwtClaims claims) {
        return claims != null && "ADMIN".equalsIgnoreCase(claims.role());
    }

    private String resoudreImages(String imageUrl, java.util.List<String> images) {
        if (images != null && !images.isEmpty()) {
            java.util.List<String> validees = new java.util.ArrayList<>();
            for (String img : images) {
                if (img != null && !img.isBlank()) {
                    String norm = normaliserSingleUrl(img.trim());
                    if (norm != null) {
                        validees.add(norm);
                    }
                }
            }
            if (!validees.isEmpty()) {
                String joined = String.join(",", validees);
                if (joined.length() > LONGUEUR_MAX_IMAGE_URL) {
                    throw new IllegalArgumentException("Les URLs d'images combinées dépassent la taille maximale autorisée.");
                }
                return joined;
            }
        }
        return normaliserImageUrl(imageUrl);
    }

    private String normaliserSingleUrl(String url) {
        if (url == null || url.isBlank()) return null;
        String v = url.trim();
        if (!v.startsWith("http://") && !v.startsWith("https://") && !v.startsWith("data:image/")) {
            throw new IllegalArgumentException("L'URL de l'image doit commencer par http://, https:// ou data:image/ : " + v);
        }
        return v;
    }

    private String normaliserImageUrl(String imageUrl) {
        if (imageUrl == null) {
            return null;
        }
        String valeur = imageUrl.trim();
        if (valeur.isEmpty()) {
            return null;
        }
        if (valeur.contains(",")) {
            String[] parts = valeur.split(",");
            java.util.List<String> vList = new java.util.ArrayList<>();
            for (String p : parts) {
                String single = normaliserSingleUrl(p);
                if (single != null) vList.add(single);
            }
            return String.join(",", vList);
        }
        return normaliserSingleUrl(valeur);
    }
}
