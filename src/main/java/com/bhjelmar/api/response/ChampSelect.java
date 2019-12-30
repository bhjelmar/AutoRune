package com.bhjelmar.api.response;

import lombok.Data;

import java.util.List;

@Data
public class ChampSelect {

	private List<List<Actions>> actions;
	private List<MyTeam> myTeam;

	@Data
	public static class Actions {
		private int actorCellId;
		private int championId;
		private boolean completed;
		private int id;
		private int pickTurn;
		private String type;
	}

	@Data
	public static class MyTeam {
		private String assignedPosition;
		private int cellId;
		private int championId;
		private int championPickIntent;
		private String entitledFeatureType;
		private String playerType;
		private int selectedSkinId;
		private int spell1Id;
		private int spell2Id;
		private int summonerId;
		private int team;
		private int wardSkinId;
	}
}
