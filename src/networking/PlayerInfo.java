package src.networking;

import java.io.Serializable;
import java.util.ArrayList;

public class PlayerInfo implements Serializable {
    public String handle;
    public String hero;
    public boolean ready;
    public ArrayList<String> purchasedItems;
    public int currentHitPoints;

    public PlayerInfo(String handle) {
        this.handle = handle;
        this.hero = null;
        this.ready = false;
        this.purchasedItems = new ArrayList<>();
        this.currentHitPoints = -1;
    }
}
