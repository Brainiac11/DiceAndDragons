package src.networking;

import java.io.Serializable;

public class GameMessage implements Serializable {
    public static final String JOIN_REQUEST = "JOIN_REQUEST";
    public static final String JOIN_ACCEPTED = "JOIN_ACCEPTED";
    public static final String JOIN_REJECTED = "JOIN_REJECTED";
    public static final String LOBBY_UPDATE = "LOBBY_UPDATE";
    public static final String HERO_SELECT = "HERO_SELECT";
    public static final String CHAT = "CHAT";
    public static final String START = "START";
    public static final String OPEN_GAME_SCREEN = "OPEN_GAME_SCREEN";
    public static final String BUY_ITEM = "BUY_ITEM";
    public static final String SELL_ITEM = "SELL_ITEM";
    public static final String SELECT_DRAGON = "SELECT_DRAGON";
    public static final String DICE_ROLL = "DICE_ROLL";
    public static final String DICE_PLACE = "DICE_PLACE";
    public static final String DICE_REMOVE = "DICE_REMOVE";
    public static final String SKILL_USED = "SKILL_USED";

    public String type;
    public String text;
    public LobbyState lobbyState;
    public int dieIndex = -1;
    public int skillIndex = -1;
    public int symbolIndex = -1;
    public int[] diceIndices;
    public boolean skillUsed;

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

    public GameMessage(String type, int[] diceIndices) {
        this.type = type;
        this.diceIndices = diceIndices;
    }

    public GameMessage(String type, int dieIndex, int skillIndex, int symbolIndex) {
        this.type = type;
        this.dieIndex = dieIndex;
        this.skillIndex = skillIndex;
        this.symbolIndex = symbolIndex;
    }

    public GameMessage(String type, int skillIndex, boolean skillUsed) {
        this.type = type;
        this.skillIndex = skillIndex;
        this.skillUsed = skillUsed;
    }
}
