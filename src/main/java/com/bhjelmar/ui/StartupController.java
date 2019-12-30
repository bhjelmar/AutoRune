package com.bhjelmar.ui;

import com.bhjelmar.api.DataDragonAPI;
import com.bhjelmar.api.LoLClientAPI;
import com.bhjelmar.api.RunesAPI;
import com.bhjelmar.api.response.ChampSelect;
import com.bhjelmar.data.Champion;
import com.bhjelmar.data.RuneSelection;
import com.bhjelmar.util.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.javafx.PlatformUtil;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.DirectoryChooser;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

@Log4j2
public class StartupController extends BaseController {

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
	public TextFlow textFlow;
	public ScrollPane textScroll;

	@Getter
	private Pair<String, Map<Integer, Champion>> versionedIdChampionMap;
	@Getter
	private Pair<String, Map<Integer, Integer>> versionedSkinIdMap;

	@SneakyThrows
	public static void start() {
		FXMLLoader loader = new FXMLLoader(StartupController.class.getResource("/fxml/startup.fxml"));
		Parent root = loader.load();

		Scene scene = new Scene(root, 450, 350);
		scene.getStylesheets().add("/css/main.css");

		BaseController.getPrimaryStage().setScene(scene);
		BaseController.getPrimaryStage().show();

		((StartupController) loader.getController()).onWindowLoad();
	}

	public void initialize() {
		sharedState();
		lolClientAPI = new LoLClientAPI();

		window.setStyle("-fx-background-image: url('images/default.jpg');");
		autoRuneIcon.setImage(new Image("images/icons/96x96.png"));
		autoRuneIcon.setFitWidth(50);
		autoRuneIcon.setFitHeight(50);

		textScroll.setStyle("-fx-border-color: #2b2b2b; -fx-border-radius: 3; -fx-border-width: 3;");
		textScroll.setFitToWidth(true);
		border.setStyle("-fx-background-color: rgba(43, 43, 43, 0.6); -fx-background-radius: 3;");
		header.setStyle("-fx-background-color: rgba(43, 43, 43, 0.6); -fx-background-radius: 3;");
		textScroll.vvalueProperty().bind(textFlow.heightProperty());
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

	@SneakyThrows
	private void beginProcessingLoop() {
		if (!validLoLHome(lolHomeDirectory.getText())) {
			selectLoLHomeText.setText("Are you sure LoL is installed here?");
			selectLoLHomeText.setFill(Paint.valueOf("Red"));
			return;
		}

		selectLoLHomeText.setText("Found League of Legends!");
		selectLoLHomeText.setFill(Paint.valueOf("Green"));

		Files.serializeData(lolHomeDirectory.getText(), "lolHome.ser");

		Task<Void> task = new Task<Void>() {
			@SneakyThrows
			public Void call() {
				if (!initializeData()) {
					logToWindowConsole("Startup failed. Exiting.", Severity.ERROR);
					Thread.sleep(2500);
					return null;
				}
				logToWindowConsole("Startup completed successfully!", Severity.INFO);
				logToWindowConsole("AutoRune will move to the background and await champion lock in.", Severity.INFO);

				Thread.sleep(5000);
				Platform.runLater(() -> {
					BaseController.getPrimaryStage().toBack();
				});

				blockForLeagueLogin();
				String summonerId = blockForSummonerId(lolHomeDirectory.getText());
				logToWindowConsole("Awaiting champion lock in.", Severity.INFO);

				Champion champion;
				if (!BaseController.isDebug()) {
					champion = blockForChampionLockIn(summonerId);
				} else {
					// get random champion
					champion = versionedIdChampionMap.getRight().values().stream()
						.skip((int) (versionedIdChampionMap.getRight().values().size() * Math.random()))
						.findAny().get();
				}
				logToWindowConsole("Locked in  " + champion.getName() + ".", Severity.INFO);

				logToWindowConsole("Getting rune information from op.gg", Severity.INFO);
				Map<String, List<RuneSelection>> runesMap = RunesAPI.getOPGGRunes(champion);

				loadRuneSelectionScene(champion, runesMap);
				return null;
			}
		};

		task.setOnFailed(evt -> {
			logToWindowConsole("The task failed with the following exception: " + task.getException().getLocalizedMessage(), Severity.ERROR);
			task.getException().printStackTrace(System.err);
		});
		new Thread(task).start();
	}

	private String selectLoLHome() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		File selectedFile = directoryChooser.showDialog(BaseController.getPrimaryStage());
		return selectedFile.getPath();
	}

	@SneakyThrows
	private boolean validLoLHome(String lolHomeDirectory) {
		String lolApplicationName;
		if (PlatformUtil.isWindows()) {
			lolApplicationName = "LeagueClient.exe";
		} else { // Mac
			lolApplicationName = "League of Legends.app";
		}
		return java.nio.file.Files.list(Paths.get(lolHomeDirectory))
			.anyMatch(e -> e.getFileName().toString().equals(lolApplicationName));
	}

	private String blockForSummonerId(String lolHome) {
		logToWindowConsole("Scraping log files for summonerId", Severity.INFO);
		String line = Files.grepStreamingFile(lolHome, true, "Received current-summoner ID change (0 -> ");
		String summonerId = StringUtils.substringAfter(line, "-> ");
		summonerId = StringUtils.removeEnd(summonerId, ")");
		logToWindowConsole("Discovered summoner id: " + summonerId, Severity.INFO);
		return summonerId;
	}

	private Champion blockForChampionLockIn(String summonerId) {
		Champion champion = null;
		while (champion == null) {
			String line = Files.grepStreamingFile(lolHomeDirectory.getText(), false, "/lol-champ-select/v1/session");
			String payload = StringUtils.substringAfter(line, ":");

			Gson gson = new GsonBuilder().create();
			ChampSelect champSelect = gson.fromJson(payload, ChampSelect.class);

			ChampSelect.MyTeam myChampion = champSelect.getMyTeam().stream()
				.filter(e -> Integer.toString(e.getSummonerId()).equals(summonerId) && e.getChampionId() != 0)
				.findFirst().orElse(new ChampSelect.MyTeam());
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
		return champion;
	}

	private boolean initializeData() {
		logToWindowConsole("Getting Current LoL version.", Severity.INFO);
		String currentLoLVersion = DataDragonAPI.getCurrentLOLVersion();
		logToWindowConsole("Current lol version is " + currentLoLVersion, Severity.INFO);

		Pair<Boolean, Pair<Pair<String, Map<Integer, Champion>>, Pair<String, Map<Integer, Integer>>>> shouldRefresh
			= Files.shouldUpdateStaticData(currentLoLVersion);
		if (shouldRefresh.getLeft()) {
			logToWindowConsole("Local data store not found, retrieving current champion data.", Severity.INFO);
			Pair<Pair<String, Map<Integer, Champion>>, Pair<String, Map<Integer, Integer>>> championAndSkinIds
				= DataDragonAPI.getChampionSkinsAndIDs(currentLoLVersion);
			versionedIdChampionMap = championAndSkinIds.getLeft();
			versionedSkinIdMap = championAndSkinIds.getRight();

			logToWindowConsole("Serializing latest data to disk,", Severity.INFO);
			com.bhjelmar.util.Files.serializeData(versionedIdChampionMap, "versionedIdChampionMap.ser");
			com.bhjelmar.util.Files.serializeData(versionedSkinIdMap, "versionedSkinIdMap.ser");
		} else {
			logToWindowConsole("Local data store found.", Severity.INFO);
			versionedIdChampionMap = shouldRefresh.getRight().getLeft();
			versionedSkinIdMap = shouldRefresh.getRight().getRight();
		}
		return versionedIdChampionMap != null && versionedSkinIdMap != null;
	}

	@SneakyThrows
	private void blockForLeagueLogin() {
		logToWindowConsole("Waiting for LoL Process to Start.", Severity.INFO);
		Pair<String, Severity> message = lolClientAPI.setLoLClientInfo();
		logToWindowConsole(message.getLeft(), message.getRight());
		while (lolClientAPI.getPid() == null || lolClientAPI.getPid().equals("null")) {
			Thread.sleep(5000);
			message = lolClientAPI.setLoLClientInfo();
			logToWindowConsole(message.getLeft(), message.getRight());
		}
	}

	private void loadRuneSelectionScene(Champion champion, Map<String, List<RuneSelection>> runesMap) {
		logToWindowConsole("Starting up runes selection...", Severity.INFO);
		Platform.runLater(() -> {
			RuneSelectionController.start(champion, runesMap);
		});
	}

	public void logToWindowConsole(String text, Severity severity) {
		SimpleDateFormat sdf = new SimpleDateFormat("hh.mm.ss");
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		Text t = new Text(sdf.format(timestamp) + ": " + text + "\n");
		switch (severity) {
			case INFO:
				log.info(text);
				t.setFill(Paint.valueOf("White"));
				break;
			case WARN:
				log.warn(text);
				t.setFill(Paint.valueOf("Yellow"));
				break;
			case ERROR:
				log.error(text);
				t.setFill(Paint.valueOf("Red"));
				break;
			case DEBUG:
				log.debug(text);
				t.setFill(Paint.valueOf("Blue"));
				break;
		}
		Platform.runLater(() -> {
			textFlow.getChildren().add(t);
		});
	}

	public enum Severity {
		INFO,
		WARN,
		ERROR,
		DEBUG;
	}

}
