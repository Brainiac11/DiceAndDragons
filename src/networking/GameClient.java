package src.networking;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class GameClient {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public LobbyState connect(String ip, int port, String handle) throws Exception {
        socket = new Socket();
        socket.connect(new InetSocketAddress(ip, port));

        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();

        in = new ObjectInputStream(socket.getInputStream());

        out.writeObject(new GameMessage(GameMessage.JOIN_REQUEST, handle));
        out.flush();

        GameMessage response = (GameMessage) in.readObject();
        // System.out.println("resonones: " + response.type);
        if (response.type.equals(GameMessage.JOIN_REJECTED)) {
            socket.close();
            throw new Exception(response.text == null || response.text.isBlank() ? "Connection failed" : response.text);
        }
        return response.lobbyState;
    }

    public LobbyState readUpdateToLobby() throws IOException, ClassNotFoundException {
        while (true) {
            GameMessage msg = (GameMessage) in.readObject();
            if (msg != null && GameMessage.LOBBY_UPDATE.equals(msg.type)) {
                return msg.lobbyState;
            }
        }
    }

    public GameMessage readUpdateToGameMessage() throws IOException, ClassNotFoundException {
        while (true) {
            GameMessage msg = (GameMessage) in.readObject();
            if (msg != null) {
                return msg;
            }
        }
    }

    public void selectHero(String hero) throws IOException {
        out.writeObject(new GameMessage(GameMessage.HERO_SELECT, hero));
        System.out.println("hero selected: " + hero);
        out.flush();
    }

    public void sendChatMessage(String message) throws IOException {
        out.writeObject(new GameMessage(GameMessage.CHAT, message));
        System.out.println(message);
        out.flush();
    }

    public void buyItem(String itemName) throws IOException {
        out.writeObject(new GameMessage(GameMessage.BUY_ITEM, itemName));
        System.out.println("buying item: " + itemName);
        out.flush();
    }

    public void sellItem(String itemName) throws IOException {
        out.writeObject(new GameMessage(GameMessage.SELL_ITEM, itemName));
        System.out.println("selling item: " + itemName);
        out.flush();
    }

    public void selectBoss(String bossName) throws IOException {
        out.writeObject(new GameMessage(GameMessage.SELECT_DRAGON, bossName));
        System.out.println("selecting dragon: " + bossName);
        out.flush();
    }

    public void rollDice(int[] diceIndices) throws IOException {
        out.writeObject(new GameMessage(GameMessage.DICE_ROLL, diceIndices));
        out.flush();
    }

    public void placeDie(int dieIndex, int skillIndex, int symbolIndex) throws IOException {
        out.writeObject(new GameMessage(GameMessage.DICE_PLACE, dieIndex, skillIndex, symbolIndex));
        out.flush();
    }

    public void removeDie(int dieIndex) throws IOException {
        out.writeObject(new GameMessage(GameMessage.DICE_REMOVE, dieIndex, -1, -1));
        out.flush();
    }

    public void setSkillUsed(int skillIndex, boolean skillUsed) throws IOException {
        out.writeObject(new GameMessage(GameMessage.SKILL_USED, skillIndex, skillUsed));
        out.flush();
    }

    public void sendGameAction(GameMessage message) throws IOException {
        if (message == null) {
            return;
        }
        out.writeObject(message);
        out.flush();
    }

    public void close() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
