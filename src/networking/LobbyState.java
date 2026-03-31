package src.networking;

import java.io.Serializable;
import java.util.ArrayList;

public class LobbyState implements Serializable {
    public ArrayList<PlayerInfo> players;
    public ArrayList<String> chat;
    public boolean allReady;
    public int teamGold;
    public String selectedDragon;

    public LobbyState(ArrayList<PlayerInfo> players, ArrayList<String> chat, boolean allReady) {
        this(players, chat, allReady, 0);
    }

    public LobbyState(ArrayList<PlayerInfo> players, ArrayList<String> chat, boolean allReady, int teamGold) {
        this(players, chat, allReady, teamGold, null);
    }

    public LobbyState(ArrayList<PlayerInfo> players, ArrayList<String> chat, boolean allReady, int teamGold,
            String selectedDragon) {
        this.players = players;
        this.chat = chat;
        this.allReady = allReady;
        this.teamGold = teamGold;
        this.selectedDragon = selectedDragon;
    }
}
