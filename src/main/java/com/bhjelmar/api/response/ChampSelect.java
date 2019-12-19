package com.bhjelmar.api.response;

import lombok.Data;

import java.util.List;

@Data
public class ChampSelect {

	private List<List<ActionsBean>> actions;
	private List<MyTeamBean> myTeam;

	@Data
	public static class ActionsBean {
		private int actorCellId;
		private int championId;
		private boolean completed;
		private int id;
		private int pickTurn;
		private String type;
	}

	@Data
	public static class MyTeamBean {
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
