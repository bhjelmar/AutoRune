package com.bhjelmar.ui;

import javafx.scene.control.Hyperlink;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Log4j2
public abstract class BaseController {

	@Getter
	@Setter
	private static Stage primaryStage;

	public GridPane footer;
	public Hyperlink donate;
	public Hyperlink contribute;

	@Getter
	@Setter
	private static boolean debug = false;

	public void sharedState() {
		footer.setStyle("-fx-background-color: #2b2b2b;");

		donate.setOnAction(event -> {
			try {
				Desktop.getDesktop().browse(new URI("https://www.paypal.me/bhjelmar"));
			} catch (IOException | URISyntaxException e) {
				log.error(e.getLocalizedMessage(), e);
			}
		});
		contribute.setOnAction(event -> {
			try {
				Desktop.getDesktop().browse(new URI("https://github.com/bhjelmar/autorune"));
			} catch (IOException | URISyntaxException e) {
				log.error(e.getLocalizedMessage(), e);
			}
		});
	}

}
