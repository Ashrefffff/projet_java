package controller;

import app.AppState;
import model.*;
import ui.Dialogs;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;


public final class TransactionsController {

    @FXML private TableView<Transaction> table;
    @FXML private TableColumn<Transaction, java.time.LocalDate> colDate;
    @FXML private TableColumn<Transaction, TransactionType> colType;
    @FXML private TableColumn<Transaction, String> colSymbol;
    @FXML private TableColumn<Transaction, String> colAssetType;
    @FXML private TableColumn<Transaction, Double> colQty;
    @FXML private TableColumn<Transaction, Double> colPrice;
    private AppState state;

    public void init(AppState state) {
        this.state = state;
        colDate.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().getDate()));
        colType.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().getType()));
        colSymbol.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().getAsset().getSymbole()));
        colAssetType.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().getAsset().getAssetType()));
        colQty.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().getQuantite()));
        colPrice.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().getPrixUnitaire()));
        state.selectedPortfolioProperty().addListener((obs, o, n) -> refresh());
        state.addDataListener(this::refresh);
        refresh();
    }

    private void refresh() {
        Portfolio p = state.selectedPortfolio();
        if (p == null) {
            table.setItems(javafx.collections.FXCollections.emptyObservableList());
        } else {
            table.setItems(javafx.collections.FXCollections.observableArrayList(p.getTransactions()));
        }
    }

    @FXML private void onAdd() {
        Portfolio p = state.selectedPortfolio();
        if (p == null) return;
        Transaction tx = Dialogs.transactionDialog(state.getManager(), null);
        if (tx == null) return;
        try {
            p.addTransactionChecked(tx);
            refresh();
            state.dataChanged();
            state.setSelectedPortfolio(state.selectedPortfolio());
        } catch (Exception ex) {
            Dialogs.error("Transaction invalide", ex.getMessage());
        }
    }

    @FXML private void onEdit() {
        Portfolio p = state.selectedPortfolio();
        Transaction selected = table.getSelectionModel().getSelectedItem();
        if (p == null || selected == null) return;
        Transaction updated = Dialogs.transactionDialog(state.getManager(), selected);
        if (updated == null) return;
        p.getTransactions().remove(selected);
        try {
            p.addTransactionChecked(updated);
            refresh();
            state.dataChanged();
        } catch (Exception ex) {
            p.getTransactions().add(selected);
            p.getTransactions().sort(java.util.Comparator.comparing(Transaction::getDate));
            Dialogs.error("Modification invalide", ex.getMessage());
        }
    }

    @FXML private void onDelete() {
        Portfolio p = state.selectedPortfolio();
        Transaction selected = table.getSelectionModel().getSelectedItem();
        if (p == null || selected == null) return;
        if (!Dialogs.confirm("Supprimer", "Supprimer la transaction ?")) return;
        p.getTransactions().remove(selected);
        refresh();
        state.dataChanged();
    }
}
