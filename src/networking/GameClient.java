package src.networking;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Objects;

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
            throw new Exception("Connecton failes");
        }
        return response.lobbyState;
    }

    public GameMessage readUpdateToLobby() throws IOException, ClassNotFoundException {
        while (true) {
            GameMessage msg = (GameMessage) in.readObject();
            if (msg != null && (GameMessage.LOBBY_UPDATE.equals(msg.type)) || GameMessage.START.equals(Objects.requireNonNull(msg).type)) {
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
