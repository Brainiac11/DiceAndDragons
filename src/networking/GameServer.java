package src.networking;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import src.item.VillageMarketplaceCatalog;
import src.players.DragonCatalog;

public class GameServer {
    private static final int STARTING_GOLD_PER_PLAYER = 10;

    private final ServerSocket serverSocket;
    private final ArrayList<GameSubscriber> clients = new ArrayList<>();
    private final ArrayList<PlayerInfo> players = new ArrayList<>();
    private final ArrayList<String> chatLog = new ArrayList<>();
    private final String hostHandle;
    private int teamGold;
    private boolean gameStarted;
    private String selectedDragon;

    public GameServer(String hostHandle) throws IOException {
        this.hostHandle = hostHandle.trim();
        serverSocket = new ServerSocket(0);
        players.add(new PlayerInfo(this.hostHandle));
        recalculateTeamGold();
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    public synchronized LobbyState getCurrentLobbyState() {
        boolean allReady = areAllPlayersReady();

        ArrayList<PlayerInfo> copyPlayers = new ArrayList<>();
        for (PlayerInfo p : players) {
            PlayerInfo cp = new PlayerInfo(p.handle);
            cp.hero = p.hero;
            cp.ready = p.ready;
            cp.purchasedItems = p.purchasedItems == null ? new ArrayList<>() : new ArrayList<>(p.purchasedItems);
            copyPlayers.add(cp);
        }

        return new LobbyState(copyPlayers, new ArrayList<>(chatLog), allReady, teamGold, selectedDragon);
    }

    public synchronized DragonCatalog.DragonProfile getSelectedDragonProfile() {
        return DragonCatalog.findById(selectedDragon);
    }

    // public synchronized GameMessage getCurrentGameMessage(){
    //
    // GameMessage gm = new GameMessage();
    // }

    private synchronized boolean areAllPlayersReady() {
        if (players.isEmpty()) {
            return false;
        }
        for (PlayerInfo p : players) {
            if (!p.ready) {
                return false;
            }
        }
        return true;
    }

    private synchronized void recalculateTeamGold() {
        if (players.isEmpty()) {
            teamGold = STARTING_GOLD_PER_PLAYER;
            return;
        }
        teamGold = players.size() * STARTING_GOLD_PER_PLAYER;
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

    public synchronized void buyItem(String itemName, String handle) {
        String cleanItem = itemName == null ? "" : itemName.trim();
        VillageMarketplaceCatalog.MarketplaceItem marketItem = VillageMarketplaceCatalog.findItem(selectedDragon,
                cleanItem);
        if (marketItem == null) {
            return;
        }

        int itemCost = marketItem.getCost();
        if (teamGold < itemCost) {
            chatLog.add("No team gold left for " + cleanItem + ".");
            broadcastLobbyState();
            return;
        }

        teamGold = teamGold - itemCost;
        String buyer = handle == null ? "Unknown" : handle.trim();
        if (buyer.isEmpty()) {
            buyer = "Unknown";
        }

        PlayerInfo buyerInfo = findPlayerByHandle(buyer);
        if (buyerInfo != null) {
            buyerInfo.purchasedItems.add(cleanItem);
            buyer = buyerInfo.handle;
        }

        chatLog.add(buyer + " bought " + cleanItem + " for " + itemCost + " gold.");
        broadcastLobbyState();
    }

    public synchronized void sellItem(String itemName, String handle) {
        String cleanItem = itemName == null ? "" : itemName.trim();
        if (cleanItem.isEmpty()) {
            return;
        }

        VillageMarketplaceCatalog.MarketplaceItem marketItem = VillageMarketplaceCatalog.findItem(selectedDragon,
                cleanItem);
        if (marketItem == null) {
            return;
        }

        PlayerInfo sellerInfo = findPlayerByHandle(handle);
        if (!removeFirstPurchasedItem(sellerInfo, cleanItem)) {
            return;
        }

        int refund = marketItem.getCost();
        teamGold = teamGold + refund;
        chatLog.add(sellerInfo.handle + " sold " + cleanItem + " for " + refund + " gold.");
        broadcastLobbyState();
    }

    public synchronized void selectBoss(String bossName) {
        String cleanBoss = bossName == null ? "" : bossName.trim();
        DragonCatalog.DragonProfile selected = DragonCatalog.findBySelection(cleanBoss);
        if (selected == null) {
            return;
        }

        selectedDragon = selected.getId();
        chatLog.add("Dragon selected: " + selected.getDisplayName());
        broadcastLobbyState();
    }

    public synchronized void startGame() {
        if (gameStarted || !areAllPlayersReady()) {
            return;
        }
        gameStarted = true;
        chatLog.add("*****Host Started the Game*****");
        LobbyState startedState = getCurrentLobbyState();
        sendGameMessage(new GameMessage(GameMessage.START, "Game started", startedState));
        broadcastLobbyState();
    }

    public synchronized void openGameScreenForAllPlayers() {
        LobbyState state = getCurrentLobbyState();
        sendGameMessage(new GameMessage(GameMessage.OPEN_GAME_SCREEN, "", state));
    }

    public synchronized void startGameNonHost() {
        System.out.println("STARITNG THE AME");
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

    private void handleTheNewClient(Socket socket) {
        GameSubscriber sub = null;
        try {
            sub = new GameSubscriber(socket);

            Object firstObject = sub.read();
            if (!(firstObject instanceof GameMessage)) {
                sub.send(new GameMessage(GameMessage.JOIN_REJECTED, "wanted a join request, wasn't sent "));
                socket.close();
                return;
            }
            GameMessage msg = (GameMessage) firstObject;
            if (!GameMessage.JOIN_REQUEST.equals(msg.type)) {
                sub.send(new GameMessage(GameMessage.JOIN_REJECTED, "wanted a join request, wasn't sent "));
                socket.close();
                return;
            }

            String handle = msg.text == null ? "" : msg.text.trim();
            if (handle.isEmpty()) {
                sub.send(new GameMessage(GameMessage.JOIN_REJECTED, "empty hangle warning"));
                socket.close();
                return;
            }

            synchronized (this) {
                if (gameStarted) {
                    sub.send(new GameMessage(GameMessage.JOIN_REJECTED, "Game already started."));
                    socket.close();
                    return;
                }

                if (isTheHandleAlreadyTaken(handle)) {
                    sub.send(new GameMessage(GameMessage.JOIN_REJECTED, "Name \"" + handle + "\" is already taken."));
                    socket.close();
                    return;
                }

                sub.handle = handle;
                clients.add(sub);
                players.add(new PlayerInfo(handle));
                recalculateTeamGold();
            }

            sub.send(new GameMessage(GameMessage.JOIN_ACCEPTED, "Connected to lobby.", getCurrentLobbyState()));
            broadcastLobbyState();

            // holy bandaid ts code sucks
            while (!socket.isClosed()) {
                Object incoming = sub.read();
                if (!(incoming instanceof GameMessage)) {
                    break;
                }

                GameMessage in = (GameMessage) incoming;
                if (in.type.equals(GameMessage.START)) {
                    startGameNonHost();
                }
                if (GameMessage.HERO_SELECT.equals(in.type)) {
                    changeHeroe(handle, in.text);
                    broadcastLobbyState();
                } else if (GameMessage.BUY_ITEM.equals(in.type)) {
                    buyItem(in.text, handle);
                } else if (GameMessage.SELL_ITEM.equals(in.type)) {
                    sellItem(in.text, handle);
                } else if (GameMessage.SELECT_DRAGON.equals(in.type)) {
                    selectBoss(in.text);
                } else if (GameMessage.CHAT.equals(in.type)) {
                    String cleanMsg = in.text == null ? "" : in.text.trim(); // 🤤
                    if (!cleanMsg.isEmpty()) {
                        synchronized (this) {
                            chatLog.add(handle + ": " + cleanMsg);
                        }
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
                socket.close();
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

    private PlayerInfo findPlayerByHandle(String handle) {
        if (handle == null) {
            return null;
        }
        String cleanHandle = handle.trim();
        if (cleanHandle.isEmpty()) {
            return null;
        }

        for (PlayerInfo p : players) {
            if (p.handle != null && p.handle.equalsIgnoreCase(cleanHandle)) {
                return p;
            }
        }
        return null;
    }

    private boolean removeFirstPurchasedItem(PlayerInfo player, String itemName) {
        if (player == null || player.purchasedItems == null || itemName == null) {
            return false;
        }

        String cleanName = itemName.trim();
        for (int i = 0; i < player.purchasedItems.size(); i++) {
            String ownedItem = player.purchasedItems.get(i);
            if (ownedItem != null && ownedItem.trim().equals(cleanName)) {
                player.purchasedItems.remove(i);
                return true;
            }
        }

        return false;
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
        boolean removedPlayer = false;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).handle.equals(sub.handle)) {
                players.remove(i);
                removedPlayer = true;
                break;
            }
        }
        if (removedPlayer && !gameStarted) {
            recalculateTeamGold();
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

    public synchronized void sendGameMessage(GameMessage message) {
        for (GameSubscriber client : clients) {
            try {
                System.out.println("HHHEE");
                client.send(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void close() {
        // sok 🫠
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
