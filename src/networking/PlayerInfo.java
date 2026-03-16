package src.networking;

import java.io.Serializable;

public class PlayerInfo implements Serializable {
    public String handle;
    public String hero;
    public boolean ready;

    public PlayerInfo(String handle) {
        this.handle = handle;
        this.hero = null;
        this.ready = false;
    }
}
