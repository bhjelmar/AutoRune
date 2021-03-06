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
import com.jfoenix.controls.JFXButton;
import com.sun.javafx.PlatformUtil;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;
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
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
public class StartupController extends BaseController {

	@Getter
	private static LoLClientAPI lolClientAPI;
	public GridPane header;
	public ImageView autoRuneIcon;
	public TextField lolHomeDirectory;
	public Text selectLoLHomeText;
	public JFXButton browseDirectory;
	public BorderPane window;
	public VBox vbox;
	public BorderPane border;
	public HBox hbox;
	public TextFlow textFlow;
	public ScrollPane textScroll;
	@Getter
	private static AtomicBoolean connectedLastIteration = new AtomicBoolean(false);
	public ImageView statusLightIcon;
	public VBox textVbox;

	@Getter
	private Pair<String, Map<Integer, Champion>> versionedIdChampionMap;
	@Getter
	private Pair<String, Map<Integer, Integer>> versionedSkinIdMap;
	public Text isLoggedInText;

	@SneakyThrows
	public static void start(boolean comingFromRuneSelection) {
		FXMLLoader loader = new FXMLLoader(StartupController.class.getResource("/fxml/startup.fxml"));
		Parent root = loader.load();

		Scene scene = new Scene(root, 450, 450);
		scene.getStylesheets().add("/css/main.css");

		Platform.runLater(() -> {
			BaseController.getPrimaryStage().setScene(scene);
			BaseController.getPrimaryStage().show();

			((StartupController) loader.getController()).onWindowLoad(comingFromRuneSelection);
		});
	}

	public void initialize() {
		sharedState();
		lolClientAPI = new LoLClientAPI();

		window.setStyle("-fx-background-image: url('images/default.jpg');");
		autoRuneIcon.setImage(new Image("images/icons/96x96.png"));
		autoRuneIcon.setFitWidth(50);
		autoRuneIcon.setFitHeight(50);

//		statusLightIcon.setImage(new Image("images/red_light.png"));
		statusLightIcon.setFitWidth(25);
		statusLightIcon.setFitHeight(25);

		textScroll.setStyle("-fx-border-color: #2b2b2b; -fx-border-radius: 3; -fx-border-width: 3;");
		textScroll.setFitToWidth(true);
		textScroll.vvalueProperty().bind(textFlow.heightProperty()); // always keep scroll at bottom

		border.setStyle("-fx-background-color: rgba(43, 43, 43, 0.2); -fx-background-radius: 3;");
		header.setStyle("-fx-background-color: rgba(43, 43, 43, 0.2); -fx-background-radius: 3;");

		textVbox.setStyle("-fx-border-color: #2b2b2b; -fx-border-radius: 2; -fx-border-width: 3 0 3 0; -fx-padding: 10 0 10 0");

		lolHomeDirectory.setStyle("-fx-border-color: #2b2b2b; -fx-border-width: 3 3 3 3;");

		selectLoLHomeText.setStyle("");
		selectLoLHomeText.setFont(Font.font("Friz Quadrata", 18));
	}

	public void onWindowLoad(boolean comingFromRuneSelection) {
		if (new File("lolHome.ser").isFile()) {
			String lolHome = Files.deserializeData("lolHome.ser");
			if (lolHome != null) {
				lolHomeDirectory.setText(lolHome);
				beginProcessingLoop(comingFromRuneSelection);
			}
		}
		browseDirectory.setOnMouseClicked(e -> {
			lolHomeDirectory.setText(selectLoLHome());
			beginProcessingLoop(comingFromRuneSelection);
		});
	}

	@SneakyThrows
	private void beginProcessingLoop(boolean comingFromRuneSelection) {
		if (!validLoLHome(lolHomeDirectory.getText())) {
			selectLoLHomeText.setText("Are you sure LoL is installed here?");
			selectLoLHomeText.setFill(Paint.valueOf("Red"));
			selectLoLHomeText.setStyle("-fx-stroke: #2d2d2d; -fx-stroke-type: outside; -fx-stroke-width: 1;");
			return;
		}

		selectLoLHomeText.setText("Found League of Legends!");
		selectLoLHomeText.setFill(Paint.valueOf("Green"));
		selectLoLHomeText.setStyle("-fx-stroke: #2d2d2d; -fx-stroke-type: outside; -fx-stroke-width: 1;");
		selectLoLHomeText.setFont(Font.font("Friz Quadrata", 18));

		isLoggedInText.setText("Awaiting connection to LoL client.");
		isLoggedInText.setFill(Paint.valueOf("White"));
		isLoggedInText.setStyle("-fx-stroke: #2d2d2d; -fx-stroke-type: outside; -fx-stroke-width: 1;");
		isLoggedInText.setFont(Font.font("Friz Quadrata", 18));

		Files.serializeData(lolHomeDirectory.getText(), "lolHome.ser");

		// Runs in the background and updates isLoggedIn every 5 seconds.
		// Other methods in the task below observe the isLoggedIn.
		Timeline leagueLoginVerification = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
//			logToWindowConsole("Ensuring LoL process is running...", Severity.DEBUG);
			Pair<String, Severity> message = lolClientAPI.setLoLClientInfo();
			Platform.runLater(() -> {
				if (LoLClientAPI.isLoggedIn()) {
					if (!connectedLastIteration.get()) {
//						statusLightIcon.setImage(new Image("images/green_light.png"));
						isLoggedInText.setText("Connected to LoL client!");
						isLoggedInText.setFill(Paint.valueOf("Green"));
						isLoggedInText.setStyle("-fx-stroke: #2d2d2d; -fx-stroke-type: outside; -fx-stroke-width: 1;");
						logToWindowConsole("Connected to LoL client.", Severity.INFO);
						// TODO: move summoner ID detection logic to here
					}
				} else {
					if (connectedLastIteration.get()) {
//						statusLightIcon.setImage(new Image("images/red_light.png"));
						isLoggedInText.setText("Disconnected from LoL client.");
						isLoggedInText.setFill(Paint.valueOf("Red"));
						isLoggedInText.setStyle("-fx-stroke: #2d2d2d; -fx-stroke-type: outside; -fx-stroke-width: 1;");
						logToWindowConsole("Disconnected from LoL client.", Severity.ERROR);
						logToWindowConsole("Awaiting connection to LoL client.", Severity.INFO);
					}
				}
				connectedLastIteration.set(LoLClientAPI.isLoggedIn());
			});
		}));
		leagueLoginVerification.setCycleCount(Timeline.INDEFINITE);
		leagueLoginVerification.play();

		Task<Void> task = new Task<Void>() {
			@SneakyThrows
			public Void call() {
				if (comingFromRuneSelection) {
					logToWindowConsole("Set runes for " + RuneSelectionController.getChampion().getName() + ".", Severity.INFO);
					logToWindowConsole("Awaiting game start.", Severity.INFO);

					// grep for POST_CHAMP_SELECT if champion is not already locked in
					// already locked in a champion
					while (!hasChampSelectEnded()) {
						Thread.sleep(1000);
					}
					logToWindowConsole("Game start detected.", Severity.INFO);
					Thread.sleep(5000);
				}

				if (!initializeData()) {
					logToWindowConsole("Startup failed. Exiting.", Severity.ERROR);
					Thread.sleep(2500);
					return null;
				}

				logToWindowConsole("Startup completed successfully!", Severity.INFO);
				logToWindowConsole("Awaiting connection to LoL client.", Severity.INFO);
//				logToWindowConsole("AutoRune will move to the background and await champion lock in.", Severity.INFO);
//				Thread.sleep(3000);

				String summonerId = null;
				Champion champion = null;

				boolean continueLooping = true;
				while (continueLooping) {
					if (!LoLClientAPI.isLoggedIn()) {
						summonerId = null;
						Thread.sleep(1000);
						continue;
					}
					if (summonerId == null || summonerId.equals("")) {
						String lastIterationSummonerId = summonerId;
						summonerId = getSummonerId(lolHomeDirectory.getText());
						if (lastIterationSummonerId == null && summonerId.equals("")) {
							logToWindowConsole("Unable to find summoner id.", Severity.INFO);
						}
						if (!summonerId.equals("")) {
							logToWindowConsole("Discovered summoner id: " + summonerId + ".", Severity.INFO);
							logToWindowConsole("Awaiting champion lock in.", Severity.INFO);
						}
					}
					if (summonerId.equals("")) {
						Thread.sleep(1000);
						continue;
					}

					// no locked champion yet
					if (!BaseController.isDebug()) {
						champion = getLockedInChampion(summonerId);
					} else {
						// get random champion
						champion = versionedIdChampionMap.getRight().values().stream()
							.skip((int) (versionedIdChampionMap.getRight().values().size() * Math.random()))
							.findAny().get();
					}
					if (champion != null) {
						continueLooping = false;
						logToWindowConsole("Locked in " + champion.getName() + ".", Severity.INFO);
						logToWindowConsole("Getting rune information from op.gg", Severity.INFO);

						Map<String, List<RuneSelection>> runesMap = RunesAPI.getOPGGRunes(champion);
						loadRuneSelectionScene(champion, runesMap);
					}
				}
				Thread.sleep(1000);
				return null;
			}
		};
		task.setOnFailed(evt -> {
			logToWindowConsole("The task failed with the following exception: " + task.getException().getLocalizedMessage(), Severity.ERROR);
			task.getException().printStackTrace(System.err);
		});
		task.setOnSucceeded(evt -> {
			leagueLoginVerification.stop();
		});
		new Thread(task).start();
	}

	private boolean hasChampSelectEnded() {
		String line = Files.grepStreamingFile(lolHomeDirectory.getText(), false, "POST_CHAMP_SELECT");
		return line != null;
		// TODO: add support for someone  dodging, this will only handle games starting
	}

	private String selectLoLHome() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		File selectedFile = directoryChooser.showDialog(BaseController.getPrimaryStage());
		return selectedFile == null ? "" : selectedFile.getPath();
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

	private String getSummonerId(String lolHome) {
		logToWindowConsole("Searching for current summoner id.", Severity.INFO);
		String line = Files.grepStreamingFile(lolHome, true, "Received current-summoner ID change (0 -> ");
		// will be null when isLoggedIn is false (league client closed while we were processing)
		if (line == null) {
			return "";
		}
		String summonerId = StringUtils.substringAfter(line, "-> ");
		return StringUtils.removeEnd(summonerId, ")");
	}

	private Champion getLockedInChampion(String summonerId) {
		Champion champion = null;
		while (champion == null) {
			String line = Files.grepStreamingFile(lolHomeDirectory.getText(), false, "/lol-champ-select/v1/session");
			if (line == null) {
				return null;
			}
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
			logToWindowConsole("Updating data for patch " + currentLoLVersion + ".", Severity.INFO);
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
		t.setFont(Font.font(java.awt.Font.MONOSPACED, 12));
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
