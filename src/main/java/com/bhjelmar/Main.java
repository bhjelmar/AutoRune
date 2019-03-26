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
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Pair;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Log4j2
public class Main extends Application {

	private static boolean debug = true;
	@Getter
	private static String skinName = "irelia";

	@Getter
	private static Map<String, List<RuneSelection>> runesMap;
	@Getter
	private static Champion champion;
	private int counter = 0;
	@Setter
	private static Pair<String, String> selectedRoleAndRune;

	public static void main(String[] args) throws Exception {
//        Parent root = FXMLLoader.load(getClass().getResource("/sample.fxml"));
//        primaryStage.setTitle("Hello World");
//        primaryStage.setScene(new Scene(root, 300, 275));
//        primaryStage.show();

		WindowScraper windowScraper = new WindowScraper();

		APIWrapper apiWrapper = new APIWrapper();
		apiWrapper.getStaticData();

		if(debug) {
			int minLevenshteinDistance = Integer.MAX_VALUE;
			String closestSkinName = null;
			for(String champName : apiWrapper.getVersionedSkinIdMap().getRight().keySet()) {
				int currLevenshteinDistance = StringUtils.getLevenshteinDistance(champName, skinName);
				if(currLevenshteinDistance < minLevenshteinDistance) {
					minLevenshteinDistance = currLevenshteinDistance;
					closestSkinName = champName;
				}
			}
			log.debug("Closest match to {} is {}", skinName, closestSkinName);
			skinName = closestSkinName;

			champion = apiWrapper.getChampionBySkinName(skinName);
			runesMap = apiWrapper.getOPGGRunes(champion);
			launch();

			apiWrapper.setLoLClientInfo();
			List<RunePage> runePageList = apiWrapper.getPages();
			RunePage apiPage = runePageList.stream()
					.filter(o -> o.getName()
							.equalsIgnoreCase("AutoRune"))
					.findFirst()
					.orElse(null);
			if(apiPage != null) {
				List<String> runes = runesMap.get(selectedRoleAndRune.getKey()).get(Integer.parseInt(selectedRoleAndRune.getValue())).getRunes();
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
					champion = apiWrapper.getChampionBySkinName(approxChampName);
					if(champion == null) {
						log.debug("Cannot find champion with name: {}", approxChampName);
						int minLevenshteinDistance = Integer.MAX_VALUE;
						String closestSkinName = null;
						for(String champName : apiWrapper.getVersionedSkinIdMap().getRight().keySet()) {
							int currLevenshteinDistance = StringUtils.getLevenshteinDistance(champName, approxChampName);
							if(currLevenshteinDistance < minLevenshteinDistance) {
								minLevenshteinDistance = currLevenshteinDistance;
								closestSkinName = champName;
							}
						}
						log.debug("Closest match to {} is {}", approxChampName, closestSkinName);
						skinName = closestSkinName;
						champion = apiWrapper.getChampionBySkinName(closestSkinName);
					} else {
						champion = apiWrapper.getChampionBySkinName(approxChampName);
					}

					log.info("Locked in champion {}", champion.getName());

					if(!debug) {
						List<RunePage> runePageList = apiWrapper.getPages();
						RunePage apiPage = runePageList.stream()
								.filter(o -> o.getName()
										.equalsIgnoreCase("AutoRune"))
								.findFirst()
								.orElse(null);

						runesMap = apiWrapper.getOPGGRunes(champion);
						launch();

						if(apiPage != null) {
							String mostFrequentPosition = runesMap.keySet().stream()
									.findFirst()
									.orElse(null);

							if(mostFrequentPosition != null) {
								List<String> runes = runesMap.get(selectedRoleAndRune.getKey()).get(Integer.parseInt(selectedRoleAndRune.getValue())).getRunes();
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
								log.error("Not enough data for champion {}", champion.getName());
							}

						} else {
							log.error("Cannot find rune page named AutoRune. Please create page.");
						}
					}

				}
				prevChampNameScraped = approxChampName;
				Thread.sleep(1000);
			}
		}
	}

	@SuppressWarnings("static-access")
	public void start(Stage primaryStage) throws Exception {
		Parent root = FXMLLoader.load(getClass().getResource("/sample.fxml"));
		primaryStage.setTitle("AutoRune");
		primaryStage.getIcons().add(new Image("/icon.png"));
		Scene scene = new Scene(root, 700, 900);
		scene.getStylesheets().add("/main.css");
		primaryStage.setResizable(false);
		primaryStage.setScene(scene);
		primaryStage.show();
	}

}
