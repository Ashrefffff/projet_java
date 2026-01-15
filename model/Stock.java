package model;

public class Stock extends Asset {
    public Stock() {}
    public Stock(String symbole, String nom) {
        super(symbole, nom);
    }
    @Override public String getAssetType() {
        return "Stock";
    }
}
