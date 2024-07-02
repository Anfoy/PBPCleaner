package me.antcode;

public class Player {

    private final String team;

    private final int playerID;

    public Player(int playerID, String team) {
        this.playerID = playerID;
        this.team = team;
    }

    public String getTeam() {
        return team;
    }


    public int getPlayerID() {
        return playerID;
    }
}
