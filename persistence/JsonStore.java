package persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import model.Asset;
import model.PortfolioManager;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public final class JsonStore {
    private final Gson gson;
    public JsonStore() {
        gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(java.time.LocalDate.class, new LocalDateAdapter())
                .registerTypeAdapter(Asset.class, new AssetAdapter())
                .create();
    }
    public void save(PortfolioManager manager, Path path) throws Exception {
        String json = gson.toJson(manager);
        Files.writeString(path, json, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    public PortfolioManager load(Path path) throws Exception {
        if (!Files.exists(path)) return new PortfolioManager();
        String json = Files.readString(path, StandardCharsets.UTF_8);
        PortfolioManager m = gson.fromJson(json, PortfolioManager.class);
        return (m == null) ? new PortfolioManager() : m;
    }
}
