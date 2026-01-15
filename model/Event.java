package model;

import java.time.LocalDate;

public class Event {
    private String titre;
    private LocalDate date;
    private String description;
    private String portfolioNom;

    public Event() {}
    public Event(String titre, LocalDate date, String description, String portfolioNom) {
        this.titre = titre;
        this.date = date;
        this.description = description;
        this.portfolioNom = portfolioNom;
    }

    public String getTitre() {
        return titre;
    }
    public LocalDate getDate() {
        return date;
    }
    public String getDescription() {
        return description;
    }
    public String getPortfolioNom() {
        return portfolioNom;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }
    public void setDate(LocalDate date) {
        this.date = date;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setPortfolioNom(String portfolioNom) {
        this.portfolioNom = portfolioNom;
    }
}
