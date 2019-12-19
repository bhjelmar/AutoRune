package com.bhjelmar;

import com.bhjelmar.ui.StartupController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {

	private double xOffset = 0;
	private double yOffset = 0;

	public static void main(String[] args) {
		launch();
	}

	public void start(Stage primaryStage) throws Exception {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/startup.fxml"));
		Parent root = loader.load();
		primaryStage.setTitle("AutoRune");
		primaryStage.getIcons().add(new Image("/icon.png"));
		Scene scene = new Scene(root, 450, 700);
		scene.getStylesheets().add("/main.css");
		primaryStage.setResizable(false);
		primaryStage.setScene(scene);
		primaryStage.show();

		((StartupController) loader.getController()).setStage(primaryStage);
	}

}
