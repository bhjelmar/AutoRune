package com.bhjelmar.ui;

import com.bhjelmar.api.APIWrapper;
import com.bhjelmar.api.response.ChampSelect;
import com.bhjelmar.data.Champion;
import com.bhjelmar.data.RuneSelection;
import com.bhjelmar.util.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Log4j2
public class StartupController {

	@Getter
	private static Champion champion;
	@Getter
	private static Map<String, List<RuneSelection>> runesMap;
	@Getter
	private static APIWrapper apiWrapper;
	public ImageView autoRuneIcon;
	public TextField lolHomeDirectory;
	public Text selectLoLHomeText;
	public Button browseDirectory;
	public Text foundLoL;
	public BorderPane window;
	public TextFlow textFlow;
	@FXML
	private AnchorPane ap;
	@Setter
	private Stage stage;
	@Getter
	private Pair<String, Map<Integer, Champion>> versionedIdChampionMap;
	@Getter
	private Pair<String, Map<Integer, Integer>> versionedSkinIdMap;

	public void initialize() {
		window.setStyle("-fx-background-image: url('https://cdn.vox-cdn.com/uploads/chorus_image/image/57522479/Ez_preseason.0.jpg');");
//		window.setOpacity(.2);
//		window.setStyle("-fx-background-color: #2b2b2b;");
		autoRuneIcon.setImage(new Image("icon.png"));
		autoRuneIcon.setFitWidth(50);
		autoRuneIcon.setFitHeight(50);

		if (new File("lolHome.ser").isFile()) {
			String lolHome = Files.deserializeData("lolHome.ser");
			if (lolHome != null) {
				lolHomeDirectory.setText(lolHome);
				beginProcessingLoop();
			}
		}

		browseDirectory.setOnMouseClicked(e -> {
			lolHomeDirectory.setText(selectLoLHome());
			beginProcessingLoop();
		});
	}

	private void beginProcessingLoop() {
		if (lolHomeIsGood(lolHomeDirectory.getText())) {
			foundLoL.setText("Found League of Legends!");
			foundLoL.setFill(Paint.valueOf("Green"));

			Files.serializeData(lolHomeDirectory.getText(), "lolHome.ser");

			Platform.runLater(() -> {
				initializeData();
				waitForLeagueLogin();
				String summonerId = getSummonerId(lolHomeDirectory.getText());
				waitForChampionLockIn(summonerId);
				runesMap = apiWrapper.getOPGGRunes(champion);

				loadRuneSelectionScene();
			});

		} else {
			foundLoL.setText("Unable to find League of Legends.");
			foundLoL.setFill(Paint.valueOf("Red"));
		}
	}

	private String getSummonerId(String lolHome) {
		String line = Files.grepStreamingFile(lolHome, true, "Received current-summoner ID change (0 -> ");
		String summonerId = StringUtils.substringAfter(line, "-> ");
		summonerId = StringUtils.removeEnd(summonerId, ")");
		printToApplicationConsole("Discovered summoner id: " + summonerId);
		return summonerId;
	}

	private void waitForChampionLockIn(String summonerId) {
		champion = null;
		while (champion == null) {
			String line = Files.grepStreamingFile(lolHomeDirectory.getText(), false, "/lol-champ-select/v1/session");
			String payload = StringUtils.substringAfter(line, ":");

			Gson gson = new GsonBuilder().create();
			ChampSelect champSelect = gson.fromJson(payload, ChampSelect.class);

			ChampSelect.MyTeamBean myChampion = champSelect.getMyTeam().stream()
				.filter(e -> Integer.toString(e.getSummonerId()).equals(summonerId) && e.getChampionId() != 0)
				.findFirst().orElse(new ChampSelect.MyTeamBean());

			if (myChampion.getChampionId() != 0) {
				champion = new Champion();
				champion.setChampionId(myChampion.getChampionId());
				champion.setSkinNum(versionedSkinIdMap.getRight().get(myChampion.getSelectedSkinId()));
				champion.setRole(myChampion.getAssignedPosition());
				String championName = versionedIdChampionMap.getRight().values().stream()
					.filter(e -> e.getChampionId() == myChampion.getChampionId())
					.map(Champion::getName)
					.findFirst()
					.orElse("unknown");
				champion.setName(championName);
			}
		}

		printToApplicationConsole("Locked in  " + champion.getName() + ".");
	}

	@SneakyThrows
	private void waitForLeagueLogin() {
		printToApplicationConsole("Waiting for LoL Process to Start.");
		apiWrapper.setLoLClientInfo();
		while (apiWrapper.getPid() == null || apiWrapper.getPid().equals("null")) {
			Thread.sleep(5000);
			apiWrapper.setLoLClientInfo();
		}
		printToApplicationConsole("LoL Process found.");
	}

	private void initializeData() {
		apiWrapper = new APIWrapper();
		printToApplicationConsole("Getting Current LoL version.");
		String currentLoLVersion = apiWrapper.getCurrentLOLVersion();
		printToApplicationConsole("Current lol version is " + currentLoLVersion);

		Pair<Boolean, Pair<Pair<String, Map<Integer, Champion>>, Pair<String, Map<Integer, Integer>>>> shouldRefresh = apiWrapper.shouldUpdateStaticData(currentLoLVersion);
		if (shouldRefresh.getLeft()) {
			printToApplicationConsole("Local data store not found, retrieving current champion data.");
			Pair<Pair<String, Map<Integer, Champion>>, Pair<String, Map<Integer, Integer>>> championAndSkinIds = apiWrapper.getChampionSkinsAndIDs(currentLoLVersion);
			versionedIdChampionMap = championAndSkinIds.getLeft();
			versionedSkinIdMap = championAndSkinIds.getRight();

			printToApplicationConsole("Serializing latest data to disk,");
			com.bhjelmar.util.Files.serializeData(versionedIdChampionMap, "versionedIdChampionMap.ser");
			com.bhjelmar.util.Files.serializeData(versionedSkinIdMap, "versionedSkinIdMap.ser");
		} else {
			printToApplicationConsole("Local data store found.");
			versionedIdChampionMap = shouldRefresh.getRight().getLeft();
			versionedSkinIdMap = shouldRefresh.getRight().getRight();
		}
		printToApplicationConsole("Startup completed successfully!");
	}

	@SneakyThrows
	private void loadRuneSelectionScene() {
		printToApplicationConsole("Starting up runes selection...");

		FXMLLoader loader = new FXMLLoader(getClass().getResource("/runesSelection.fxml"));
		Parent root = loader.load();

		stage.setTitle("AutoRune");
		stage.getIcons().add(new Image("/icon.png"));
		Scene scene = new Scene(root, 700, 900);
		scene.getStylesheets().add("/main.css");
		stage.setResizable(false);
		stage.setScene(scene);

		((RuneSelectionController) loader.getController()).setStage(stage);
	}

	@SneakyThrows
	private boolean lolHomeIsGood(String lolHomeDirectory) {
		return java.nio.file.Files.list(Paths.get(lolHomeDirectory))
			.anyMatch(e -> e.getFileName().toString().equals("LeagueClient.exe"));
	}

	private String selectLoLHome() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		File selectedFile = directoryChooser.showDialog(stage);
		return selectedFile.getPath();
	}

	public void printToApplicationConsole(String text) {
		Text t = new Text(text + "\n");
		t.setFill(Paint.valueOf("White"));
		textFlow.getChildren().add(t);
	}

}
