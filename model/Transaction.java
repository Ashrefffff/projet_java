package model;

import java.time.LocalDate;

public class Transaction {
    private TransactionType type;
    private LocalDate date;
    private double quantite;
    private double prixUnitaire;
    private Asset asset;

    public Transaction() {}
    public Transaction(TransactionType type, LocalDate date, double quantite, double prixUnitaire, Asset asset) {
        this.type = type;
        this.date = date;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
        this.asset = asset;
    }

    public TransactionType getType() {
        return type;
    }
    public LocalDate getDate() {
        return date;
    }
    public double getQuantite() {
        return quantite;
    }
    public double getPrixUnitaire() {
        return prixUnitaire;
    }
    public Asset getAsset() {
        return asset;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }
    public void setDate(LocalDate date) {
        this.date = date;
    }
    public void setQuantite(double quantite) {
        this.quantite = quantite;
    }
    public void setPrixUnitaire(double prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }
    public void setAsset(Asset asset) {
        this.asset = asset;
    }

    public double signedQuantity() {
        return type == TransactionType.ACHAT ? quantite : -quantite;
    }

    public double grossAmount() {
        return quantite * prixUnitaire;
    }
}
