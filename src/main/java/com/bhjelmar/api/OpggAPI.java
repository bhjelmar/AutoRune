package com.bhjelmar.api;

import com.bhjelmar.data.Champion;
import com.bhjelmar.data.RuneSelection;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
public class OpggAPI {

	public static Map<String, List<RuneSelection>> getOPGGRunes(Champion champion) {
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

	@Getter
	@AllArgsConstructor
	private enum RuneAPI {
		OPGG_ROLES("https://www.op.gg/champion/{championName}/statistics/{role}/rune"),
		OPGG_RUNES("https://www.op.gg/champion/ajax/statistics/runeList/championId={championId}&position={role}&");
		private final String path;
	}

}
