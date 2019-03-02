package com.bhjelmar;

import com.bhjelmar.api.APIWrapper;
import com.bhjelmar.data.Champion;
import com.bhjelmar.data.RunePage;
import com.bhjelmar.data.RuneSelection;
import com.bhjelmar.imgcap.WindowScraper;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Data;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Log4j2
public class Main extends Application {

	@Getter
	private static Map<String, List<RuneSelection>> runesMap;
	@Getter
	private static String championName;
	private int counter = 0;

	public static void main(String[] args) throws Exception {
//        Parent root = FXMLLoader.load(getClass().getResource("/sample.fxml"));
//        primaryStage.setTitle("Hello World");
//        primaryStage.setScene(new Scene(root, 300, 275));
//        primaryStage.show();

		WindowScraper windowScraper = new WindowScraper();

		APIWrapper apiWrapper = new APIWrapper();
		apiWrapper.getStaticData();


		runesMap = apiWrapper.getOPGGRunes("Akali", 84);
		championName = "Akali";
		launch();

		apiWrapper.setLoLClientInfo();
		List<RunePage> runePageList = apiWrapper.getPages();
		RunePage apiPage = runePageList.stream()
			.filter(o -> o.getName()
				.equalsIgnoreCase("AutoRune"))
			.findFirst()
			.orElse(null);
		if(apiPage != null) {
			String mostFrequentPosition = runesMap.keySet().stream()
				.findFirst()
				.orElse(null);
			List<String> runes = runesMap.get(mostFrequentPosition).get(0).getRunes();
			apiPage.setPrimaryStyleId(Integer.parseInt(runes.get(0)));
			apiPage.setSubStyleId(Integer.parseInt(runes.get(5)));
			List<Integer> selectedPerkIds = new ArrayList<>();
			selectedPerkIds.add(Integer.parseInt(runes.get(1)));
			selectedPerkIds.add(Integer.parseInt(runes.get(2)));
			selectedPerkIds.add(Integer.parseInt(runes.get(3)));
			selectedPerkIds.add(Integer.parseInt(runes.get(4)));
			selectedPerkIds.add(Integer.parseInt(runes.get(6)));
			selectedPerkIds.add(Integer.parseInt(runes.get(7)));
			// runes now include perks
			selectedPerkIds.add(Integer.parseInt(runes.get(8)));
			selectedPerkIds.add(Integer.parseInt(runes.get(9)));
			selectedPerkIds.add(Integer.parseInt(runes.get(10)));
			apiPage.setSelectedPerkIds(selectedPerkIds);

			apiWrapper.replacePage(apiPage.getId(), apiPage);

		} else {
			log.error("Cannot find rune page named AutoRune. Please create page.");
		}


		String prevChampNameScraped = "";
		while(true) {
			if(windowScraper.getHWnd() == 0 || apiWrapper.getPid().equals("null")) {
				windowScraper.findWindow("League of Legends");
				apiWrapper.setLoLClientInfo();
				Thread.sleep(5000);
			} else {
				String approxChampName = windowScraper.update();
				if(!approxChampName.equals("") && prevChampNameScraped.equals("")) {
					log.debug("Name scraped from screenshot: {}", approxChampName);
					Champion champion = apiWrapper.getChampionBySkinName(approxChampName);
					if(champion == null) {
						log.debug("Cannot find champion with name: {}", approxChampName);
						int minLevenshteinDistance = Integer.MAX_VALUE;
						String closestChampionName = null;
						for(String champName : apiWrapper.getVersionedSkinIdMap().getRight().keySet()) {
							int currLevenshteinDistance = StringUtils.getLevenshteinDistance(champName, approxChampName);
							if(currLevenshteinDistance < minLevenshteinDistance) {
								minLevenshteinDistance = currLevenshteinDistance;
								closestChampionName = champName;
							}
						}
						log.debug("Closest match to {} is {}", approxChampName, closestChampionName);
						champion = apiWrapper.getChampionBySkinName(closestChampionName);
					} else {
						champion = apiWrapper.getChampionBySkinName(approxChampName);
					}

					log.info("Locked in champion {}", champion.getName());

//					List<RunePage> runePageList = apiWrapper.getPages();
//					RunePage apiPage = runePageList.stream()
//						.filter(o -> o.getName()
//							.equalsIgnoreCase("AutoRune"))
//						.findFirst()
//						.orElse(null);
//
//					runesMap = apiWrapper.getOPGGRunes(champion.getName());
////					launch();
//
//					if(apiPage != null) {
//						String mostFrequentPosition = runesMap.keySet().stream()
//							.findFirst()
//							.orElse(null);
//
//						if(mostFrequentPosition != null) {
//							List<String> runes = runesMap.get(mostFrequentPosition).get(0).getRunes();
//							apiPage.setPrimaryStyleId(Integer.parseInt(runes.get(0)));
//							apiPage.setSubStyleId(Integer.parseInt(runes.get(5)));
//							List<Integer> selectedPerkIds = new ArrayList<>();
//							selectedPerkIds.add(Integer.parseInt(runes.get(1)));
//							selectedPerkIds.add(Integer.parseInt(runes.get(2)));
//							selectedPerkIds.add(Integer.parseInt(runes.get(3)));
//							selectedPerkIds.add(Integer.parseInt(runes.get(4)));
//							selectedPerkIds.add(Integer.parseInt(runes.get(6)));
//							selectedPerkIds.add(Integer.parseInt(runes.get(7)));
//							// runes now include perks
//							selectedPerkIds.add(Integer.parseInt(runes.get(8)));
//							selectedPerkIds.add(Integer.parseInt(runes.get(9)));
//							selectedPerkIds.add(Integer.parseInt(runes.get(10)));
//							apiPage.setSelectedPerkIds(selectedPerkIds);
//
//							apiWrapper.replacePage(apiPage.getId(), apiPage);
//
//						} else {
//							log.error("Not enough data for champion {}", champion.getName());
//						}
//
//					} else {
//						log.error("Cannot find rune page named AutoRune. Please create page.");
//					}
				}
				prevChampNameScraped = approxChampName;
				Thread.sleep(1000);
			}
		}
	}

	@SuppressWarnings("static-access")
	public void start(Stage primaryStage) throws Exception {
		Parent root = FXMLLoader.load(getClass().getResource("/sample.fxml"));
		primaryStage.setTitle("Rune Selector");
		Scene scene = new Scene(root, 900, 800);
//		scene.getStylesheets().add("/sample.css");
		primaryStage.setScene(scene);
		primaryStage.show();

//		Label selectedRole = new Label();
//		selectedRole.setFont(new Font("Calibri", 15));
//		selectedRole.setTextFill(Color.BLACK);
//
//		List<Button> roleButtons = new ArrayList<>();
//		for(String role : runesMap.keySet()) {
//			Button button = new Button();
//			button.setFont(new Font("Calibri", 15));
//			button.setText(role);
//			button.setOnAction(event -> selectedRole.setText("Your role is " + button.getText()));
//			roleButtons.add(button);
//		}
//
//		BorderPane bp = new BorderPane();
//		bp.setBottom(selectedRole);
//		bp.setAlignment(selectedRole, Pos.TOP_CENTER);
//		int i = 0;
//		for(Button button : roleButtons) {
//			grid.add(button, i++, 0);
//			bp.setCenter(selectedRole);
//		}
//
//		StackPane root = new StackPane();
//		root.getChildren().add(grid);
//		root.getChildren().add(bp);
//		stage.setScene(new Scene(root, 250, 250));
//		stage.show();
	}

}
