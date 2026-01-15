package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CryptoQuote {
    private final String symbol;
    private final String name;
    private final List<Double> history = new ArrayList<>();
    private double price;
    private double change24hPct;

    public CryptoQuote(String symbol, String name, double startPrice) {
        this.symbol = symbol;
        this.name = name;
        this.price = startPrice;
        history.add(startPrice);
    }

    public String getSymbol() {
        return symbol;
    }
    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public double getChange24hPct() {
        return change24hPct;
    }

    public List<Double> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public void push(double newPrice) {
        this.price = newPrice;
        history.add(newPrice);
        if (history.size() > 120) history.remove(0);
    }

    public void setChange24hPct(double v) {
        this.change24hPct = v;
    }

    public String priceString() {
        return String.format("%.2f", price);
    }

    public String changeString() {
        return String.format("%.2f%%", change24hPct);
    }
}
