package com.bhjelmar.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.cert.X509Certificate;

@Log4j2
public class APIWrapper {

	@SneakyThrows
	public APIWrapper() {
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


		TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
			public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
			}

			public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
			}

			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		}};
		// disable cert checking
		SSLContext sslcontext = null;

		sslcontext = SSLContext.getInstance("SSL");
		sslcontext.init(null, trustAllCerts, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sslcontext.getSocketFactory());
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext);
		CloseableHttpClient httpclient = HttpClients.custom()
			.setSSLSocketFactory(sslsf)
			.build();
		Unirest.setHttpClient(httpclient);
	}

}
