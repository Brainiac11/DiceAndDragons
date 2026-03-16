package src.networking;

import java.io.Serializable;

public class GameMessage implements Serializable {
    public static final String JOIN_REQUEST = "JOIN_REQUEST";
    public static final String JOIN_ACCEPTED = "JOIN_ACCEPTED";
    public static final String JOIN_REJECTED = "JOIN_REJECTED";
    public static final String LOBBY_UPDATE = "LOBBY_UPDATE";
    public static final String HERO_SELECT = "HERO_SELECT";
    public static final String CHAT = "CHAT";

    public String type;
    public String text;
    public LobbyState lobbyState;

    public GameMessage(String type, String text) {
        this.type = type;
        this.text = text;
    }

    public GameMessage(String type, LobbyState lobbyState) {
        this.type = type;
        this.lobbyState = lobbyState;
    }

    public GameMessage(String type, String text, LobbyState lobbyState) {
        this.type = type;
        this.text = text;
        this.lobbyState = lobbyState;
    }
}
