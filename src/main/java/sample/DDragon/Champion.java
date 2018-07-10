package sample.DDragon;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data @NoArgsConstructor
public class Champion {

//	Data from http://ddragon.leagueoflegends.com/cdn/LOL_VERSION/data/en_US/championFull.json
	private int championId;
	private String name;
	private List<String> skins;

//	Data from http://api.champion.gg/v2/champions?champData=hashes&limit=1000&api_key=API_KEY
	private Map<String, List<String>> mostFrequentRoleRuneMap = new HashMap<>();
	private Map<String, List<String>> highestWinRoleRuneMap = new HashMap<>();

	public void addToMostFrequentRuneRoleMap(String role, List<String> runes) {
		mostFrequentRoleRuneMap.put(role, runes);
	}

	public void addToHighestWinRuneRoleMap(String role, List<String> runes) {
		highestWinRoleRuneMap.put(role, runes);
	}

}
