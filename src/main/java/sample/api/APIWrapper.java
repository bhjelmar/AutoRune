package sample.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import sample.data.Champion;
import sample.data.Rune;
import sample.data.RunePage;
import sample.data.RuneTree;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.*;

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

public class APIWrapper {

	private static final String cggAPIKey = "5ba6c2b143952b70050ef6c312584c96";
	private final Logger logger = Logger.getLogger(this.getClass());
	private String remotingAuthToken;
	private String port;
	private String pid;

	private String currentLOLVersion;

	@Getter
	private Map<Integer, Champion> idChampionMap = new HashMap<>();
	@Getter
	private Map<String, Integer> skinIdMap = new HashMap<>();
	@Getter
	private List<RuneTree> runeTrees = new ArrayList<>();

	public APIWrapper() {
		createFakeTrustManager();

		Runtime runtime = Runtime.getRuntime();
		try {
			currentLOLVersion = getCurrentLOLVersion();
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
				logger.error("Cannot find LeagueClientUx pid. Is league process running?");
			} else if(remotingAuthToken == null) {
				logger.error("Cannot find LeagueClientUx remoting-auth-token.");
			} else if(port == null) {
				logger.error("Cannot find LeagueClientUx port.");
			}
			bufferedreader.close();
		} catch(IOException e) {
			logger.error(e.getLocalizedMessage());
		}
	}

	public Champion getChampionBySkinName(String skinName) {
		logger.info("Getting champion object from skin: " + skinName);
		return idChampionMap.get(skinIdMap.get(skinName));
	}

	public boolean updateData() {
		if(new File("idChampionMap.ser").isFile() && new File("skinIdMap.ser").isFile()) {
			logger.info("Champion.gg rune info and champion skin info found locally.");
			idChampionMap = deserializeData("idChampionMap.ser");
			skinIdMap = deserializeData("skinIdMap.ser");
		} else {
			logger.info("Champion.gg rune info not found locally.");
			getChampSkinList();
			getCGGRunes();
			serializeData(idChampionMap, "idChampionMap.ser");
			serializeData(skinIdMap, "skinIdMap.ser");
		}
		if(new File("runeTrees.ser").isFile()) {
			logger.info("Rune info from Riot found locally.");
			runeTrees = deserializeData("runeTrees.ser");
		} else {
			logger.info("Rune info from Riot not found locally.");
			getAllRunes();
			serializeData(runeTrees, "runeTrees.ser");
		}

		if(idChampionMap == null || skinIdMap == null || runeTrees == null) {
			return false;
		}
		return true;
	}

	private void serializeData(Object o, String name) {
		try {
			FileOutputStream fos = new FileOutputStream(name);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(o);
			oos.close();
			fos.close();
			logger.info("Serialized data is saved in " + name + ".");
		} catch(IOException e) {
			logger.error(e.getLocalizedMessage());
		}
	}

	private <T> T deserializeData(String fileName) {
		T obj;
		try {
			FileInputStream fis = new FileInputStream(fileName);
			ObjectInputStream ois = new ObjectInputStream(fis);
			obj = (T) ois.readObject();
			ois.close();
			fis.close();
		} catch(IOException e) {
			logger.error(e.getLocalizedMessage());
			return null;
		} catch(ClassNotFoundException e) {
			logger.error(e.getLocalizedMessage());
			return null;
		}
		logger.info("Deserialized " + fileName + " successfully.");
		return obj;
	}

	public List<RunePage> getPages() {
		StringBuffer stringBuffer = makeHTTPCall("https://127.0.0.1:" + port + "/lol-perks/v1/pages", "GET");
		ObjectMapper mapper = new ObjectMapper();
		List<RunePage> pageList = null;
		try {
			pageList = mapper.readValue(stringBuffer.toString(), mapper.getTypeFactory().constructCollectionType(
					List.class, RunePage.class));
		} catch(IOException e) {
			e.printStackTrace();
		}
		return pageList;
	}

	private void getAllRunes() {
		String stringUrl = "http://ddragon.leagueoflegends.com/cdn/" + currentLOLVersion + "/data/en_US/runesReforged.json";
		logger.info("Getting rune information from Riot: " + stringUrl);
		StringBuffer result = makeHTTPCall(stringUrl, "GET");
		JSONArray jsonArray = new JSONArray(result.toString());

		for(Object runeTreeTemp : jsonArray) {
			if(runeTreeTemp instanceof JSONObject) {
				JSONObject runeTreeJSONObject = (JSONObject) runeTreeTemp;
				JSONArray slots = (JSONArray) runeTreeJSONObject.get("slots");
				List<Rune> runeTreeRunes = new ArrayList<>();
				for(Object slotTemp : slots) {
					if(slotTemp instanceof JSONObject) {
						JSONObject slotJSONObject = (JSONObject) slotTemp;
						JSONArray runesJSONArray = (JSONArray) slotJSONObject.get("runes");
						for(Object runeTemp : runesJSONArray) {
							if(runeTemp instanceof JSONObject) {
								JSONObject runeJSON = (JSONObject) runeTemp;
								Rune rune = new Rune((String) runeJSON.get("name"), (Integer) runeJSON.get("id"));
								runeTreeRunes.add(rune);
							}
						}
					}
				}
				RuneTree runeTree = new RuneTree((String) runeTreeJSONObject.get("name"),
						(Integer) runeTreeJSONObject.get("id"),
						runeTreeRunes);
				runeTrees.add(runeTree);
			}
		}
	}

	private void getChampSkinList() {
		String stringUrl = "http://ddragon.leagueoflegends.com/cdn/" + currentLOLVersion + "/data/en_US/championFull.json";
		logger.info("Getting champion skin list from Riot: " + stringUrl);
		StringBuffer result = makeHTTPCall(stringUrl, "GET");
		JSONObject jsonObject = new JSONObject(result.toString());
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

	private void getCGGRunes() {
		String stringUrl = "http://api.champion.gg/v2/champions?champData=hashes&limit=1000&api_key=" + cggAPIKey;
		logger.info("Getting rune information from champion.gg's API: " + stringUrl);
		StringBuffer result = makeHTTPCall(stringUrl, "GET");
		JSONArray jsonArray = new JSONArray(result.toString());
		for(Object o : jsonArray) {
			if(o instanceof JSONObject) {
				JSONObject champJSONObject = (JSONObject) o;
				JSONObject _id = (JSONObject) champJSONObject.get("_id");
				int id = (int) _id.get("championId");
				Champion champion = idChampionMap.get(id);
				String role = (String) _id.get("role");
				role = role.equals("DUO_CARRY") ? "ADC" : role;
				role = role.equals("DUO_SUPPORT") ? "SUPPORT" : role;
				try {
					JSONObject runehash = (JSONObject) ((JSONObject) champJSONObject.get("hashes")).get("runehash");
					List<String> highestCount = Arrays.asList(((String) ((JSONObject) runehash.get("highestCount")).get("hash")).split("-"));
					List<String> highestWinrate = Arrays.asList(((String) ((JSONObject) runehash.get("highestWinrate")).get("hash")).split("-"));
					champion.addToMostFrequentRuneRoleMap(role, highestCount);
					champion.addToHighestWinRuneRoleMap(role, highestWinrate);
				} catch(Exception e) {
					logger.info("no rune data for " + champion.getName() + " " + role);
				}
			}
		}
	}

	private String getCurrentLOLVersion() throws IOException {
		String stringUrl = "https://ddragon.leagueoflegends.com/api/versions.json";
		StringBuffer result = makeHTTPCall(stringUrl, "GET");
		List<String> items = Arrays.asList(result.toString().replaceAll("\"", "").replaceAll("^.|.$", "").split("\\s*,\\s*"));
		if(items.get(0) == null) {
			logger.error("could not get current LoL version");
			return null;
		}
		return items.get(0);
	}

	private StringBuffer makeHTTPCall(String stringUrl, String method) {
		StringBuffer content = null;
		try {
			URL url = new URL(stringUrl);
			try {
				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.setRequestMethod(method);

//				TODO: this is a bit lazy...
				String encoded = Base64.getEncoder().encodeToString(("riot" + ":" + remotingAuthToken).getBytes(StandardCharsets.UTF_8));
				con.setRequestProperty("Authorization", "Basic " + encoded);

				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
				String inputLine;
				content = new StringBuffer();
				while((inputLine = in.readLine()) != null) {
					content.append(inputLine);
				}
				in.close();
			} catch(IOException e) {
				logger.error(e.getLocalizedMessage());
			}
		} catch(MalformedURLException e) {
			logger.error(e.getLocalizedMessage());
		}
		return content;
	}

	private void createFakeTrustManager() {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] {
				new X509TrustManager() {
					public X509Certificate[] getAcceptedIssuers() {
						return null;
					}

					public void checkClientTrusted(X509Certificate[] certs, String authType) {
					}

					public void checkServerTrusted(X509Certificate[] certs, String authType) {
					}
				}
		};
		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch(Exception e) {
			logger.error(e.getLocalizedMessage());
		}
	}

}