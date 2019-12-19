package com.bhjelmar.data;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class Champion implements Serializable {

	//	Data from http://ddragon.leagueoflegends.com/cdn/LOL_VERSION/data/en_US/championFull.json
	private int championId;
	private String name;
	private int skinNum;
	private String role;

}
