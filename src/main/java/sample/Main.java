package sample;

import javafx.application.Application;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import sample.DDragon.Champion;
import sample.api.APIWrapper;

import java.text.SimpleDateFormat;

public class Main extends Application {

	private static final Logger logger = Logger.getLogger(Main.class);

	private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("dd-MM-yyyy");

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
//        Parent root = FXMLLoader.load(getClass().getResource("/sample.fxml"));
//        primaryStage.setTitle("Hello World");
//        primaryStage.setScene(new Scene(root, 300, 275));
//        primaryStage.show();

//		WindowScraper windowScraper = new WindowScraper();
//		windowScraper.update();

		APIWrapper apiWrapper = new APIWrapper();
		apiWrapper.updateData();

		Champion champion = apiWrapper.getChampionBySkinName("Zed");

		System.out.println(champion);
	}
}
