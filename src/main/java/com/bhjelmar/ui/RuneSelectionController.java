package com.bhjelmar.ui;

import com.bhjelmar.api.RunesAPI;
import com.bhjelmar.data.Champion;
import com.bhjelmar.data.RunePage;
import com.bhjelmar.data.RuneSelection;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Log4j2
public class RuneSelectionController extends BaseController {

	public Label championNameLabel;
	public VBox runesPane;
	public ImageView championImage;
	public GridPane footer;
	public GridPane header;
	public Hyperlink donate;
	public Hyperlink contribute;
	public BorderPane window;

	@Setter
	private static Champion champion;
	@Setter
	private static Map<String, List<RuneSelection>> runesMap;

	private static ClassLoader classLoader = RuneSelectionController.class.getClassLoader();

//	Media buttonPressed = new Media(new File(Objects.requireNonNull(classLoader.getResource("audio/buttonPressed.wav")).getFile()).toURI().toString());
//	Media buttonReleased = new Media(new File(Objects.requireNonNull(classLoader.getResource("audio/buttonReleased.wav")).getFile()).toURI().toString());
//	MediaPlayer buttonPressedPlayer = new MediaPlayer(buttonPressed);
//	MediaPlayer buttonReleasedPlayer = new MediaPlayer(buttonReleased);
//	Media mouseHover = new Media(new File(Objects.requireNonNull(classLoader.getResource("audio/mouseHover.wav")).getFile()).toURI().toString());
//	MediaPlayer mouseHoverPlayer = new MediaPlayer(mouseHover);

	@FXML
	private TabPane roleSelection;

	@SneakyThrows
	public static void start(Champion champion, Map<String, List<RuneSelection>> runesMap) {
		RuneSelectionController.setChampion(champion);
		RuneSelectionController.setRunesMap(runesMap);

		FXMLLoader loader = new FXMLLoader(RuneSelectionController.class.getResource("/fxml/runesSelection.fxml"));
		Parent root = loader.load();

		Scene scene = new Scene(root, 700, 900);
		scene.getStylesheets().add("/css/main.css");

		BaseController.getPrimaryStage().setScene(scene);
		BaseController.getPrimaryStage().show();
		BaseController.getPrimaryStage().toFront();
	}

	public void initialize() {
		sharedState();

		String championSplashUrl = "https://ddragon.leagueoflegends.com/cdn/img/champion/splash/" +
			champion.getName() + "_" +
			champion.getSkinNum() + ".jpg";
		try {
			if (Unirest.get(championSplashUrl).asString().getStatus() == 200) {
				window.setStyle("-fx-background-image: url('" + championSplashUrl + "');");
			} else {
				log.debug("Unable to find {}", championSplashUrl);
				window.setStyle("-fx-background-image: url('images/default.jpg');");
			}
		} catch (UnirestException e) {
			log.error(e.getLocalizedMessage(), e);
		}

		footer.setStyle("-fx-background-color: #2b2b2b;");
		header.setStyle("-fx-background-color: #2b2b2b;");
		runesPane.setSpacing(10);

		for (String role : runesMap.keySet()) {
			roleSelection.getTabs().add(new Tab(role));
		}

		createRunesList(runesMap.keySet().iterator().next());
//		roleSelection.setOnMousePressed(event -> playAudio(buttonPressedPlayer));
//		roleSelection.setOnMouseReleased(event -> playAudio(buttonReleasedPlayer));
		roleSelection.getSelectionModel().selectedItemProperty().addListener(
			(ov, t, t1) -> {
				Media media = new Media(new File(Objects.requireNonNull(classLoader.getResource("audio/tabSelect.wav")).getFile()).toURI().toString());
				MediaPlayer sound = new MediaPlayer(media);
				sound.play();

				createRunesList(t1.getText());
			}
		);
	}

	private void createRunesList(String role) {
		runesPane.getChildren().clear();

		championNameLabel.setText(champion.getName());
		championImage.setImage(new Image("https://opgg-static.akamaized.net/images/lol/champion/" + champion.getName() + ".png"));

		championImage.setFitHeight(30);
		championImage.setFitWidth(30);

		int i = 0;
		// showing > 3 pages per role is overkill imo
		while (i < 3 && runesMap.get(role).size() > i) {
			RuneSelection runeSelection = runesMap.get(role).get(i);

			WebView webView = new WebView();
			WebEngine webEngine = webView.getEngine();
			webView.setId(role + ":" + i);
			webView.setOpacity(.70);
			webView.setOnMouseClicked(event -> {
				Media media = new Media(new File(Objects.requireNonNull(classLoader.getResource("audio/selection.wav")).getFile()).toURI().toString());
				MediaPlayer sound = new MediaPlayer(media);
				sound.play();

				String paneId = ((WebView) event.getSource()).getId();
				String selectedRole = paneId.substring(0, paneId.indexOf(":"));
				String selectedPage = paneId.substring(paneId.indexOf(":") + 1);
				Pair<String, String> selectedRoleAndRune = Pair.of(selectedRole, selectedPage);

				createNewPage(selectedRoleAndRune);

				StartupController.start();
			});
			webView.setOnMouseEntered(event -> {
				webView.setOpacity(.85);


				Media media = new Media(new File(Objects.requireNonNull(classLoader.getResource("audio/mouseHover.wav")).getFile()).toURI().toString());
				log.info(new File(Objects.requireNonNull(classLoader.getResource("audio/tabSelect.wav")).getFile()).toURI().toString());
				MediaPlayer sound = new MediaPlayer(media);
				sound.play();
			});
			webView.setOnMouseExited(event -> webView.setOpacity(.70));
//			webView.setOnMousePressed(event -> playAudio(buttonPressedPlayer));
//			webView.setOnMouseReleased(event -> playAudio(buttonReleasedPlayer));

			StringBuilder html = new StringBuilder();
			html.append("<div class=\"tabItem ChampionKeystoneRune-All\" data-tab-data-url=\"/champion/ajax/statistics/runeList/championId=32&amp;position=JUNGLE\" style=\"display: block;\"><table class=\"champion-stats__table champion-stats__table--rune sortable tablesorter tablesorter-default\" role=\"grid\">");
			html.append("<tbody aria-live=\"polite\" aria-relevant=\"all\">");
			html.append(runeSelection.getElement().toString()
				.replaceAll("//opgg-static.akamaized.net", "http://opgg-static.akamaized.net")
				.replaceAll("champion-stats__table__cell--pickrate\"> ", "champion-stats__table__cell--pickrate\">&emsp;")
				.replaceAll("<em>", " Playrate<br><br><em>&emsp;")
				.replaceAll("</em>", "</em> Games")
				.replaceAll("</td> \n" +
					" <td class=\"champion-stats__table__cell champion-stats__table__cell--winrate\">", "<br><br>&emsp;")
				.replaceAll("%</td>", "% Winrate"));
			html.append("</tbody></table></div>");

			webEngine.loadContent(html.toString());
			webEngine.setUserStyleSheetLocation(getClass().getResource("/css/runetable.css").toString());

			runesPane.getChildren().add(webView);
			i++;
		}
	}

	private void createNewPage(Pair<String, String> selectedRoleAndRune) {
		List<RunePage> runePageList = StartupController.getLolClientAPI().getPages();
		RunePage apiPage = runePageList.stream()
			.filter(o -> o.getName()
				.equalsIgnoreCase("AutoRune"))
			.findFirst()
			.orElse(null);

		runesMap = RunesAPI.getOPGGRunes(champion);

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

				StartupController.getLolClientAPI().replacePage(apiPage.getId(), apiPage);

			} else {
				log.error("Not enough data for champion {}", champion.getName());
			}

		} else {
			log.error("Cannot find rune page named AutoRune. Please create page.");
		}
	}

}
