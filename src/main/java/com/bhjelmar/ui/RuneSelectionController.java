package com.bhjelmar.ui;

import com.bhjelmar.Main;
import com.bhjelmar.data.RuneSelection;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import javafx.application.Platform;
import javafx.fxml.FXML;
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
import javafx.util.Pair;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Data
@Log4j2
public class RuneSelectionController {

	private static Map<String, List<RuneSelection>> runesMap;
	public Label championNameLabel;
	public VBox runesPane;
	public ImageView championImage;
	public GridPane footer;
	public GridPane header;
	public Hyperlink donate;
	public Hyperlink contribute;
	public BorderPane window;
	public Label lolConnected;

	private static ClassLoader classLoader = RuneSelectionController.class.getClassLoader();

//	Media buttonPressed = new Media(new File(Objects.requireNonNull(classLoader.getResource("audio/buttonPressed.wav")).getFile()).toURI().toString());
//	Media buttonReleased = new Media(new File(Objects.requireNonNull(classLoader.getResource("audio/buttonReleased.wav")).getFile()).toURI().toString());
//	MediaPlayer buttonPressedPlayer = new MediaPlayer(buttonPressed);
//	MediaPlayer buttonReleasedPlayer = new MediaPlayer(buttonReleased);
//	Media mouseHover = new Media(new File(Objects.requireNonNull(classLoader.getResource("audio/mouseHover.wav")).getFile()).toURI().toString());
//	MediaPlayer mouseHoverPlayer = new MediaPlayer(mouseHover);

	@FXML
	private TabPane roleSelection;

	public void initialize() {
//		buttonReleasedPlayer.setVolume(.5);
//		buttonPressedPlayer.setVolume(.5);
//		mouseHoverPlayer.setVolume(.5);

		String championSplashUrl = "https://ddragon.leagueoflegends.com/cdn/img/champion/splash/" +
				Main.getChampion().getName() + "_" +
				Main.getChampion().getSkins().indexOf(Main.getSkinName()) + ".jpg";
		try {
			if(Unirest.get(championSplashUrl).asString().getStatus() == 200) {
				window.setStyle("-fx-background-image: url('" + championSplashUrl + "');");
			} else {
				log.debug("Unable to find {}", championSplashUrl);
				window.setStyle("-fx-background-image: url('https://cdn.vox-cdn.com/uploads/chorus_image/image/57522479/Ez_preseason.0.jpg');");
			}
		} catch(UnirestException e) {
			log.error(e.getLocalizedMessage(), e);
		}


		footer.setStyle("-fx-background-color: #2b2b2b;");
		header.setStyle("-fx-background-color: #2b2b2b;");
		runesPane.setSpacing(10);

		runesMap = Main.getRunesMap();
		for(String role : runesMap.keySet()) {
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

		donate.setOnAction(event -> {
			try {
				Desktop.getDesktop().browse(new URI("https://www.paypal.me/bhjelmar"));
			} catch(IOException | URISyntaxException e) {
				log.error(e.getLocalizedMessage(), e);
			}
		});
		contribute.setOnAction(event -> {
			try {
				Desktop.getDesktop().browse(new URI("https://github.com/bhjelmar/autorune"));
			} catch(IOException | URISyntaxException e) {
				log.error(e.getLocalizedMessage(), e);
			}
		});
	}

	private void createRunesList(String role) {
		runesPane.getChildren().clear();

		championNameLabel.setText(Main.getChampion().getName() + " Locked In");
		championImage.setImage(new Image("https://opgg-static.akamaized.net/images/lol/champion/" + Main.getChampion().getName() + ".png"));

		championImage.setFitHeight(30);
		championImage.setFitWidth(30);

		int i = 0;
		// showing > 3 pages per role is overkill imo
		while(i < 3 && runesMap.get(role).size() > i) {
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
				Main.setSelectedRoleAndRune(new Pair<>(selectedRole, selectedPage));

//				Platform.exit();
			});
			webView.setOnMouseEntered(event -> {
				webView.setOpacity(.85);
				Media media = new Media(new File(Objects.requireNonNull(classLoader.getResource("audio/mouseHover.wav")).getFile()).toURI().toString());
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
			webEngine.setUserStyleSheetLocation(getClass().getResource("/runetable.css").toString());

			runesPane.getChildren().add(webView);
			i++;
		}
	}

}
