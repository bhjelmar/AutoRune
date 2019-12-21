package com.bhjelmar.ui;

import javafx.application.Platform;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import lombok.extern.log4j.Log4j2;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

@Log4j2
public abstract class Controller {

	public GridPane footer;
	public Hyperlink donate;
	public Hyperlink contribute;

	public TextFlow textFlow;
	public ScrollPane textScroll;

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

	public void log(String text, Severity severity) {
		SimpleDateFormat sdf = new SimpleDateFormat("hh.mm.ss");
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		Text t = new Text(sdf.format(timestamp) + ": " + text + "\n");
		switch (severity) {
			case INFO:
				log.info(text);
				t.setFill(javafx.scene.paint.Paint.valueOf("White"));
				break;
			case WARN:
				log.warn(text);
				t.setFill(javafx.scene.paint.Paint.valueOf("Yellow"));
				break;
			case ERROR:
				log.error(text);
				t.setFill(javafx.scene.paint.Paint.valueOf("Red"));
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
