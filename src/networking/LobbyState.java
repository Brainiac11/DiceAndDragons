package src.networking;

import java.io.Serializable;
import java.util.ArrayList;

public class LobbyState implements Serializable {
    public ArrayList<PlayerInfo> players;
    public ArrayList<String> chat;
    public boolean allReady;

    public LobbyState(ArrayList<PlayerInfo> players, ArrayList<String> chat, boolean allReady) {
        this.players = players;
        this.chat = chat;
        this.allReady = allReady;
    }
}
