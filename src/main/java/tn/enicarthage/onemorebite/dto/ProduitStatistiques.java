package tn.enicarthage.onemorebite.dto;

public record ProduitStatistiques(
        long totalProduits,
        long produitsDisponibles,
        long produitsIndisponibles,
        long produitsEnRupture,
        long quantiteTotale,
        double valeurStock
) {}
