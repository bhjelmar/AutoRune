package sample;

import javafx.application.Application;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import sample.api.APIWrapper;
import sample.data.Champion;
import sample.data.RunePage;
import sample.imgcap.WindowScraper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Main extends Application {

	private static final Logger logger = Logger.getLogger(Main.class);

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
		apiWrapper.updateData();
		while(true) {
			Thread.sleep(1000);
			WindowScraper windowScraper = new WindowScraper();
			String approxChampName = windowScraper.update();
			if(!approxChampName.equals("")) {
				logger.info("Name scraped from screenshot: " + approxChampName);
				Champion champion = apiWrapper.getChampionBySkinName(approxChampName);
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

				logger.info("Found champion info: " + champion);


				List<RunePage> runePageList = apiWrapper.getPages();
				RunePage apiPage = runePageList.stream().filter(o -> o.getName().equals("AutoRune")).findFirst().orElse(null);
				if(apiPage != null) {
					Set<String> roles = champion.getHighestWinRoleRuneMap().keySet();
					String role = roles.iterator().next();

					List<String> runes = champion.getHighestWinRoleRuneMap().get(role);
					apiPage.setPrimaryStyleId(Integer.parseInt(runes.get(0)));
					List<Integer> selectedPerkIds = new ArrayList<>();
					selectedPerkIds.add(Integer.parseInt(runes.get(1)));
					selectedPerkIds.add(Integer.parseInt(runes.get(2)));
					selectedPerkIds.add(Integer.parseInt(runes.get(3)));
					selectedPerkIds.add(Integer.parseInt(runes.get(4)));
					selectedPerkIds.add(Integer.parseInt(runes.get(5)));
					apiPage.setSubStyleId(Integer.parseInt(runes.get(6)));

					System.out.println(champion);
					System.out.println(apiPage);

				}
//				return;
			} else {
//				logger.info("No selected champion found.");
			}
		}
	}
}
