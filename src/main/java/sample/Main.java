package sample;

import javafx.application.Application;
import javafx.stage.Stage;
import sample.DDragon.Champion;
import sample.imgcap.WindowScraper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	    Map<Integer, Champion> idChampionMap = new HashMap<>();
	    Map<String, Integer> skinIdMap = new HashMap<>();
	    apiWrapper.getChampSkinList(idChampionMap, skinIdMap);
	    apiWrapper.getCGGRunes(idChampionMap);

//	    for(Integer id : idChampionMap.keySet()) {
//			System.out.println(idChampionMap.get(id));
//		}

		int champId = skinIdMap.get("PROJECT: Zed");
		Champion champ = idChampionMap.get(champId);

		System.out.println(champ);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
