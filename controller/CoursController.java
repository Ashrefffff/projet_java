package controller;

import app.AppState;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.util.Duration;
import model.*;
import ui.Dialogs;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public final class CoursController {

    @FXML private TableView<CryptoRow> table;
    @FXML private TableColumn<CryptoRow, String> colSymbol;
    @FXML private TableColumn<CryptoRow, String> colName;
    @FXML private TableColumn<CryptoRow, Double> colPrice;
    @FXML private LineChart<Number, Number> priceChart;
    @FXML private Label selectedLabel;
    @FXML private Label lastUpdateLabel;
    @FXML private CheckBox autoCheck;
    @FXML private TableView<MarketEvent> eventsTable;
    @FXML private TableColumn<MarketEvent, String> colEvtTime;
    @FXML private TableColumn<MarketEvent, String> colEvtType;
    @FXML private TableColumn<MarketEvent, String> colEvtSymbol;
    @FXML private TableColumn<MarketEvent, String> colEvtImpact;
    @FXML private TableColumn<MarketEvent, String> colEvtMsg;

    private AppState state;
    private final ObservableList<CryptoRow> rows = FXCollections.observableArrayList();
    private final Map<String, Deque<Double>> seriesBySymbol = new HashMap<>();
    private final Random rng = new Random();
    private final ObservableList<MarketEvent> events = FXCollections.observableArrayList();
    private Timeline autoTimeline;

    public void init(AppState state) {
        this.state = state;
        if (table == null || colSymbol == null || colName == null || colPrice == null ||
                priceChart == null || selectedLabel == null || autoCheck == null ||
                eventsTable == null || colEvtTime == null || colEvtType == null || colEvtSymbol == null ||
                colEvtImpact == null || colEvtMsg == null) {
            throw new IllegalStateException("cours.fxml: fx:id manquant (table/cols/chart/labels/eventsTable/cols). Vérifie les fx:id.");
        }
        colSymbol.setCellValueFactory(v -> v.getValue().symbolProperty());
        colName.setCellValueFactory(v -> v.getValue().nameProperty());
        colPrice.setCellValueFactory(v -> v.getValue().priceProperty().asObject());
        table.setItems(rows);
        colEvtTime.setCellValueFactory(v -> new ReadOnlyStringWrapper(v.getValue().time()));
        colEvtType.setCellValueFactory(v -> new ReadOnlyStringWrapper(v.getValue().type()));
        colEvtSymbol.setCellValueFactory(v -> new ReadOnlyStringWrapper(v.getValue().symbol()));
        colEvtImpact.setCellValueFactory(v -> new ReadOnlyStringWrapper(v.getValue().impact()));
        colEvtMsg.setCellValueFactory(v -> new ReadOnlyStringWrapper(v.getValue().message()));
        eventsTable.setItems(events);
        table.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) renderChart(n.symbol());
        });
        autoCheck.selectedProperty().addListener((obs, old, on) -> setAuto(on));
        initMarket();
        refreshMarket();
        if (!rows.isEmpty()) table.getSelectionModel().select(0);
    }

    @FXML private void onRefresh() {
        refreshMarket();
    }

    @FXML private void onCrash() {
        CryptoRow sel = table.getSelectionModel().getSelectedItem();
        if (sel != null) {
            applyShock(sel.symbol(), -randBetween(0.10, 0.35), "Flash crash / liquidation");
        } else {
            applyShockAll(-randBetween(0.08, 0.22), "Market crash global");
        }
        refreshMarket();
    }

    @FXML private void onPump() {
        CryptoRow sel = table.getSelectionModel().getSelectedItem();
        if (sel != null) {
            applyShock(sel.symbol(), +randBetween(0.08, 0.25), "Pump / news positive");
        } else {
            applyShockAll(+randBetween(0.05, 0.15), "Market rally global");
        }
        refreshMarket();
    }

    @FXML private void onBuy() { trade(TransactionType.ACHAT); }
    @FXML private void onSell() { trade(TransactionType.VENTE); }

    private void trade(TransactionType type) {
        Portfolio p = state.selectedPortfolio();
        if (p == null) {
            Dialogs.error("Aucun portfolio", "Sélectionne un portfolio avant de trader.");
            return;
        }
        CryptoRow row = table.getSelectionModel().getSelectedItem();
        if (row == null) {
            Dialogs.error("Aucune crypto", "Sélectionne une crypto dans la table.");
            return;
        }
        Optional<Double> qtyOpt = Dialogs.askPositiveDouble(
                (type == TransactionType.ACHAT ? "Acheter " : "Vendre ") + row.symbol(),
                "Quantité",
                "Ex: 0.5, 1, 10"
        );
        if (qtyOpt.isEmpty()) return;
        double qty = qtyOpt.get();
        Asset existing = findAssetInPortfolio(p, row.symbol());
        CryptoToken asset = (existing instanceof CryptoToken)
                ? (CryptoToken) existing
                : new CryptoToken(row.symbol(), row.name());
        double price = row.price();
        asset.upsertPrice(new PricePoint(LocalDate.now(), price));
        Transaction tx = new Transaction(type, LocalDate.now(), qty, price, asset);
        try {
            p.addTransactionChecked(tx);
            state.dataChanged();
        } catch (Exception ex) {
            Dialogs.error("Trade refusé", ex.getMessage());
        }
    }

    private Asset findAssetInPortfolio(Portfolio p, String symbol) {
        for (Transaction t : p.getTransactions()) {
            if (t.getAsset() != null && symbol.equalsIgnoreCase(t.getAsset().getSymbole())) {
                return t.getAsset();
            }
        }
        return null;
    }

    private void initMarket() {
        addCrypto("BTC", "Bitcoin", 42000);
        addCrypto("ETH", "Ethereum", 2300);
        addCrypto("SOL", "Solana", 105);
        addCrypto("BNB", "BNB", 335);
        addCrypto("XRP", "XRP", 0.62);
        addCrypto("ADA", "Cardano", 0.45);
    }

    private void addCrypto(String symbol, String name, double startPrice) {
        rows.add(new CryptoRow(symbol, name, startPrice));
        Deque<Double> s = new ArrayDeque<>();
        double p = startPrice;
        for (int i = 0; i < 50; i++) {
            p = nextPrice(p);
            s.addLast(p);
        }
        seriesBySymbol.put(symbol, s);
    }

    private double nextPrice(double prev) {
        double pct = (rng.nextDouble() - 0.5) * 0.02; // +-1%
        double next = prev * (1.0 + pct);
        if (next < 0.0001) next = 0.0001;
        return next;
    }

    private void refreshMarket() {
        for (CryptoRow r : rows) {
            Deque<Double> s = seriesBySymbol.get(r.symbol());
            double last = (s == null || s.isEmpty()) ? r.price() : s.peekLast();
            double next = nextPrice(last);
            if (s != null) {
                s.addLast(next);
                while (s.size() > 60) s.removeFirst();
            }
            r.setPrice(round2(next));
        }
        if (lastUpdateLabel != null) {
            lastUpdateLabel.setText("Dernière maj: " + LocalTime.now().withNano(0));
        }
        CryptoRow sel = table.getSelectionModel().getSelectedItem();
        if (sel != null) renderChart(sel.symbol());
    }

    private void renderChart(String symbol) {
        Deque<Double> s = seriesBySymbol.get(symbol);
        priceChart.getData().clear();
        if (s == null || s.isEmpty()) {
            selectedLabel.setText("Crypto: -");
            return;
        }
        selectedLabel.setText("Crypto: " + symbol);
        XYChart.Series<Number, Number> serie = new XYChart.Series<>();
        int i = 0;
        for (double v : s) {
            serie.getData().add(new XYChart.Data<>(i++, v));
        }
        serie.setName(symbol);
        priceChart.getData().add(serie);
    }

    private void setAuto(boolean on) {
        if (on) {
            if (autoTimeline == null) {
                autoTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                    if (rng.nextDouble() < 0.06) {
                        CryptoRow sel = table.getSelectionModel().getSelectedItem();
                        if (sel != null) {
                            if (rng.nextBoolean()) onCrash(); else onPump();
                        }
                    } else {
                        refreshMarket();
                    }
                }));
                autoTimeline.setCycleCount(Timeline.INDEFINITE);
            }
            autoTimeline.playFromStart();
        } else {
            if (autoTimeline != null) autoTimeline.stop();
        }
    }

    private void applyShock(String symbol, double pct, String reason) {
        Deque<Double> s = seriesBySymbol.get(symbol);
        if (s == null || s.isEmpty()) return;
        double last = s.peekLast();
        double shocked = last * (1.0 + pct);
        if (shocked < 0.0001) shocked = 0.0001;
        s.addLast(shocked);
        while (s.size() > 60) s.removeFirst();
        CryptoRow row = rows.stream().filter(r -> r.symbol().equals(symbol)).findFirst().orElse(null);
        if (row != null) row.setPrice(round2(shocked));
        String type = (pct < 0) ? "CRASH" : "UP";
        events.add(0, new MarketEvent(
                LocalTime.now().withNano(0).toString(),
                type,
                symbol,
                formatPct(pct),
                reason
        ));
        trimEvents();
    }

    private void applyShockAll(double pct, String reason) {
        for (CryptoRow r : rows) applyShock(r.symbol(), pct, reason);
    }

    private void trimEvents() {
        while (events.size() > 30) events.remove(events.size() - 1);
    }

    private static String formatPct(double pct) {
        double v = pct * 100.0;
        String sign = (v >= 0) ? "+" : "";
        return sign + String.format(Locale.US, "%.1f%%", v);
    }

    private double randBetween(double a, double b) {
        return a + (b - a) * rng.nextDouble();
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    public static final class CryptoRow {
        private final StringProperty symbol = new SimpleStringProperty();
        private final StringProperty name = new SimpleStringProperty();
        private final DoubleProperty price = new SimpleDoubleProperty();

        public CryptoRow(String symbol, String name, double price) {
            this.symbol.set(symbol);
            this.name.set(name);
            this.price.set(price);
        }

        public String symbol() {
            return symbol.get();
        }

        public StringProperty symbolProperty() {
            return symbol;
        }

        public String name() {
            return name.get();
        }

        public StringProperty nameProperty() {
            return name;
        }

        public double price() {
            return price.get();
        }

        public DoubleProperty priceProperty() {
            return price;
        }

        public void setPrice(double p) {
            price.set(p);
        }
    }

    public static final class MarketEvent {
        private final String time;
        private final String type;
        private final String symbol;
        private final String impact;
        private final String message;

        public MarketEvent(String time, String type, String symbol, String impact, String message) {
            this.time = time;
            this.type = type;
            this.symbol = symbol;
            this.impact = impact;
            this.message = message;
        }

        public String time() {
            return time;
        }

        public String type() {
            return type;
        }

        public String symbol() {
            return symbol;
        }

        public String impact() {
            return impact;
        }

        public String message() {
            return message;
        }
    }
}
