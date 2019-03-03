package com.bhjelmar.api;

import com.bhjelmar.data.Champion;
import com.bhjelmar.data.RunePage;
import com.bhjelmar.data.RuneSelection;
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
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

/*
always show last date of data fetch with update button beside
if no serialized champ - rune list exists:
	automatically fetch and serialize
if serialized list is > 2 weeks old:
	prompt for update
eventlistener for champion lock in:
	get champion they are playing and their assigned role (will pull in all roles from champion.gg but will smart complete to their assigned role)
	if AutoRune page doesn't exist:
		if user has no available rune page slots:
			ask them if/which they would like to overwrite, exit if they choose none
		else
			create AutoRune page
	else
		deserialize champion - rune list
		show them all options for their champ
		apply user selection
 */
//TODO: serialize/deserialize mechanism for maps
//TODO: champion select lock in listener
//TODO: get selected champ and role
//TODO: delete rune page
//TODO: post rune page
//TODO: get list of all current rune pages
//TODO: UI... lmao

@Log4j2
public class APIWrapper {

	private String remotingAuthToken;
	private String port;
	@Getter
	private String pid;

	private String currentLOLVersion;

	@Getter
	private Pair<String, Map<Integer, Champion>> versionedIdChampionMap;
	@Getter
	private Pair<String, Map<String, Integer>> versionedSkinIdMap;

	public APIWrapper() {
		// disable cert checking
		SSLContext sslcontext = null;
		try {
			sslcontext = SSLContexts.custom()
				.loadTrustMaterial(null, new TrustSelfSignedStrategy())
				.build();
		} catch(NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
			log.error(e.getStackTrace());
		}
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext);
		BasicCookieStore cookieStore = new org.apache.http.impl.client.BasicCookieStore();
		CloseableHttpClient httpclient = HttpClients.custom()
			.setSSLSocketFactory(sslsf)
			.setDefaultCookieStore(cookieStore)
			.build();
		Unirest.setHttpClient(httpclient);


		// Override default UniRest mapper for use with custom POJOs
		Unirest.setObjectMapper(new ObjectMapper() {
			private com.fasterxml.jackson.databind.ObjectMapper jacksonObjectMapper
				= new com.fasterxml.jackson.databind.ObjectMapper();

			public <T> T readValue(String value, Class<T> valueType) {
				try {
					return jacksonObjectMapper.readValue(value, valueType);
				} catch(IOException e) {
					throw new RuntimeException(e);
				}
			}

			public String writeValue(Object value) {
				try {
					return jacksonObjectMapper.writeValueAsString(value);
				} catch(JsonProcessingException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	public boolean getStaticData() {
		currentLOLVersion = getCurrentLOLVersion();
		boolean updateData = true;
		if(new File("versionedIdChampionMap.ser").isFile() && new File("versionedSkinIdMap.ser").isFile()) {
			versionedIdChampionMap = Files.deserializeData("versionedIdChampionMap.ser");
			versionedSkinIdMap = Files.deserializeData("versionedSkinIdMap.ser");
			if(versionedIdChampionMap.getLeft().equals(currentLOLVersion) && versionedSkinIdMap.getLeft().equals(currentLOLVersion)) {
				updateData = false;
			}
		}
		if(updateData) {
			log.info("updating op.gg rune info and champion skin info for patch {}", currentLOLVersion);
			getChampionSkinsAndIDs();
			//TODO: delete old files
			Files.serializeData(versionedIdChampionMap, "versionedIdChampionMap.ser");
			Files.serializeData(versionedSkinIdMap, "versionedSkinIdMap.ser");
		} else {
			log.info("op.gg rune info and champion skin info already up to date for patch {}", currentLOLVersion);
		}
		if(versionedIdChampionMap == null || versionedSkinIdMap == null) {
			return false;
		}
		return true;
	}

	public void setLoLClientInfo() {
		Runtime runtime = Runtime.getRuntime();
		try {
			Process proc = runtime.exec(System.getenv().get("SystemRoot") + "\\System32\\wbem\\WMIC.exe process where name='leagueclientux.exe' get commandline");
			InputStream inputstream = proc.getInputStream();
			InputStreamReader inputstreamreader = new InputStreamReader(inputstream, "UTF-8");
			BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
			String line;
			while((line = bufferedreader.readLine()) != null) {
				if(line.contains("LeagueClientUx.exe")) {
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
			if(pid == null) {
				log.debug("Cannot find LeagueClientUx pid. Is league process running?");
			} else if(remotingAuthToken == null) {
				log.error("Cannot find LeagueClientUx remoting-auth-token.");
			} else if(port == null) {
				log.error("Cannot find LeagueClientUx port.");
			} else {
				log.info("Found LeagueClientUx with pid {} port {} auth {}", pid, port, remotingAuthToken);
			}
			bufferedreader.close();
		} catch(IOException e) {
			log.error(e.getStackTrace());
		}
	}

	public Champion getChampionBySkinName(String skinName) {
		log.debug("Getting champion object from skin: " + skinName);
		return versionedIdChampionMap.getRight().get(versionedSkinIdMap.getRight().get(skinName));
	}

	public List<RunePage> getPages() {
		createFakeTrustManager();
		HttpResponse<String> response;
		try {
			log.debug(LoLClient.GET_RUNE_PAGE.getPath());
			response = Unirest.get(LoLClient.GET_RUNE_PAGE.getPath())
				.routeParam("port", port)
				.basicAuth("riot", remotingAuthToken)
				.asString();
			if(response.getStatus() != 200) {
				log.error(response.getStatusText());
			} else {
				Gson gson = new GsonBuilder().create();
				return Arrays.asList(gson.fromJson(response.getBody(), RunePage[].class));
			}
		} catch(UnirestException e) {
			e.printStackTrace();
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
			if(response.getStatus() != 204) {
				log.error(response.getStatusText());
			} else {
				log.debug("deleted old page");
				response = Unirest.post(LoLClient.POST_RUNE_PAGE.getPath())
					.header("Content-Type", "application/json")
					.basicAuth("riot", remotingAuthToken)
					.routeParam("port", port)
					.body(new Gson().toJson(runePage))
					.asString();
				if(response.getStatus() != 200) {
					log.error(response.getStatusText());
				} else {
					log.info("Successfully replaced Rune Page");
					return true;
				}
			}
		} catch(UnirestException e) {
			e.printStackTrace();
		}
		return false;
	}

	private void getChampionSkinsAndIDs() {
		log.debug("Getting skin list from {} " + RiotAPI.SKINS.getPath().replaceAll("\\{currentLOLVersion}", currentLOLVersion));
		HttpResponse<String> response;

		Map<String, Integer> skinIdMap = new HashMap<>();
		Map<Integer, Champion> idChampionMap = new HashMap<>();

		try {
			response = Unirest.get(RiotAPI.SKINS.getPath())
				.routeParam("currentLOLVersion", currentLOLVersion)
				.asString();
			if(response.getStatus() != 200) {
				log.error(response.getStatusText());
			} else {
				JSONObject jsonObject = new JSONObject(response.getBody());
				JSONObject champions = (JSONObject) jsonObject.get("data");

				Iterator keys = champions.keys();
				while(keys.hasNext()) {
					String name = (String) keys.next();
					JSONObject championJSONObject = champions.getJSONObject(name);
					JSONArray skins = (JSONArray) championJSONObject.get("skins");
					int id = Integer.parseInt(championJSONObject.getString("key"));
					List<String> skinsArray = new ArrayList<>();
					for(Object skinTemp : skins) {
						if(skinTemp instanceof JSONObject) {
							JSONObject skinJSONObject = (JSONObject) skinTemp;
							String skinName = skinJSONObject.getString("name");
							if(skinName.equals("default")) {
								skinName = name;
							}
							skinsArray.add(skinName);
							skinIdMap.put(skinName, id);
						}
					}
					Champion champion = new Champion();
					champion.setChampionId(id);
					champion.setName(name);
					champion.setSkins(skinsArray);

					idChampionMap.put(id, champion);
				}
			}
		} catch(UnirestException e) {
			log.error(e.getStackTrace());
		}

		versionedSkinIdMap = Pair.of(currentLOLVersion, skinIdMap);
		versionedIdChampionMap = Pair.of(currentLOLVersion, idChampionMap);
	}

	//	private void pprintRunePage(RunePage runePage) {
////		//primary
////		System.out.println("primary runes");
////		for(RuneTree runeTree : runeTrees) {
////			if(runePage.getPrimaryStyleId() == runeTree.getId()) {
////				System.out.println(runeTree.getName());
//////				for(Rune rune : runeTree.getRunes()) {
//////					if(runePage.getSelectedPerkIds().contains(rune.getId())) {
//////						System.out.println(rune.getName());
//////					}
//////				}
////			}
////		}
////		//secondary
////		System.out.println("secondary runes");
////		for(RuneTree runeTree : runeTrees) {
////			if(runePage.getSubStyleId() == runeTree.getId()) {
////				System.out.println(runeTree.getName());
//////				for(Rune rune : runeTree.getRunes()) {
//////					if(runePage.getSelectedPerkIds().contains(rune.getId())) {
//////						System.out.println(rune.getName());
//////					}
//////				}
////			}
////		}
////	}

//	private void getAllRunes() {
//		log.debug("Getting Runes Reforged from {}", RiotAPI.RUNES_REFORGED.getPath().replaceAll("\\{currentLOLVersion}", currentLOLVersion));
//		HttpResponse<String> response;
//		try {
//			response = Unirest.get(RiotAPI.RUNES_REFORGED.getPath())
//				.routeParam("currentLOLVersion", currentLOLVersion)
//				.asString();
//			if(response.getStatus() != 200) {
//				log.error(response.getStatusText());
//			} else {
//				Gson gson = new GsonBuilder().create();
//				runeTrees = gson.fromJson(response.getBody(), new TypeToken<List<RuneTree>>(){}.getType());
//			}
//		} catch(UnirestException e) {
//			log.error(e.getMessage());
//		}
//	}

//	private void getCGGRunes() {
//		String stringUrl = "http://api.champion.gg/v2/champions?champData=hashes&limit=1000&api_key=" + cggAPIKey;
//		log.info("Getting rune information from champion.gg's API: " + stringUrl);
//		StringBuffer result = makeHTTPCall(stringUrl, "GET", null);
//		JSONArray jsonArray = new JSONArray(result.toString());
//		for(Object o : jsonArray) {
//			if(o instanceof JSONObject) {
//				JSONObject champJSONObject = (JSONObject) o;
//				JSONObject _id = (JSONObject) champJSONObject.get("_id");
//				int id = (int) _id.get("championId");
//				Champion champion = idChampionMap.get(id);
//				String role = (String) _id.get("role");
//				role = role.equals("DUO_CARRY") ? "ADC" : role;
//				role = role.equals("DUO_SUPPORT") ? "SUPPORT" : role;
//				try {
//					JSONObject runehash = (JSONObject) ((JSONObject) champJSONObject.get("hashes")).get("runehash");
//					List<String> highestCount = Arrays.asList(((String) ((JSONObject) runehash.get("highestCount")).get("hash")).split("-"));
//					List<String> highestWinrate = Arrays.asList(((String) ((JSONObject) runehash.get("highestWinrate")).get("hash")).split("-"));
//					champion.addToMostFrequentRuneRoleMap(role, highestCount);
//					champion.addToHighestWinRuneRoleMap(role, highestWinrate);
//				} catch(Exception e) {
//					log.info("no rune data for " + champion.getName() + " " + role);
//				}
//			}
//		}
//	}

	public Map<String, List<RuneSelection>> getOPGGRunes(String championName, long championId) {
		String url = RuneAPI.OPGG_ROLES.getPath().replaceAll("\\{championName}", championName).replaceAll("\\{role}", "mid");
//		url = url.substring(0, url.indexOf("{"));
		// op.gg will autocomplete the url for us but it takes ages to load... if we do every lookup for the champion's mid page the load times are way faster

		Map<String, List<RuneSelection>> roleRuneSelectionMap = new HashMap<>();
		try {
			log.debug("Getting Champion role info from {}", url.substring(0, url.indexOf("/mid/rune")));
			Document doc = Jsoup.connect(url).timeout(0).get();
			log.debug("Finished fetch");
			if(doc.getElementsByClass("champion-stats-position").isEmpty()) {
				log.error("Unable to fetch runes for Champion: {}", championName);
			} else {
				List<String> roles = doc.getElementsByClass("champion-stats-position").first().getElementsByClass("champion-stats-header__position").stream()
					.map(e -> e.attr("data-position"))
					.collect(Collectors.toList());
				for(String role : roles) {
					url = RuneAPI.OPGG_RUNES.getPath().replaceAll("\\{championId}", String.valueOf(championId)).replaceAll("\\{role}", role);
					log.debug("Getting Champion Rune info from {}", url);
					doc = Jsoup.connect(url).timeout(0).get();

					log.debug("Finished fetch");

					Element runesTable = doc.select("table").get(0);
					Elements runesRows = runesTable.select("tr");
					runesRows.remove(0); // header
					for(Element runesRow : runesRows) {
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
						if(roleRuneSelectionMap.get(role) != null) {
							List<RuneSelection> prev = roleRuneSelectionMap.get(role);
							prev.add(runeSelection);
							roleRuneSelectionMap.put(role, prev);
						} else {
							List<RuneSelection> runeList = new ArrayList<>();
							runeList.add(runeSelection);
							roleRuneSelectionMap.put(role, runeList);
						}
						log.debug(championName + ":" + role + ":" + winRate + ":" + pickRate + ":" + runes + ":" + perks);
					}
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
		log.debug("----------");
		return roleRuneSelectionMap;
	}

	private String getCurrentLOLVersion() {
		log.debug("Getting Current LoL version from {}", RiotAPI.VERSION.getPath());
		HttpResponse<String> response;
		try {
			response = Unirest.get(RiotAPI.VERSION.getPath())
				.asString();
			if(response.getStatus() != 200) {
				log.error(response.getStatusText());
			} else {
				Gson gson = new GsonBuilder().create();
				List<String> versionList = gson.fromJson(response.getBody(), new TypeToken<List<String>>() {
				}.getType());
				log.info("Current LoL version is {}", versionList.get(0));
				return versionList.get(0);
			}
		} catch(UnirestException e) {
			log.error(e.getStackTrace());
		}
		return null;
	}

//	private StringBuffer makeHTTPCall(String stringUrl, String method, String body) {
//		StringBuffer content = null;
//		try {
//			System.out.println("HTTP " + method + " " + stringUrl);
//			URL url = new URL(stringUrl);
//			try {
//				HttpURLConnection con = (HttpURLConnection) url.openConnection();
////				con.setConnectTimeout(5000);
//				con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
//				con.setDoOutput(true);
//				con.setDoInput(true);
//				con.setRequestMethod(method);
//
////				TODO: this is a bit lazy to do every time we make a rest call
//				String encoded = Base64.getEncoder().encodeToString(("riot" + ":" + remotingAuthToken).getBytes(StandardCharsets.UTF_8));
//				con.setRequestProperty("Authorization", "Basic " + encoded);
//
//				if(method == "POST") {
//					OutputStream os = con.getOutputStream();
//					os.write(body.getBytes("UTF-8"));
//					os.close();
//				}
//
//				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
//				String inputLine;
//				content = new StringBuffer();
//				while((inputLine = in.readLine()) != null) {
//					content.append(inputLine);
//				}
//				System.out.println("HTTP status response code: " + con.getResponseCode());
//				in.close();
//			} catch(IOException e) {
//				log.error(e.getStackTrace());
//			}
//		} catch(MalformedURLException e) {
//			log.error(e.getStackTrace());
//		}
//		return content;
//	}

	private void createFakeTrustManager() {
		// disable cert checking
		SSLContext sslcontext = null;
		try {
			sslcontext = SSLContexts.custom()
				.loadTrustMaterial(null, new TrustSelfSignedStrategy())
				.build();
		} catch(NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
			log.error(e.getStackTrace());
		}
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext);
		CloseableHttpClient httpclient = HttpClients.custom()
			.setSSLSocketFactory(sslsf)
			.build();
		Unirest.setHttpClient(httpclient);
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