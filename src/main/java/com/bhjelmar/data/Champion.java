package com.bhjelmar.data;

import lombok.Data;

import java.io.Serializable;

@Data
public class Champion implements Serializable {

	private static final long serialVersionUID = 3411487633492707912L;

	//	Data from http://ddragon.leagueoflegends.com/cdn/LOL_VERSION/data/en_US/championFull.json
	private int championId;
	private String name;
	private int skinNum;
	private String role;

}
