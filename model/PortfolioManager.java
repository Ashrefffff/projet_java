package model;

import java.util.ArrayList;
import java.util.List;

public class PortfolioManager {
    private List<Portfolio> portefeuilles = new ArrayList<>();
    private List<Event> events = new ArrayList<>();
    private Currency deviseReference = Currency.EUR;

    public List<Portfolio> getPortefeuilles() {
        return portefeuilles;
    }
    public void setPortefeuilles(List<Portfolio> portefeuilles) {
        this.portefeuilles = portefeuilles;
    }

    public List<Event> getEvents() {
        return events;
    }
    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public Currency getDeviseReference() {
        return deviseReference;
    }
    public void setDeviseReference(Currency deviseReference) {
        this.deviseReference = deviseReference;
    }
}
