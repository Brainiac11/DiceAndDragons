package src.networking;

import src.Constants;

public class GameNetworkManager extends AbstractNetworkManager {
    public static GameNetworkManager instance;

    public static GameNetworkManager getInstance() {
        if (instance == null) {
            instance = new GameNetworkManager();
        }
        return instance;
    }

    private GameNetworkManager() {
        super(Constants.kGameManagerInitialPort);
    }

}
