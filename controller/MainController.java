package controller;

import app.AppState;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.util.StringConverter;
import model.Portfolio;
import service.PortfolioAnalytics;
import ui.Dialogs;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import EventsController.EventsController;

public final class MainController {

    @FXML private ListView<Portfolio> portfolioList;
    @FXML private Label summaryLabel;
    @FXML private LineChart<Number, Number> lineChart;
    @FXML private PieChart pieChart;
    @FXML private BorderPane transactionsHost;
    @FXML private BorderPane assetsHost;
    @FXML private BorderPane eventsHost;
    @FXML private BorderPane coursHost;
    private AppState state;

    private static final DateTimeFormatter CHART_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM");

    public void init(AppState state) {
        this.state = state;
        portfolioList.setItems(state.portfolios());
        portfolioList.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, cur) -> state.setSelectedPortfolio(cur)
        );
        if (!state.portfolios().isEmpty() && portfolioList.getSelectionModel().getSelectedItem() == null) {
            portfolioList.getSelectionModel().select(0);
        }
        configureCharts();
        loadSubViews();
        state.addDataListener(this::refreshAll);
        state.selectedPortfolioProperty().addListener((obs, o, n) -> refreshAll());
        refreshAll();
    }

    private void configureCharts() {
        if (lineChart.getXAxis() instanceof NumberAxis xAxis) {
            xAxis.setTickLabelFormatter(new StringConverter<>() {
                @Override public String toString(Number n) {
                    if (n == null) return "";
                    return LocalDate.ofEpochDay(n.longValue()).format(CHART_DATE_FMT);
                }
                @Override public Number fromString(String s) { return 0; }
            });
        }
        lineChart.setAnimated(false);
        lineChart.setCreateSymbols(false);
        pieChart.setLabelsVisible(true);
    }

    private void loadSubViews() {
        loadInto("/view/transactions.fxml", transactionsHost);
        loadInto("/view/assets.fxml", assetsHost);
        loadInto("/view/events.fxml", eventsHost);
        if (coursHost != null) {
            loadInto("/view/cours.fxml", coursHost);
        }
    }

    private void loadInto(String fxml, BorderPane host) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Object c = loader.getController();
            if (c instanceof TransactionsController tc) {
                tc.init(state);
            } else if (c instanceof AssetsController ac) {
                ac.init(state);
            } else if (c instanceof EventsController ec) {
                ec.init(state);
            } else if (c instanceof CoursController cc) {
                cc.init(state);
            }
            host.setCenter(root);
        } catch (Exception e) {
            e.printStackTrace();
            host.setCenter(new Label("Erreur chargement: " + fxml));
        }
    }

    @FXML private void onCreatePortfolio() {
        Portfolio p = Dialogs.createPortfolioDialog();
        if (p == null) return;
        state.getManager().getPortefeuilles().add(p);
        state.portfolios().add(p);
        portfolioList.getSelectionModel().select(p);
        state.dataChanged();
    }

    @FXML private void onClonePortfolio() {
        Portfolio src = state.selectedPortfolio();
        if (src == null) return;
        Portfolio copy = new Portfolio(src.getNom() + "_clone", src.getDescription());
        copy.setCash(src.getCash());
        copy.getTransactions().addAll(src.getTransactions());
        state.getManager().getPortefeuilles().add(copy);
        state.portfolios().add(copy);
        portfolioList.getSelectionModel().select(copy);
        state.dataChanged();
    }

    @FXML private void onDeletePortfolio() {
        Portfolio p = state.selectedPortfolio();
        if (p == null) return;
        if (!Dialogs.confirm("Supprimer", "Supprimer le portfolio '" + p.getNom() + "' ?")) return;
        state.getManager().getPortefeuilles().remove(p);
        state.portfolios().remove(p);
        if (!state.portfolios().isEmpty()) portfolioList.getSelectionModel().select(0);
        state.dataChanged();
    }

    @FXML private void onSave() {
        boolean ok = state.save();
        if (ok) {
            Dialogs.info("Sauvegarde", "Données sauvegardées avec succès.");
        } else {
            Dialogs.error("Sauvegarde", "Échec de la sauvegarde. Consulte la console pour le détail.");
        }
    }

    private void refreshAll() {
        refreshSummaryAndCharts();
    }

    private void refreshSummaryAndCharts() {
        Portfolio p = state.selectedPortfolio();
        if (p == null) {
            summaryLabel.setText("Aucun portfolio sélectionné.");
            lineChart.getData().clear();
            pieChart.setData(javafx.collections.FXCollections.emptyObservableList());
            return;
        }
        LocalDate today = LocalDate.now();
        double invested = p.investedCost();
        double assetsValue = PortfolioAnalytics.valueAt(p, today);
        double pnl = assetsValue - invested;
        String sym = state.getManager().getDeviseReference().symbol();
        double netWorth = p.getCash() + assetsValue;
        summaryLabel.setText(
                "Nom: " + p.getNom() + "\n" +
                        "Cash: " + p.getCash() + " " + sym + "\n" +
                        "Investi (ACHAT): " + invested + " " + sym + "\n" +
                        "Valeur (aujourd'hui): " + assetsValue + " " + sym + "\n" +
                        "Net Worth: " + netWorth + " " + sym + "\n" +
                        "P/L: " + pnl + " " + sym
        );
        lineChart.getData().clear();
        XYChart.Series<Number, Number> s = new XYChart.Series<>();
        var series = PortfolioAnalytics.valueSeries(p, today.minusMonths(6), today);
        for (var e : series.entrySet()) {
            long x = e.getKey().toEpochDay();
            double y = e.getValue();
            s.getData().add(new XYChart.Data<>(x, y));
        }
        s.setName("Valeur des assets");
        lineChart.getData().add(s);
        Map<String, Double> alloc = PortfolioAnalytics.allocationByType(p, today);
        var pie = javafx.collections.FXCollections.<PieChart.Data>observableArrayList();
        alloc.forEach((k, v) -> {
            if (v > 0.0) pie.add(new PieChart.Data(k, v));
        });
        if (p.getCash() > 0.0) {
            pie.add(new PieChart.Data("Cash", p.getCash()));
        }
        pieChart.setData(pie);
    }
}
