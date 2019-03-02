package com.bhjelmar.ui;

import com.bhjelmar.Main;
import com.bhjelmar.data.RuneSelection;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.GridPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Map;

@Data
@Log4j2
public class RuneSelectionController {

	private static Map<String, List<RuneSelection>> runesMap;
	private static String championName;
	//	public Label championNameLabel;
	public GridPane runesGrid;
	public WebView web;
	WebEngine webEngine;
	@FXML
	private TabPane roleSelection;

	public void initialize() {
//		championName = Main.getChampionName();
//		championNameLabel.setText(championName);
		webEngine = web.getEngine();

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
		webEngine.load("about:blank");
		StringBuilder html = new StringBuilder();
		int i = 0;
		while(i++ < 3 && i < runesMap.get(role).size()) {
			RuneSelection runeSelection = runesMap.get(role).get(i);
			html.append(runeSelection.getElement().toString().replaceAll("//opgg-static.akamaized.net", "http://opgg-static.akamaized.net"));
		}
		webEngine.loadContent(html.toString());
		webEngine.setUserStyleSheetLocation(getClass().getResource("/sample.css").toString());
	}

}
