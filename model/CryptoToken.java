package model;

public class CryptoToken extends Asset {
    public CryptoToken() {
    }
    public CryptoToken(String symbole, String nom) {
        super(symbole, nom);
    }
    @Override public String getAssetType() {
        return "CryptoToken";
    }
}
