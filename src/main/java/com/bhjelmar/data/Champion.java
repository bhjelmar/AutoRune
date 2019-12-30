package com.bhjelmar.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Champion implements Serializable {

	private static final long serialVersionUID = 3411487633492707912L;

	//	Data from http://ddragon.leagueoflegends.com/cdn/LOL_VERSION/data/en_US/championFull.json
	private int championId;
	private String name;
	private int skinNum;
	private String role;

}
