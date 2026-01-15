package model;

import java.time.LocalDate;
import java.util.*;

public final class Portfolio {

    private String nom;
    private String description;
    private double cash = 10000.0;
    private final List<Transaction> transactions = new ArrayList<>();

    public Portfolio() {
    }

    public Portfolio(String nom, String description) {
        this.nom = nom;
        this.description = description;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Transaction> getTransactions() { return transactions; }

    public double getCash() {
        return cash;
    }

    public void setCash(double cash) {
        this.cash = cash;
    }

    public double investedCost() {
        double sum = 0.0;
        for (Transaction t : transactions) {
            if (t.getType() == TransactionType.ACHAT) {
                sum += t.getQuantite() * t.getPrixUnitaire();
            }
        }
        return sum;
    }

    public Map<String, Double> holdingsAt(LocalDate date) {
        Map<String, Double> h = new HashMap<>();
        for (Transaction t : transactions) {
            if (t.getDate() == null || t.getAsset() == null) continue;
            if (date != null && t.getDate().isAfter(date)) continue;
            String sym = t.getAsset().getSymbole();
            double q = t.getQuantite();
            if (t.getType() == TransactionType.ACHAT) {
                h.merge(sym, q, Double::sum);
            } else if (t.getType() == TransactionType.VENTE) {
                h.merge(sym, -q, Double::sum);
            }
        }
        h.entrySet().removeIf(e -> e.getValue() <= 0.0);
        return h;
    }

    public void addTransactionChecked(Transaction tx) {
        if (tx == null) throw new IllegalArgumentException("Transaction null");
        if (tx.getAsset() == null) throw new IllegalArgumentException("Asset manquant");
        if (tx.getDate() == null) throw new IllegalArgumentException("Date manquante");
        if (tx.getQuantite() <= 0) throw new IllegalArgumentException("Quantité invalide");
        if (tx.getPrixUnitaire() <= 0) throw new IllegalArgumentException("Prix unitaire invalide");
        if (tx.getType() == null) throw new IllegalArgumentException("Type manquant");
        double amount = tx.getQuantite() * tx.getPrixUnitaire();
        if (tx.getType() == TransactionType.ACHAT) {
            if (cash < amount) {
                throw new IllegalArgumentException(
                        "Cash insuffisant. Solde: " + cash + " - Besoin: " + amount
                );
            }
            cash -= amount;
        } else if (tx.getType() == TransactionType.VENTE) {
            Map<String, Double> holdings = holdingsAt(tx.getDate());
            double held = holdings.getOrDefault(tx.getAsset().getSymbole(), 0.0);
            if (held < tx.getQuantite()) {
                throw new IllegalArgumentException(
                        "Vente impossible: détenu " + held + " < " + tx.getQuantite()
                );
            }
            cash += amount;
        }
        transactions.add(tx);
        transactions.sort(Comparator.comparing(Transaction::getDate));
    }

    @Override
    public String toString() {
        return nom == null ? "(portfolio)" : nom;
    }

}
