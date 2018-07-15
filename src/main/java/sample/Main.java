package sample;

import javafx.application.Application;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import sample.DDragon.Champion;
import sample.api.APIWrapper;
import sample.imgcap.WindowScraper;

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
		APIWrapper apiWrapper = new APIWrapper();

		WindowScraper windowScraper = new WindowScraper();
		String approxChampName = windowScraper.update();
		if(!approxChampName.equals("")) {
			logger.info("Name scraped from screenshot: " + approxChampName);
			Champion champion = null;
			if(apiWrapper.updateData()) {
				champion = apiWrapper.getChampionBySkinName(approxChampName);
				if(champion == null) {
					logger.info("Cannot find champion with name: " + approxChampName);
					int minLevenshteinDistance = Integer.MAX_VALUE;
					String closestChampionName = null;
					for(String champName : apiWrapper.getSkinIdMap().keySet()) {
						int currLevenshteinDistance = StringUtils.getLevenshteinDistance(champName, approxChampName);
						if(currLevenshteinDistance < minLevenshteinDistance) {
							minLevenshteinDistance = currLevenshteinDistance;
							closestChampionName = champName;
						}
					}
					logger.info("Closest match to " + approxChampName + " is " + closestChampionName);
					champion = apiWrapper.getChampionBySkinName(closestChampionName);
				} else {
					champion = apiWrapper.getChampionBySkinName(approxChampName);
				}
			}
			logger.info("Found champion info: " + champion);
		} else {
			logger.error("No selected champion found.");
		}
	}
}
