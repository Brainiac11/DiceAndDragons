package src.networking;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class GameServer {
    private ServerSocket serverSocket;
    private ArrayList<GameSubscriber> clients = new ArrayList<>();
    private ArrayList<PlayerInfo> players = new ArrayList<>();
    private ArrayList<String> chatLog = new ArrayList<>();
    private String hostHandle;

    public GameServer(String hostHandle) throws IOException {
        this.hostHandle = hostHandle.trim();
        serverSocket = new ServerSocket(0);
        players.add(new PlayerInfo(this.hostHandle));
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    public synchronized LobbyState getCurrentLobbyState() {
        boolean allReady = !players.isEmpty();
        for (PlayerInfo p : players) {
            if (!p.ready) {
                allReady = false;
                break;
            }
        }

        ArrayList<PlayerInfo> copyPlayers = new ArrayList<>();
        for (PlayerInfo p : players) {
            PlayerInfo cp = new PlayerInfo(p.handle);
            cp.hero = p.hero;
            cp.ready = p.ready;
            copyPlayers.add(cp);
        }

        return new LobbyState(copyPlayers, new ArrayList<>(chatLog), allReady);
    }

    public synchronized void setHostHero(String hero) {
        for (PlayerInfo p : players) {
            if (p.handle.equals(hostHandle)) {
                p.hero = hero;
                p.ready = isRealHero(hero);
                break;
            }
        }
        broadcastLobbyState();
    }

    public synchronized void sendHostChatMessage(String m) {
        m = m.trim();
        if (m.isEmpty()) {
            return;
        }
        System.out.println(m + " " + hostHandle);
        chatLog.add(hostHandle + ": " + m);
        broadcastLobbyState();
    }

    public synchronized void startGame() {
        chatLog.add("*****Host Started the Game***");
        broadcastLobbyState();
    }

    public void startListening() {
        Thread acceptThread = new Thread(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    Socket s = serverSocket.accept();
                    Thread t = new Thread(() -> handleTheNewClient(s));
                    t.setDaemon(true);
                    t.start();
                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        e.printStackTrace();
                    }
                }
            }
        });
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    private void handleTheNewClient(Socket wetSock) {
        GameSubscriber sub = null;
        try {
            sub = new GameSubscriber(wetSock);

            GameMessage msg = (GameMessage) sub.read();
            if (!msg.type.equals(GameMessage.JOIN_REQUEST)) {
                sub.send(new GameMessage(GameMessage.JOIN_REJECTED, "wanted a join request, wasn't sent "));
                wetSock.close();
                return;
            }

            String handle = msg.text.trim();
            if (handle.isEmpty()) {
                sub.send(new GameMessage(GameMessage.JOIN_REJECTED, "empty hangle warning"));
                wetSock.close();
                return;
            }

            if (isTheHandleAlreadyTaken(handle)) {
                sub.send(new GameMessage(GameMessage.JOIN_REJECTED, "Handle \"" + handle + "\" is already taken."));
                wetSock.close();
                return;
            }

            sub.handle = handle;
            clients.add(sub);
            players.add(new PlayerInfo(handle));

            sub.send(new GameMessage(GameMessage.JOIN_ACCEPTED, "Connected to lobby.", getCurrentLobbyState()));
            broadcastLobbyState();

            // holy bandaid ts code sucks
            while (!wetSock.isClosed()) {
                GameMessage in = (GameMessage) sub.read();
                if (in.type.equals(GameMessage.HERO_SELECT)) {
                    changeHeroe(handle, in.text);
                    broadcastLobbyState();
                } else if (in.type.equals(GameMessage.CHAT)) {
                    String cleanMsg = in.text == null ? "" : in.text.trim(); // 🤤
                    if (!cleanMsg.isEmpty()) {
                        chatLog.add(handle + ": " + cleanMsg);
                        broadcastLobbyState();
                    }
                }
            }
        } catch (Exception e) {
            // smth happened with client but miht jsut remeove this accc such bad code
            e.printStackTrace();
        } finally {
            if (sub != null && sub.handle != null) {
                removeClient(sub);
            }
            try {
                wetSock.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized boolean isTheHandleAlreadyTaken(String handle) {
        // 1st is thee worst 2nd is the best
        if (handle.equalsIgnoreCase(hostHandle)) {
            return true;
        }
        for (PlayerInfo p : players) {
            if (p.handle.equalsIgnoreCase(handle)) {
                return true;
            }
        }
        return false;
    }

    private synchronized void changeHeroe(String handle, String hero) {
        for (PlayerInfo p : players) {
            if (p.handle.equals(handle)) {
                p.hero = hero;
                p.ready = isRealHero(hero);
                return;
            }
        }
    }

    private boolean isRealHero(String hero) {
        if (hero == null) {
            return false;
        }
        String h = hero.trim();
        if (h.isEmpty()) {
            return false;
        }
        // GRammaer is hard
        if (h.equals("Select an Hero") || h.equals("Select a Hero")) {
            return false;
        }
        return true;
    }

    private synchronized void removeClient(GameSubscriber sub) {
        clients.remove(sub);
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).handle.equals(sub.handle)) {
                players.remove(i);
                break;
            }
        }
        broadcastLobbyState();
    }

    // ALLLLLAHHHHHHHHHHHH
    private synchronized void broadcastLobbyState() {
        LobbyState state = getCurrentLobbyState();
        for (GameSubscriber client : clients) {
            try {
                client.send(new GameMessage(GameMessage.LOBBY_UPDATE, state));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void close() {
        //sok 🫠
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (GameSubscriber c : clients) {
            try {
                c.socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
