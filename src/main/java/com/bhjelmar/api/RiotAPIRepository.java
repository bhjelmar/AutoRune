package com.bhjelmar.api;

import com.bhjelmar.data.Champion;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Log4j2
public class RiotAPIRepository {

	public static Pair<Pair<String, Map<Integer, Champion>>, Pair<String, Map<Integer, Integer>>> getChampionSkinsAndIDs(String currentLOLVersion) {
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

	public static String getCurrentLOLVersion() {
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

}
