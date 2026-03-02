package src.networking;

import src.Constants;

public class PlayerNetworkManager extends AbstractNetworkManager {
    public static PlayerNetworkManager instance;

    public static PlayerNetworkManager getInstance() {
        if (instance == null) {
            instance = new PlayerNetworkManager();
        }
        return instance;
    }

    private PlayerNetworkManager() {
        super(Constants.kPlayerManagerInitialPort);
    }

}
