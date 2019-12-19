package com.bhjelmar;

import com.bhjelmar.api.APIWrapper;
import com.bhjelmar.data.Champion;
import com.bhjelmar.data.RunePage;
import com.bhjelmar.data.RuneSelection;
import com.bhjelmar.ui.RuneSelectionController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Log4j2
public class Main_old extends Application {

	private static boolean debug = true;
	@Getter
	private static String skinName = "Kennen";

	@Getter
	private static Map<String, List<RuneSelection>> runesMap;
	@Getter
	private static Champion champion;
	@Setter
	private static Pair<String, String> selectedRoleAndRune;

	public static void main(String[] args) throws Exception {

		APIWrapper apiWrapper = new APIWrapper();
//		apiWrapper.getStaticData();

		String prevChampNameScraped = "";
		while (true) {
			if (apiWrapper.getPid() == null || apiWrapper.getPid().equals("null")) {
				apiWrapper.setLoLClientInfo();
				Thread.sleep(5000);
			} else {

				champion = null;
				while (champion == null) {
//					Pair<Integer, Integer> champSkinNum = Files.grepStreamingFile("E:/Games/LoL");
////					champion = apiWrapper.getChampionById(champSkinNum.getLeft());
//					if(champion != null) {
//						champion.setSkinNum(champSkinNum.getRight());
//					}
				}

				// TODO: turn this into observer
				log.info("Locked in champion {}", champion.getName());

				List<RunePage> runePageList = apiWrapper.getPages();
				RunePage apiPage = runePageList.stream()
					.filter(o -> o.getName()
						.equalsIgnoreCase("AutoRune"))
					.findFirst()
					.orElse(null);

				runesMap = apiWrapper.getOPGGRunes(champion);
				launch();

				if (apiPage != null) {
					String mostFrequentPosition = runesMap.keySet().stream()
						.findFirst()
						.orElse(null);

					if (mostFrequentPosition != null) {
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

				Thread.sleep(1000);
			}
		}
	}

	@SuppressWarnings("static-access")
	public void start(Stage primaryStage) throws Exception {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/startup.fxml"));
		Parent root = FXMLLoader.load(getClass().getResource("/startup.fxml"));
		primaryStage.setTitle("AutoRune");
		primaryStage.getIcons().add(new Image("/icon.png"));
		Scene scene = new Scene(root, 700, 900);
		scene.getStylesheets().add("/main.css");
		primaryStage.setResizable(false);
		primaryStage.setScene(scene);
		primaryStage.show();
		((RuneSelectionController) loader.getController()).setStage(primaryStage);
	}

}
