package sample;

import javafx.application.Application;
import javafx.stage.Stage;
import sample.imgcap.WindowScraper;

import java.text.SimpleDateFormat;

public class Main extends Application {

	private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("dd-MM-yyyy");



    @Override
    public void start(Stage primaryStage) throws Exception{
//        Parent root = FXMLLoader.load(getClass().getResource("/sample.fxml"));
//        primaryStage.setTitle("Hello World");
//        primaryStage.setScene(new Scene(root, 300, 275));
//        primaryStage.show();

		WindowScraper windowScraper = new WindowScraper();
		windowScraper.update();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
