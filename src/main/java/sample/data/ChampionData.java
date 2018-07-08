package sample.data;

import java.util.Map;

public class ChampionData {

	private enum Role {
		TOP,
		JUNGLE,
		Middle,
		ADC,
		Support
	}

	private String name;
	private Map<Role, Rune> roleRunesMap;

}
