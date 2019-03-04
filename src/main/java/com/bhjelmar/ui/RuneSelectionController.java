package com.bhjelmar.ui;

import com.bhjelmar.Main;
import com.bhjelmar.data.RuneSelection;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Pair;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

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

	@FXML
	private TabPane roleSelection;

	public void initialize() {
		window.setStyle("-fx-background-image: url('https://ddragon.leagueoflegends.com/cdn/img/champion/splash/" +
			Main.getChampionName() +
			"_0.jpg');");

		footer.setStyle("-fx-background-color: #2b2b2b;");
		header.setStyle("-fx-background-color: #2b2b2b;");
		runesPane.setSpacing(10);

//		roleSelection.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
//		roleSelection.tabMinWidthProperty().set(100);
//		roleSelection.tabMaxWidthProperty().set(100);

		runesMap = Main.getRunesMap();
		for(String role : runesMap.keySet()) {
			roleSelection.getTabs().add(new Tab(role));
		}

		createRunesList(runesMap.keySet().iterator().next());
		roleSelection.getSelectionModel().selectedItemProperty().addListener(
			(ov, t, t1) -> createRunesList(t1.getText())
		);

		donate.setOnAction(event -> {
			try {
				Desktop.getDesktop().browse(new URI("https://www.paypal.me/bhjelmar"));
			} catch (IOException | URISyntaxException e1) {
				e1.printStackTrace();
			}
		});
		contribute.setOnAction(event -> {
			try {
				Desktop.getDesktop().browse(new URI("https://github.com/bhjelmar/autorune"));
			} catch (IOException | URISyntaxException e1) {
				e1.printStackTrace();
			}
		});
	}

	private void createRunesList(String role) {
//		runesPane.getChildren().remove(1, runesPane.getChildren().size());
//		GridPane gp = ((GridPane)runesPane.getChildren().get(0));
//		gp.setStyle("-fx-background-color: #2b2b2b;");
		runesPane.getChildren().clear();

		championNameLabel.setText(Main.getChampionName());
		championImage.setImage(new Image("https://opgg-static.akamaized.net/images/lol/champion/" + Main.getChampionName() + ".png"));
		championImage.setFitHeight(30);
		championImage.setFitWidth(30);

		int i = 0;
		// showing > 3 pages per role is overkill imo
		while(i < 3 && runesMap.get(role).size() > i) {
			RuneSelection runeSelection = runesMap.get(role).get(i);

			WebView webView = new WebView();
			WebEngine webEngine = webView.getEngine();
			webView.setMaxSize(479, 250);
			webView.setId(role + ":" + i);
			webView.setOpacity(.85);
			webView.setOnMouseClicked(event -> {
				String paneId = ((WebView) event.getSource()).getId();
				String selectedRole = paneId.substring(0, paneId.indexOf(":"));
				String selectedPage = paneId.substring(paneId.indexOf(":") + 1);
				Main.setSelectedRoleAndRune(new Pair<>(selectedRole, selectedPage));

				Platform.exit();
			});
			webView.setOnMouseEntered(event -> {
				webView.setOpacity(.90);
			});
			webView.setOnMouseExited(event -> {
				webView.setOpacity(.85);
			});

			webEngine.loadContent(runeSelection.getElement().toString()
				.replaceAll("//opgg-static.akamaized.net", "http://opgg-static.akamaized.net")
				.replaceFirst("% <em>", "% Pick Rate___________<em>")
				.replaceFirst("%</td>", "% Win Rate </td>")
				.replaceFirst("</em>", " Games_______</em>"));
			webEngine.setUserStyleSheetLocation(getClass().getResource("/rune_selection.css").toString());

			runesPane.getChildren().add(webView);

			i++;
		}
	}

}
