package com.bhjelmar.ui;

import com.bhjelmar.Main;
import com.bhjelmar.data.RuneSelection;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Pair;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Map;

@Data
@Log4j2
public class RuneSelectionController {

	private static Map<String, List<RuneSelection>> runesMap;
	public Label championNameLabel;
	public StackPane runesPane;
	public WebView testWebView;

	@FXML
	private TabPane roleSelection;

	public void initialize() {
		championNameLabel.setText(Main.getChampionName());

		roleSelection.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
		roleSelection.tabMinWidthProperty().set(100);
		roleSelection.tabMaxWidthProperty().set(100);

		runesMap = Main.getRunesMap();
		for(String role : runesMap.keySet()) {
			roleSelection.getTabs().add(new Tab(role));
		}

		createRunesList(runesMap.keySet().iterator().next());
		roleSelection.getSelectionModel().selectedItemProperty().addListener(
			(ov, t, t1) -> createRunesList(t1.getText())
		);
	}

	private void createRunesList(String role) {
//		runesPane.getChildren().removeAll(runesPane.getChildren().stream().filter(e -> e instanceof WebView).collect(Collectors.toList()));
		runesPane.getChildren().clear();

		int i = 0;
		// showing > 3 pages per role is overkill imo
		while(i < 3 && i < runesMap.get(role).size()) {
			RuneSelection runeSelection = runesMap.get(role).get(i);

			WebView webView = new WebView();
			WebEngine webEngine = webView.getEngine();

			webView.setId(role + ":" + String.valueOf(i));
			webView.setOnMouseClicked(event -> {
				String paneId = ((WebView) event.getSource()).getId();
				String selectedRole = paneId.substring(0, paneId.indexOf(":"));
				String selectedPage = paneId.substring(paneId.indexOf(":") + 1);
				Main.setSelectedRoleAndRune(new Pair<>(selectedRole, selectedPage));

				Platform.exit();
			});
			webView.setOnMouseEntered(event -> {
				webView.setOpacity(.5);
			});
			webView.setOnMouseExited(event -> {
				webView.setOpacity(1);
			});
			webView.setTranslateY(220 * i);
			webEngine.loadContent(runeSelection.getElement().toString().replaceAll("//opgg-static.akamaized.net", "http://opgg-static.akamaized.net"));
			webEngine.setUserStyleSheetLocation(getClass().getResource("/attempt_2.css").toString());

			runesPane.getChildren().add(webView);

			i++;
		}
	}

}
