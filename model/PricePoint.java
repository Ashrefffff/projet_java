package model;

import java.time.LocalDate;

public class PricePoint {
    private LocalDate date;
    private double prix;

    public PricePoint() {}
    public PricePoint(LocalDate date, double prix) {
        this.date = date;
        this.prix = prix;
    }

    public LocalDate getDate() {
        return date;
    }
    public double getPrix() {
        return prix;
    }
    public void setDate(LocalDate date) {
        this.date = date;
    }
    public void setPrix(double prix) {
        this.prix = prix;
    }
}
