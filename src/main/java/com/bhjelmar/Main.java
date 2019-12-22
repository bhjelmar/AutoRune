package com.bhjelmar;

import com.bhjelmar.ui.StartupController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import lombok.SneakyThrows;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.cert.X509Certificate;

public class Main extends Application {

	public static void main(String[] args) {
		configureUnirest();
		launch();
	}

	/**
	 * Configures default object mapper for Unirest and disables cert verification.
	 */
	@SneakyThrows
	public static void configureUnirest() {
		// Override default UniRest mapper for use with custom POJOs
		Unirest.setObjectMapper(new ObjectMapper() {
			private com.fasterxml.jackson.databind.ObjectMapper jacksonObjectMapper
				= new com.fasterxml.jackson.databind.ObjectMapper();

			public <T> T readValue(String value, Class<T> valueType) {
				try {
					return jacksonObjectMapper.readValue(value, valueType);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			public String writeValue(Object value) {
				try {
					return jacksonObjectMapper.writeValueAsString(value);
				} catch (JsonProcessingException e) {
					throw new RuntimeException(e);
				}
			}
		});

		// disable cert checking for authenticating with the lol client
		TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
			public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
			}

			public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
			}

			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		}};
		SSLContext sslcontext = SSLContext.getInstance("SSL");
		sslcontext.init(null, trustAllCerts, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sslcontext.getSocketFactory());
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext);
		CloseableHttpClient httpclient = HttpClients.custom()
			.setSSLSocketFactory(sslsf)
			.build();
		Unirest.setHttpClient(httpclient);
	}

	public void start(Stage primaryStage) throws Exception {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/startup.fxml"));
		Parent root = loader.load();
		primaryStage.setTitle("AutoRune");
		primaryStage.getIcons().add(new Image("/images/icon.png"));
		Scene scene = new Scene(root, 450, 350);
		scene.getStylesheets().add("/css/main.css");
		primaryStage.setResizable(false);
		primaryStage.setScene(scene);
		((StartupController) loader.getController()).setPrimaryStage(primaryStage);
		primaryStage.show();
		((StartupController) loader.getController()).onWindowLoad();
	}

}
