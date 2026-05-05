package src.networking;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import src.dice.DiceEnum;
import src.item.ItemEnum;
import src.item.VillageMarketplaceCatalog;
import src.players.DragonCatalog;
import src.players.Heroes;

public class GameServer {
    private static final int kStartingGoldPerPlayer = 10;
    private static final int kMaxItemsPerPlayer = 2;
    private static final int kSkillSlotCount = 8;
    private static final int kDiceCount = 5;
    private static final int kSkillSymbolCount = 5;
    private static final DiceEnum[] kEligibleDiceFaces = {
            DiceEnum.SWORD,
            DiceEnum.CROSSBOWS,
            DiceEnum.DAGGGERS,
            DiceEnum.SHIELD,
            DiceEnum.DRAGON
    };
    private static final Map<String, HeroStats> kHeroStats = createHeroStats();

    private final ServerSocket serverSocket;
    private final ArrayList<GameSubscriber> clients = new ArrayList<>();
    private final ArrayList<PlayerInfo> players = new ArrayList<>();
    private final ArrayList<String> chatLog = new ArrayList<>();
    private final String hostHandle;
    private int teamGold;
    private boolean gameStarted;
    private String selectedDragon;
    private DiceEnum[] dicePool;
    private int[] diceSkillIndex;
    private int[] diceSymbolIndex;
    private boolean[] usedSkills;
    private int pendingDragonRetaliationSymbols;
    private boolean pendingDragonRetaliation;

    public GameServer(String hostHandle) throws IOException {
        this.hostHandle = hostHandle.trim();
        serverSocket = new ServerSocket(0);
        players.add(new PlayerInfo(this.hostHandle));
        recalculateTeamGold();
        resetDiceState();
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
            cp.currentHitPoints = p.currentHitPoints;
            copyPlayers.add(cp);
        }

        LobbyState state = new LobbyState(copyPlayers, new ArrayList<>(chatLog), allReady, teamGold, selectedDragon);
        ensureDiceState();
        state.dicePool = dicePool == null ? null : dicePool.clone();
        state.diceSkillIndex = diceSkillIndex == null ? null : Arrays.copyOf(diceSkillIndex, diceSkillIndex.length);
        state.diceSymbolIndex = diceSymbolIndex == null ? null : Arrays.copyOf(diceSymbolIndex, diceSymbolIndex.length);
        state.usedSkills = usedSkills == null ? null : Arrays.copyOf(usedSkills, usedSkills.length);
        return state;
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
            teamGold = kStartingGoldPerPlayer;
            return;
        }
        teamGold = players.size() * kStartingGoldPerPlayer;
    }

    private synchronized void resetDiceState() {
        dicePool = new DiceEnum[kDiceCount];
        diceSkillIndex = new int[kDiceCount];
        diceSymbolIndex = new int[kDiceCount];
        usedSkills = new boolean[kSkillSlotCount];
        pendingDragonRetaliationSymbols = 0;
        pendingDragonRetaliation = false;
        fillDicePool();
        Arrays.fill(diceSkillIndex, -1);
        Arrays.fill(diceSymbolIndex, -1);
    }

    private synchronized void ensureDiceState() {
        if (dicePool == null || dicePool.length != kDiceCount) {
            dicePool = new DiceEnum[kDiceCount];
            fillDicePool();
        } else {
            for (int i = 0; i < kDiceCount; i++) {
                if (dicePool[i] == null) {
                    dicePool[i] = rollRandomDie();
                }
            }
        }
        if (diceSkillIndex == null || diceSkillIndex.length != kDiceCount) {
            diceSkillIndex = new int[kDiceCount];
            Arrays.fill(diceSkillIndex, -1);
        }
        if (diceSymbolIndex == null || diceSymbolIndex.length != kDiceCount) {
            diceSymbolIndex = new int[kDiceCount];
            Arrays.fill(diceSymbolIndex, -1);
        }
        if (usedSkills == null || usedSkills.length != kSkillSlotCount) {
            usedSkills = new boolean[kSkillSlotCount];
        }
    }

    private void fillDicePool() {
        for (int i = 0; i < kDiceCount; i++) {
            dicePool[i] = rollRandomDie();
        }
    }

    private DiceEnum rollRandomDie() {
        return kEligibleDiceFaces[(int) (Math.random() * kEligibleDiceFaces.length)];
    }

    private synchronized void rollDice(int[] indices) {
        ensureDiceState();
        if (indices == null || indices.length == 0) {
            for (int i = 0; i < kDiceCount; i++) {
                if (diceSkillIndex[i] == -1) {
                    dicePool[i] = rollRandomDie();
                }
            }
            pendingDragonRetaliationSymbols = countDragonSymbols();
            return;
        }

        for (int index : indices) {
            if (index < 0 || index >= kDiceCount) {
                continue;
            }
            if (diceSkillIndex[index] != -1) {
                continue;
            }
            dicePool[index] = rollRandomDie();
        }
        pendingDragonRetaliationSymbols = countDragonSymbols();
    }

    private synchronized void placeDie(int dieIndex, int skillIndex, int symbolIndex) {
        ensureDiceState();
        if (dieIndex < 0 || dieIndex >= kDiceCount) {
            return;
        }
        if (skillIndex < 0 || skillIndex >= kSkillSlotCount) {
            return;
        }
        if (symbolIndex < 0 || symbolIndex >= kSkillSymbolCount) {
            return;
        }

        int existing = findDieAt(skillIndex, symbolIndex);
        if (existing != -1 && existing != dieIndex) {
            return;
        }

        diceSkillIndex[dieIndex] = skillIndex;
        diceSymbolIndex[dieIndex] = symbolIndex;
    }

    private synchronized void removeDie(int dieIndex) {
        ensureDiceState();
        if (dieIndex < 0 || dieIndex >= kDiceCount) {
            return;
        }
        diceSkillIndex[dieIndex] = -1;
        diceSymbolIndex[dieIndex] = -1;
    }

    private synchronized int findDieAt(int skillIndex, int symbolIndex) {
        if (diceSkillIndex == null || diceSymbolIndex == null) {
            return -1;
        }
        for (int i = 0; i < kDiceCount; i++) {
            if (diceSkillIndex[i] == skillIndex && diceSymbolIndex[i] == symbolIndex) {
                return i;
            }
        }
        return -1;
    }

    private synchronized void setSkillUsed(int skillIndex, boolean used) {
        ensureDiceState();
        if (skillIndex < 0 || skillIndex >= kSkillSlotCount) {
            return;
        }
        if (used) {
            usedSkills[skillIndex] = true;
        }
    }

    private void resetPlayerHitPoints(PlayerInfo player) {
        if (player == null) {
            return;
        }
        HeroStats stats = getHeroStats(player.hero);
        if (stats == null) {
            return;
        }
        player.currentHitPoints = stats.hitPoints;
    }

    private void resetHitPointsForAllPlayers() {
        for (PlayerInfo player : players) {
            resetPlayerHitPoints(player);
        }
    }

    private int getHeroArmourClass(String hero) {
        HeroStats stats = getHeroStats(hero);
        return stats == null ? 0 : stats.armourClass;
    }

    private int countDragonSymbols() {
        ensureDiceState();
        int total = 0;
        for (DiceEnum face : dicePool) {
            if (face == DiceEnum.DRAGON) {
                total++;
            }
        }
        return total;
    }

    private void applyDragonRetaliation(String handle) {
        if (selectedDragon == null || selectedDragon.trim().isEmpty()) {
            pendingDragonRetaliation = false;
            return;
        }

        int dragonSymbols = pendingDragonRetaliationSymbols;
        if (dragonSymbols <= 0) {
            pendingDragonRetaliation = false;
            return;
        }

        DragonCatalog.DragonProfile dragon = DragonCatalog.findById(selectedDragon);
        if (dragon == null) {
            pendingDragonRetaliation = false;
            return;
        }

        PlayerInfo player = findPlayerByHandle(handle);
        if (player == null) {
            pendingDragonRetaliation = false;
            return;
        }

        if (player.currentHitPoints < 0) {
            resetPlayerHitPoints(player);
        }
        if (player.currentHitPoints < 0) {
            pendingDragonRetaliation = false;
            return;
        }

        int baseDamage = dragon.getCounterAttackDamage(dragonSymbols);
        if (baseDamage <= 0) {
            pendingDragonRetaliation = false;
            return;
        }

        int armourClass = getHeroArmourClass(player.hero);
        int damage = Math.max(0, baseDamage - armourClass);
        int nextHp = Math.max(0, player.currentHitPoints - damage);
        player.currentHitPoints = nextHp;

        String attacker = dragon.getDisplayName();
        String target = player.handle == null ? "Hero" : player.handle;
        chatLog.add(attacker + " counter attacks " + target + " for " + damage
                + " HP (" + baseDamage + " - AC " + armourClass + ").");
        pendingDragonRetaliationSymbols = 0;
        pendingDragonRetaliation = false;
    }

    private HeroStats getHeroStats(String hero) {
        HeroStats stats = kHeroStats.get(hero);
        if (stats == null) {
            return null;
        }
        return stats;
    }

    private static Map<String, HeroStats> createHeroStats() {
        LinkedHashMap<String, HeroStats> map = new LinkedHashMap<>();

        map.put(Heroes.WARRIOR, new HeroStats(60, 3));
        map.put(Heroes.WIZARD, new HeroStats(42, 1));
        map.put(Heroes.CLERIC, new HeroStats(50, 2));
        map.put(Heroes.RANGER, new HeroStats(48, 2));
        map.put(Heroes.ROGUE, new HeroStats(44, 1));

        return map;
    }

    private static final class HeroStats {
        private final int hitPoints;
        private final int armourClass;

        private HeroStats(int hitPoints, int armourClass) {
            this.hitPoints = hitPoints;
            this.armourClass = armourClass;
        }
    }

    private synchronized boolean isPrimaryHandle(String handle) {
        if (handle == null || players.isEmpty()) {
            return false;
        }
        PlayerInfo primary = players.get(0);
        return primary != null && primary.handle != null && primary.handle.equalsIgnoreCase(handle.trim());
    }

    public synchronized void applyGameAction(GameMessage message, String handle) {
        if (message == null) {
            return;
        }
        if (!isPrimaryHandle(handle)) {
            return;
        }

        if (GameMessage.DICE_ROLL.equals(message.type)) {
            rollDice(message.diceIndices);
            broadcastLobbyState();
            return;
        }

        if (GameMessage.DICE_PLACE.equals(message.type)) {
            placeDie(message.dieIndex, message.skillIndex, message.symbolIndex);
            broadcastLobbyState();
            return;
        }

        if (GameMessage.DICE_REMOVE.equals(message.type)) {
            removeDie(message.dieIndex);
            broadcastLobbyState();
            return;
        }

        if (GameMessage.SKILL_USED.equals(message.type)) {
            setSkillUsed(message.skillIndex, message.skillUsed);
            if (message.skillUsed) {
                pendingDragonRetaliation = true;
            }
            broadcastLobbyState();
            return;
        }

        if (GameMessage.END_TURN.equals(message.type)) {
            if (pendingDragonRetaliation) {
                applyDragonRetaliation(handle);
            }
            broadcastLobbyState();
        }
    }

    public synchronized void setHostHero(String hero) {
        for (PlayerInfo p : players) {
            if (p.handle.equals(hostHandle)) {
                p.hero = hero;
                p.ready = isRealHero(hero);
                if (!gameStarted) {
                    resetPlayerHitPoints(p);
                }
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

        PlayerInfo buyerInfo = findPlayerByHandle(handle);
        if (buyerInfo == null) {
            return;
        }

        String buyer = buyerInfo.handle;
        if (marketItem.getType() == ItemEnum.SKILL) {
            int maxPurchasedSkills = getMaxPurchasedSkillsForHero(buyerInfo.hero);
            int ownedSkills = countPurchasedByType(buyerInfo, true);
            if (ownedSkills >= maxPurchasedSkills) {
                chatLog.add(buyer + " cannot buy more skills.");
                broadcastLobbyState();
                return;
            }
        } else {
            int ownedItems = countPurchasedByType(buyerInfo, false);
            if (ownedItems >= kMaxItemsPerPlayer) {
                chatLog.add(buyer + " already has the maximum of " + kMaxItemsPerPlayer + " items");
                broadcastLobbyState();
                return;
            }
        }

        int itemCost = marketItem.getCost();
        if (teamGold < itemCost) {
            chatLog.add("No team gold left for " + cleanItem + ".");
            broadcastLobbyState();
            return;
        }

        int totalPurchased = 0;
        for (PlayerInfo p : players) {
            if (p.purchasedItems != null) {
                for (String pi : p.purchasedItems) {
                    if (pi.equals(cleanItem)) {
                        totalPurchased++;
                    }
                }
            }
        }

        if (totalPurchased >= marketItem.getQuantity()) {
            chatLog.add("No more " + cleanItem + " variants available.");
            broadcastLobbyState();
            return;
        }

        teamGold = teamGold - itemCost;
        buyerInfo.purchasedItems.add(cleanItem);

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
        resetDiceState();
        resetHitPointsForAllPlayers();
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
                } else if (GameMessage.DICE_ROLL.equals(in.type)
                        || GameMessage.DICE_PLACE.equals(in.type)
                        || GameMessage.DICE_REMOVE.equals(in.type)
                        || GameMessage.SKILL_USED.equals(in.type)
                        || GameMessage.END_TURN.equals(in.type)) {
                    applyGameAction(in, handle);
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
                if (!gameStarted) {
                    resetPlayerHitPoints(p);
                }
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

    private int countPurchasedByType(PlayerInfo player, boolean skillType) {
        if (player == null || player.purchasedItems == null) {
            return 0;
        }

        int total = 0;
        for (String purchasedName : player.purchasedItems) {
            if (isSkillPurchase(purchasedName) == skillType) {
                total++;
            }
        }
        return total;
    }

    private boolean isSkillPurchase(String purchasedName) {
        if (purchasedName == null) {
            return false;
        }

        VillageMarketplaceCatalog.MarketplaceItem marketItem = VillageMarketplaceCatalog.findItem(selectedDragon,
                purchasedName.trim());
        return marketItem != null && marketItem.getType() == ItemEnum.SKILL;
    }

    private int getMaxPurchasedSkillsForHero(String heroName) {
        int availableSkillSlots = kSkillSlotCount - getDefaultSkillCount(heroName);
        return Math.max(0, availableSkillSlots);
    }

    private int getDefaultSkillCount(String heroName) {
        if (heroName == null) {
            return 0;
        }

        String cleanHeroName = heroName.trim();
        if (cleanHeroName.equals(Heroes.WARRIOR)
                || cleanHeroName.equals(Heroes.WIZARD)
                || cleanHeroName.equals(Heroes.CLERIC)
                || cleanHeroName.equals(Heroes.RANGER)
                || cleanHeroName.equals(Heroes.ROGUE)) {
            return 6;
        }

        return 0;
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
