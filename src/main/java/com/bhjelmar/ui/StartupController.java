package com.bhjelmar.ui;

import com.bhjelmar.api.LoLClientAPI;
import com.bhjelmar.api.OpggAPI;
import com.bhjelmar.api.RiotAPIRepository;
import com.bhjelmar.api.response.ChampSelect;
import com.bhjelmar.data.Champion;
import com.bhjelmar.data.RuneSelection;
import com.bhjelmar.util.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Log4j2
public class StartupController extends Controller {

	@Getter
	private static LoLClientAPI lolClientAPI;
	public GridPane header;
	public ImageView autoRuneIcon;
	public TextField lolHomeDirectory;
	public Text selectLoLHomeText;
	public Button browseDirectory;
	public BorderPane window;
	public VBox vbox;
	public BorderPane border;
	public HBox hbox;
	@Setter
	private Stage primaryStage;

	@Getter
	private Pair<String, Map<Integer, Champion>> versionedIdChampionMap;
	@Getter
	private Pair<String, Map<Integer, Integer>> versionedSkinIdMap;

	public void initialize() {
		// TODO: why does this not  throw exception
//		window.setStyle("-fx-background-image: url('https://cdn.vox-cdn.com/uploads/chorus_image/image/57522479/Ez_preseason.0.jpg');");
		autoRuneIcon.setImage(new Image("icon.png"));
		autoRuneIcon.setFitWidth(50);
		autoRuneIcon.setFitHeight(50);

		sharedState();

//		footer.setStyle("-fx-background-color: #2b2b2b;");
		textScroll.setStyle("-fx-border-color: #2b2b2b; -fx-border-radius: 3; -fx-border-width: 3;");
		textScroll.setFitToWidth(true);
		border.setStyle("-fx-background-color: rgba(43, 43, 43, 0.6); -fx-background-radius: 3;");
		header.setStyle("-fx-background-color: rgba(43, 43, 43, 0.6); -fx-background-radius: 3;");
		textScroll.vvalueProperty().bind(textFlow.heightProperty());

		lolClientAPI = new LoLClientAPI();
	}

	public void onWindowLoad() {
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
			selectLoLHomeText.setText("Found League of Legends!");
			selectLoLHomeText.setFill(Paint.valueOf("Green"));

			Files.serializeData(lolHomeDirectory.getText(), "lolHome.ser");

			Task<Void> task = new Task<Void>() {
				public Void call() {
					initializeData();
					waitForLeagueLogin();
					String summonerId = getSummonerId(lolHomeDirectory.getText());
					Champion champion = waitForChampionLockIn(summonerId);
					Map<String, List<RuneSelection>> runesMap = OpggAPI.getOPGGRunes(champion);
					loadRuneSelectionScene(champion, runesMap);
					return null;
				}
			};

			new Thread(task).start();
		} else {
			selectLoLHomeText.setText("Are you sure LoL is installed here?");
			selectLoLHomeText.setFill(Paint.valueOf("Red"));
		}
	}

	private String getSummonerId(String lolHome) {
		log("Scraping log files for summonerId", Severity.INFO);
		String line = Files.grepStreamingFile(lolHome, true, "Received current-summoner ID change (0 -> ");
		String summonerId = StringUtils.substringAfter(line, "-> ");
		summonerId = StringUtils.removeEnd(summonerId, ")");
		log("Discovered summoner id: " + summonerId, Severity.INFO);
		return summonerId;
	}

	private Champion waitForChampionLockIn(String summonerId) {
		log("Awaiting champion lock in.", Severity.INFO);
		Champion champion = null;
		while (champion == null) {
			String line = Files.grepStreamingFile(lolHomeDirectory.getText(), false, "/lol-champ-select/v1/session");
			String payload = StringUtils.substringAfter(line, ":");

			Gson gson = new GsonBuilder().create();
			ChampSelect champSelect = gson.fromJson(payload, ChampSelect.class);

			ChampSelect.MyTeamBean myChampion = champSelect.getMyTeam().stream()
				.filter(e -> Integer.toString(e.getSummonerId()).equals(summonerId) && e.getChampionId() != 0)
				.findFirst().orElse(new ChampSelect.MyTeamBean());
			boolean championLockedIn = champSelect.getActions().stream()
				.flatMap(List::stream)
				.anyMatch(e -> e.getActorCellId() == myChampion.getCellId() && e.isCompleted());
			if (championLockedIn) {
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
		log("Locked in  " + champion.getName() + ".", Severity.INFO);
		return champion;
	}

	@SneakyThrows
	private void waitForLeagueLogin() {
		log("Waiting for LoL Process to Start.", Severity.INFO);
		lolClientAPI.setLoLClientInfo();
		while (lolClientAPI.getPid() == null || lolClientAPI.getPid().equals("null")) {
			Thread.sleep(5000);
			Pair<String, Severity> message = lolClientAPI.setLoLClientInfo();
			log(message.getLeft(), message.getRight());
		}
		log("LoL Process found.", Severity.INFO);
	}

	private void initializeData() {
		log("Getting Current LoL version.", Severity.INFO);
		String currentLoLVersion = RiotAPIRepository.getCurrentLOLVersion();
		log("Current lol version is " + currentLoLVersion, Severity.INFO);

		Pair<Boolean, Pair<Pair<String, Map<Integer, Champion>>, Pair<String, Map<Integer, Integer>>>> shouldRefresh = Files.shouldUpdateStaticData(currentLoLVersion);
		if (shouldRefresh.getLeft()) {
			log("Local data store not found, retrieving current champion data.", Severity.INFO);
			Pair<Pair<String, Map<Integer, Champion>>, Pair<String, Map<Integer, Integer>>> championAndSkinIds = RiotAPIRepository.getChampionSkinsAndIDs(currentLoLVersion);
			versionedIdChampionMap = championAndSkinIds.getLeft();
			versionedSkinIdMap = championAndSkinIds.getRight();

			log("Serializing latest data to disk,", Severity.INFO);
			com.bhjelmar.util.Files.serializeData(versionedIdChampionMap, "versionedIdChampionMap.ser");
			com.bhjelmar.util.Files.serializeData(versionedSkinIdMap, "versionedSkinIdMap.ser");
		} else {
			log("Local data store found.", Severity.INFO);
			versionedIdChampionMap = shouldRefresh.getRight().getLeft();
			versionedSkinIdMap = shouldRefresh.getRight().getRight();
		}
		log("Startup completed successfully!", Severity.INFO);
	}

	private void loadRuneSelectionScene(Champion champion, Map<String, List<RuneSelection>> runesMap) {
		log("Starting up runes selection...", Severity.INFO);

		Platform.runLater(() -> {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/runesSelection.fxml"));
			Parent root = null;
			try {
				root = loader.load();
			} catch (IOException e) {
				e.printStackTrace();
			}
			primaryStage.setTitle("AutoRune");
			primaryStage.getIcons().add(new Image("/icon.png"));
			Scene scene = new Scene(root, 700, 900);
			scene.getStylesheets().add("/main.css");
			primaryStage.setResizable(false);
			primaryStage.setScene(scene);
			primaryStage.show();
			((RuneSelectionController) loader.getController()).setPrimaryStage(primaryStage);

			((RuneSelectionController) loader.getController()).setChampion(champion);
			((RuneSelectionController) loader.getController()).setRunesMap(runesMap);

			primaryStage.setMaximized(true);
			primaryStage.setWidth(700);
			primaryStage.setHeight(900);

		});
	}

	@SneakyThrows
	private boolean lolHomeIsGood(String lolHomeDirectory) {
		return java.nio.file.Files.list(Paths.get(lolHomeDirectory))
			.anyMatch(e -> e.getFileName().toString().equals("LeagueClient.exe"));
	}

	private String selectLoLHome() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		File selectedFile = directoryChooser.showDialog(primaryStage);
		return selectedFile.getPath();
	}

}
