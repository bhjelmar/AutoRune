package com.bhjelmar.api;

import com.bhjelmar.data.Champion;
import com.bhjelmar.data.RunePage;
import com.bhjelmar.data.RuneSelection;
import com.bhjelmar.ui.StartupController;
import com.bhjelmar.util.Files;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class APIWrapper {

	private String remotingAuthToken;
	private String port;
	@Getter
	private String pid;

//	private String currentLOLVersion;

//	@Getter
//	private Pair<String, Map<Integer, Champion>> versionedIdChampionMap;
//	@Getter
//	private Pair<String, Map<Integer, Integer>> versionedSkinIdMap;

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

	public Pair<Boolean, Pair<Pair<String, Map<Integer, Champion>>, Pair<String, Map<Integer, Integer>>>> shouldUpdateStaticData(String currentLOLVersion) {
		boolean updateData = true;
		Pair<String, Map<Integer, Champion>> versionedIdChampionMap = null;
		Pair<String, Map<Integer, Integer>> versionedSkinIdMap = null;
		if (new File("versionedIdChampionMap.ser").isFile() && new File("versionedSkinIdMap.ser").isFile()) {
			versionedIdChampionMap = Files.deserializeData("versionedIdChampionMap.ser");
			versionedSkinIdMap = Files.deserializeData("versionedSkinIdMap.ser");
			if (versionedIdChampionMap.getLeft().equals(currentLOLVersion) && versionedSkinIdMap.getLeft().equals(currentLOLVersion)) {
				updateData = false;
			}
		}
		return Pair.of(updateData, Pair.of(versionedIdChampionMap, versionedSkinIdMap));
	}

	public Pair<String, StartupController.Severity> setLoLClientInfo() {
		Runtime runtime = Runtime.getRuntime();
		Pair<String, StartupController.Severity> message;
		try {
			Process proc = runtime.exec(System.getenv().get("SystemRoot") + "\\System32\\wbem\\WMIC.exe process where name='leagueclientux.exe' get commandline");
			InputStream inputstream = proc.getInputStream();
			InputStreamReader inputstreamreader = new InputStreamReader(inputstream, "UTF-8");
			BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
			String line;
			while ((line = bufferedreader.readLine()) != null) {
				if (line.contains("LeagueClientUx.exe")) {
					int beginningOfToken = line.indexOf("--remoting-auth-token=") + "--remoting-auth-token=".length();
					int endOfToken = line.indexOf("\"", beginningOfToken);
					remotingAuthToken = line.substring(beginningOfToken, endOfToken);

					int beginningOfPort = line.indexOf("--app-port=") + "--app-port=".length();
					int endOfPort = line.indexOf("\"", beginningOfPort);
					port = line.substring(beginningOfPort, endOfPort);

					int beginningOfPid = line.indexOf("--app-pid=") + "--app-pid=".length();
					int endOfPid = line.indexOf("\"", beginningOfPid);
					pid = line.substring(beginningOfPid, endOfPid);
				}
			}
			if (pid == null) {
				message = Pair.of("Cannot find LeagueClientUx pid. Is league process running?", StartupController.Severity.DEBUG);
			} else if (remotingAuthToken == null) {
				message = Pair.of("Cannot find LeagueClientUx remoting-auth-token.", StartupController.Severity.ERROR);
			} else if (port == null) {
				message = Pair.of("Cannot find LeagueClientUx port.", StartupController.Severity.ERROR);
			} else {
				message = Pair.of("Found LeagueClientUx process", StartupController.Severity.INFO);
			}
			bufferedreader.close();
		} catch (IOException e) {
			message = Pair.of(e.getLocalizedMessage(), StartupController.Severity.ERROR);
		}
		return message;
	}

	public Champion getChampionBySkinName(Pair<String, Map<Integer, Champion>> versionedIdChampionMap, Pair<String, Map<Integer, Integer>> versionedSkinIdMap, String skinName) {
		log.debug("Getting champion object from skin: " + skinName);
		return versionedIdChampionMap.getRight().get(versionedSkinIdMap.getRight().get(skinName));
	}

	public Champion getChampionById(Pair<String, Map<Integer, Champion>> versionedIdChampionMap, int id) {
		log.debug("Getting champion object by id: " + id);
		return versionedIdChampionMap.getRight().values().stream()
			.filter(e -> e.getChampionId() == id)
			.findFirst().orElse(null);
	}

	public List<RunePage> getPages() {
		HttpResponse<String> response;
		try {
			log.debug(LoLClient.GET_RUNE_PAGE.getPath().replaceAll("\\{port}", port));
			response = Unirest.get(LoLClient.GET_RUNE_PAGE.getPath())
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
			response = Unirest.delete(LoLClient.DELETE_RUNE_PAGE.getPath())
				.routeParam("pageId", String.valueOf(pageIdToReplace))
				.routeParam("port", port)
				.basicAuth("riot", remotingAuthToken)
				.asString();
			if (response.getStatus() != 204) {
				log.error(response.getStatusText());
			} else {
				log.debug("deleted old page");
				response = Unirest.post(LoLClient.POST_RUNE_PAGE.getPath())
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

	public Pair<Pair<String, Map<Integer, Champion>>, Pair<String, Map<Integer, Integer>>> getChampionSkinsAndIDs(String currentLOLVersion) {
		log.debug("Getting skin list from {} " + RiotAPI.SKINS.getPath().replaceAll("\\{currentLOLVersion}", currentLOLVersion));
		HttpResponse<String> response;

		Map<Integer, Integer> skinIdMap = new HashMap<>();
		Map<Integer, Champion> idChampionMap = new HashMap<>();

		try {
			response = Unirest.get(RiotAPI.SKINS.getPath())
				.routeParam("currentLOLVersion", currentLOLVersion)
				.asString();
			if (response.getStatus() != 200) {
				log.error(response.getStatusText());
			} else {
				JSONObject jsonObject = new JSONObject(response.getBody());
				JSONObject champions = (JSONObject) jsonObject.get("data");

				Iterator<String> keys = champions.keys();
				while (keys.hasNext()) {
					String name = keys.next();
					JSONObject championJSONObject = champions.getJSONObject(name);
					JSONArray skins = (JSONArray) championJSONObject.get("skins");
					int champId = Integer.parseInt(championJSONObject.getString("key"));
					for (Object skinTemp : skins) {
						if (skinTemp instanceof JSONObject) {
							JSONObject skinJSONObject = (JSONObject) skinTemp;
							int skinId = Integer.parseInt(skinJSONObject.getString("id"));
							int skinNum = skinJSONObject.getInt("num");

							skinIdMap.put(skinId, skinNum);
						}
					}
					Champion champion = new Champion();
					champion.setChampionId(champId);
					champion.setName(name);

					idChampionMap.put(champId, champion);
				}
			}
		} catch (UnirestException e) {
			log.error(e.getLocalizedMessage(), e);
		}

		return Pair.of(Pair.of(currentLOLVersion, idChampionMap), Pair.of(currentLOLVersion, skinIdMap));
	}

	public Map<String, List<RuneSelection>> getOPGGRunes(Champion champion) {
		String url = RuneAPI.OPGG_ROLES.getPath().replaceAll("\\{championName}", champion.getName()).replaceAll("\\{role}", "mid");
		// op.gg will autocomplete the url for us but it takes ages to load... if we do every lookup for the champion's mid page the load times are way faster

		Map<String, List<RuneSelection>> roleRuneSelectionMap = new HashMap<>();
		try {
			log.debug("Getting Champion role info from {}", url.substring(0, url.indexOf("/mid/rune")));
			Document doc = Jsoup.connect(url).timeout(0).get();
			log.debug("Finished fetch");
			if (doc.getElementsByClass("champion-stats-position").isEmpty()) {
				log.error("Unable to fetch runes for Champion: {}", champion.getName());
			} else {
				List<String> roles = doc.getElementsByClass("champion-stats-position").first().getElementsByClass("champion-stats-header__position").stream()
					.map(e -> e.attr("data-position"))
					.collect(Collectors.toList());
				for (String role : roles) {
					url = RuneAPI.OPGG_RUNES.getPath().replaceAll("\\{championId}", String.valueOf(champion.getChampionId())).replaceAll("\\{role}", role);
					log.debug("Getting Champion Rune info from {}", url);
					doc = Jsoup.connect(url).timeout(0).get();

					log.debug("Finished fetch");

					Element runesTable = doc.select("table").get(0);
					Elements runesRows = runesTable.select("tr");
					runesRows.remove(0); // header
					for (Element runesRow : runesRows) {
						List<String> mainTree = runesRow.getElementsByClass("perk-page__item--mark").stream()
							.map(e -> {
								String imgUrl = e.select("img").first().absUrl("src");
								return imgUrl.substring(imgUrl.lastIndexOf("/") + 1, imgUrl.lastIndexOf("."));
							})
							.collect(Collectors.toList());
						List<String> runes = runesRow.getElementsByClass("perk-page__item--active").stream()
							.map(e -> {
								String imgUrl = e.getElementsByClass("perk-page__item--active").select("img").first().absUrl("src");
								return imgUrl.substring(imgUrl.lastIndexOf("/") + 1, imgUrl.lastIndexOf("."));
							})
							.collect(Collectors.toList());
						List<String> perks = runesRow.getElementsByClass("active").stream()
							.map(e -> {
								String imgUrl = e.select("img").first().absUrl("src");
								return imgUrl.substring(imgUrl.lastIndexOf("/") + 1, imgUrl.lastIndexOf("."));
							})
							.collect(Collectors.toList());

						runes.add(0, mainTree.get(0));
						runes.add(5, mainTree.get(1));
						runes.addAll(perks);

						String pickRateRaw = runesRow.getElementsByClass("champion-stats__table__cell--pickrate").first().text().replaceAll("%", "");
						double pickRate = Double.parseDouble(pickRateRaw.substring(0, pickRateRaw.indexOf(" "))); // has # games we need to parse out
						double winRate = Double.parseDouble(runesRow.getElementsByClass("champion-stats__table__cell--winrate").get(0).text().replaceAll("%", ""));

						RuneSelection runeSelection = new RuneSelection(runesRow, runes, pickRate, winRate);
						if (roleRuneSelectionMap.get(role) != null) {
							List<RuneSelection> prev = roleRuneSelectionMap.get(role);
							prev.add(runeSelection);
							roleRuneSelectionMap.put(role, prev);
						} else {
							List<RuneSelection> runeList = new ArrayList<>();
							runeList.add(runeSelection);
							roleRuneSelectionMap.put(role, runeList);
						}
						log.debug(champion.getName() + ":" + role + ":" + winRate + ":" + pickRate + ":" + runes + ":" + perks);
					}
				}
			}
		} catch (IOException e) {
			log.error(e.getLocalizedMessage(), e);
		}
		return roleRuneSelectionMap;
	}

	public String getCurrentLOLVersion() {
		log.debug("Getting Current LoL version from {}", RiotAPI.VERSION.getPath());
		HttpResponse<String> response;
		try {
			response = Unirest.get(RiotAPI.VERSION.getPath())
				.asString();
			if (response.getStatus() != 200) {
				log.error(response.getStatusText());
			} else {
				Gson gson = new GsonBuilder().create();
				List<String> versionList = gson.fromJson(response.getBody(), new TypeToken<List<String>>() {
				}.getType());
				log.info("Current LoL version is {}", versionList.get(0));
				return versionList.get(0);
			}
		} catch (UnirestException e) {
			log.error(e.getLocalizedMessage(), e);
		}
		return null;
	}

	@Getter
	@AllArgsConstructor
	private enum RiotAPI {
		VERSION("https://ddragon.leagueoflegends.com/api/versions.json"),
		RUNES_REFORGED("https://ddragon.leagueoflegends.com/cdn/{currentLOLVersion}/data/en_US/runesReforged.json"),
		SKINS("http://ddragon.leagueoflegends.com/cdn/{currentLOLVersion}/data/en_US/championFull.json");

		private final String path;
	}

	@Getter
	@AllArgsConstructor
	private enum RuneAPI {
		OPGG_ROLES("https://www.op.gg/champion/{championName}/statistics/{role}/rune"),
		OPGG_RUNES("https://www.op.gg/champion/ajax/statistics/runeList/championId={championId}&position={role}&");
		private final String path;
	}

	@Getter
	@AllArgsConstructor
	private enum LoLClient {
		GET_RUNE_PAGE("https://127.0.0.1:{port}/lol-perks/v1/pages"),
		POST_RUNE_PAGE("https://127.0.0.1:{port}/lol-perks/v1/pages"),
		DELETE_RUNE_PAGE("https://127.0.0.1:{port}/lol-perks/v1/pages/{pageId}");
		private final String path;
	}

}
