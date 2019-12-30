package com.bhjelmar.api;

import com.bhjelmar.data.RunePage;
import com.bhjelmar.ui.StartupController;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.sun.javafx.PlatformUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.tuple.Pair.of;

@Log4j2
public class LoLClientAPI {

	private String remotingAuthToken;
	private String port;
	@Getter
	private String pid;

	public Pair<String, StartupController.Severity> setLoLClientInfo() {
		if (PlatformUtil.isWindows()) {
			return setLoLClientInfo(
				"LeagueClientUx.exe",
				System.getenv().get("SystemRoot") + "\\System32\\wbem\\WMIC.exe process where name='leagueclientux.exe' get commandline");
		} else {
			String[] cmd = {
				"/bin/sh",
				"-c",
				"ps -ef | grep \"League of Legends.app/Contents/MacOS/LeagueClientUx\" | grep -v \"grep\""
			};
			return setLoLClientInfo(
				"LeagueClientUx",
				cmd);
		}
	}

	private Pair<String, StartupController.Severity> setLoLClientInfo(String processName, String... command) {
		Runtime runtime = Runtime.getRuntime();
		Pair<String, StartupController.Severity> message;
		try {
			Process proc = runtime.exec(command);
			try (InputStream inputstream = proc.getInputStream();
				 InputStreamReader inputstreamreader = new InputStreamReader(inputstream, StandardCharsets.UTF_8);
				 BufferedReader bufferedreader = new BufferedReader(inputstreamreader)
			) {
				String line;
				while ((line = bufferedreader.readLine()) != null) {
					if (line.contains(processName)) {
						int beginningOfToken = line.indexOf("--remoting-auth-token=") + "--remoting-auth-token=".length();
						int endOfToken;
						if (PlatformUtil.isWindows()) {
							endOfToken = line.indexOf("\"", beginningOfToken);
						} else { // Mac or Unix
							endOfToken = line.indexOf(" ", beginningOfToken);
						}
						remotingAuthToken = line.substring(beginningOfToken, endOfToken);

						int beginningOfPort = line.indexOf("--app-port=") + "--app-port=".length();
						int endOfPort;
						if (PlatformUtil.isWindows()) {
							endOfPort = line.indexOf("\"", beginningOfPort);
						} else { // Mac or Unix
							endOfPort = line.indexOf(" ", beginningOfPort);
						}
						port = line.substring(beginningOfPort, endOfPort);

						int beginningOfPid = line.indexOf("--app-pid=") + "--app-pid=".length();
						int endOfPid;
						if (PlatformUtil.isWindows()) {
							endOfPid = line.indexOf("\"", beginningOfPid);
						} else { // Mac or Unix
							endOfPid = line.indexOf(" ", beginningOfPid);
						}
						pid = line.substring(beginningOfPid, endOfPid);
					}
				}
				if (pid == null) {
					message = of("Cannot find LeagueClientUx pid. Is league process running?", StartupController.Severity.DEBUG);
				} else if (remotingAuthToken == null) {
					message = of("Cannot find LeagueClientUx remoting-auth-token.", StartupController.Severity.ERROR);
				} else if (port == null) {
					message = of("Cannot find LeagueClientUx port.", StartupController.Severity.ERROR);
				} else {
					message = of("Found LeagueClientUx process", StartupController.Severity.INFO);
				}
			}
		} catch (IOException e) {
			message = of(e.getLocalizedMessage(), StartupController.Severity.ERROR);
		}
		return message;
	}

	public List<RunePage> getPages() {
		HttpResponse<String> response;
		try {
			log.debug(API.GET_RUNE_PAGE.getPath().replaceAll("\\{port}", port));
			response = Unirest.get(API.GET_RUNE_PAGE.getPath())
				.routeParam("port", port)
				.basicAuth("riot", remotingAuthToken)
				.asString();
			if (response.getStatus() != 200) {
				log.error(response.getStatusText());
			} else {
				Gson gson = new GsonBuilder().create();
				return Arrays.asList(gson.fromJson(response.getBody(), RunePage[].class));
			}
		} catch (UnirestException e) {
			log.error(e.getLocalizedMessage(), e);
		}
		return new ArrayList<>();
	}

	public boolean replacePage(long pageIdToReplace, RunePage runePage) {
		HttpResponse<String> response;
		try {
			response = Unirest.delete(API.DELETE_RUNE_PAGE.getPath())
				.routeParam("pageId", String.valueOf(pageIdToReplace))
				.routeParam("port", port)
				.basicAuth("riot", remotingAuthToken)
				.asString();
			if (response.getStatus() != 204) {
				log.error(response.getStatusText());
			} else {
				log.debug("deleted old page");
				response = Unirest.post(API.POST_RUNE_PAGE.getPath())
					.header("Content-Type", "application/json")
					.basicAuth("riot", remotingAuthToken)
					.routeParam("port", port)
					.body(new Gson().toJson(runePage))
					.asString();
				if (response.getStatus() != 200) {
					log.error(response.getStatusText());
				} else {
					log.info("Successfully replaced Rune Page");
					return true;
				}
			}
		} catch (UnirestException e) {
			log.error(e.getLocalizedMessage(), e);
		}
		return false;
	}

	@Getter
	@AllArgsConstructor
	private enum API {
		GET_RUNE_PAGE("https://127.0.0.1:{port}/lol-perks/v1/pages"),
		POST_RUNE_PAGE("https://127.0.0.1:{port}/lol-perks/v1/pages"),
		DELETE_RUNE_PAGE("https://127.0.0.1:{port}/lol-perks/v1/pages/{pageId}");
		private final String path;
	}

}
