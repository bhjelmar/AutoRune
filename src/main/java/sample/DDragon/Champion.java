package sample.DDragon;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data @AllArgsConstructor
public class Champion {

	int championId;
	String role;

//	Map<String, List<String>> roleRuneMap;
	List<String> mostFrequentRunes;
	List<String> highestWinRunes;

	String name;
	List<String> skins;

}
