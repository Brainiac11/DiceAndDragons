package src.networking;

import java.io.Serializable;
import java.util.ArrayList;
import src.dice.DiceEnum;

public class PlayerInfo implements Serializable {
    public String handle;
    public String hero;
    public boolean ready;
    public ArrayList<String> purchasedItems;
    public int currentHitPoints;
    public ArrayList<DiceEnum> bankedDice;

    public PlayerInfo(String handle) {
        this.handle = handle;
        this.hero = null;
        this.ready = false;
        this.purchasedItems = new ArrayList<>();
        this.currentHitPoints = -1;
        this.bankedDice = new ArrayList<>();
    }
}
