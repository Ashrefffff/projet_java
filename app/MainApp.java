package app;

import controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import persistence.JsonStore;
import java.nio.file.Path;

public final class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        JsonStore store = new JsonStore();
        Path savePath = Path.of("portfolio-data.json");
        AppState state = new AppState(store, savePath);
        state.load();
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/view/main.fxml"));
        Scene scene = new Scene(loader.load(), 1150, 720);
        scene.getStylesheets().add(MainApp.class.getResource("/view/styles.css").toExternalForm());
        MainController controller = loader.getController();
        controller.init(state);
        stage.setTitle("Portfolio Manager (JavaFX)");
        stage.setScene(scene);
        stage.show();
    }
    public static void main(String[] args) {
        launch(args);
    }
}
