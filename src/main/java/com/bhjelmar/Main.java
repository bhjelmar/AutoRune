package com.bhjelmar;

import com.bhjelmar.ui.BaseController;
import com.bhjelmar.ui.StartupController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
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
		if (args.length > 0 && "DEBUG".equals(args[0])) {
			BaseController.setDebug(true);
		}
		// bug report? Font::loadFont is unable to handle URLs with spaces...
		Font.loadFont(Main.class.getResource("/fonts/Friz-Quadrata-Regular.ttf").toExternalForm(), 18);
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

	public void start(Stage primaryStage) {
		primaryStage.setTitle("AutoRune");
		primaryStage.getIcons().add(new Image("/images/icons/96x96.png"));
		primaryStage.setResizable(false);
		primaryStage.setMaximized(true);

		BaseController.setPrimaryStage(primaryStage);
		StartupController.start(false);
	}
}
