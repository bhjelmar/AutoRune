package sample;

import javafx.application.Application;
import javafx.stage.Stage;
import sample.DDragon.Champion;
import sample.imgcap.WindowScraper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class Main extends Application {

	private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("dd-MM-yyyy");



    @Override
    public void start(Stage primaryStage) throws Exception{
//        Parent root = FXMLLoader.load(getClass().getResource("/sample.fxml"));
//        primaryStage.setTitle("Hello World");
//        primaryStage.setScene(new Scene(root, 300, 275));
//        primaryStage.show();

//		WindowScraper windowScraper = new WindowScraper();
//		windowScraper.update();

	    APIWrapper apiWrapper = new APIWrapper();
//	    List<Champion> championList = new ArrayList<>();
	    apiWrapper.getChampSkinList();

    }

    public static void main(String[] args) {
        launch(args);
    }
}
