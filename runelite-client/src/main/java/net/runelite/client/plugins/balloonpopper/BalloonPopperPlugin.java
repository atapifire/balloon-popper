package net.runelite.client.plugins.balloonpopper;

import javax.swing.SwingUtilities;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.widgets.Widget;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Experience;
import net.runelite.api.Player;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.MouseManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.balloonpopper.ui.BalloonPopperPanel;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(name = "Balloon Popper", description = "Track balloons popped and display it as Sailing XP", tags = {
        "balloon", "party", "sailing", "xp" })
@Slf4j
public class BalloonPopperPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private BalloonPopperConfig config;

    @Inject
    private ItemManager itemManager;

    @Inject
    private ConfigManager configManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private MouseManager mouseManager;

    @Inject
    private BalloonPopperOverlay overlay;

    @Inject
    private BalloonPopperGuideOverlay guideOverlay; // The new Custom Interface

    @Inject
    private BalloonPopperInputListener inputListener;

    @Inject
    private BalloonPopperLogOverlay logOverlay;

    @Inject
    private BalloonPopperLevelUpOverlay levelUpOverlay;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private Gson gson;

    private BalloonPopperPanel panel;
    private NavigationButton navButton;

    private int balloonsSingle;
    private int balloonsDouble;
    private int balloonsTriple;

    @Getter
    private Map<Integer, Integer> collectionLog = new HashMap<>();

    // Logic for new custom interface
    @Getter
    @Setter
    private boolean guideVisible = false;

    private int balloonsYellow;
    private int balloonsRed;
    private int balloonsBlue;
    private int balloonsGreen;
    private int balloonsPurple;
    private int balloonsWhite;

    private final Map<WorldPoint, Long> pendingSelfPops = new HashMap<>();
    private final Map<WorldPoint, Integer> validPops = new HashMap<>();
    private final Map<WorldPoint, Map<Integer, Integer>> tileSnapshots = new HashMap<>();
    private final Map<WorldPoint, Map<Integer, Integer>> accountedItems = new HashMap<>();

    private WorldPoint lastBurstPoint;
    private int lastLevel = -1;
    @Getter
    private boolean levelUpVisible = false;
    @Getter
    private int levelUpLevel = 1;

    public long getTotalXp() {
        return (balloonsSingle * 3L) + (balloonsDouble * 6L) + (balloonsTriple * 9L);
    }

    public int getBalloonsSingle() {
        return balloonsSingle;
    }

    public int getBalloonsDouble() {
        return balloonsDouble;
    }

    public int getBalloonsTriple() {
        return balloonsTriple;
    }

    public int getBalloonsYellow() {
        return balloonsYellow;
    }

    public int getBalloonsRed() {
        return balloonsRed;
    }

    public int getBalloonsBlue() {
        return balloonsBlue;
    }

    public int getBalloonsGreen() {
        return balloonsGreen;
    }

    public int getBalloonsPurple() {
        return balloonsPurple;
    }

    public int getBalloonsWhite() {
        return balloonsWhite;
    }

    @Provides
    BalloonPopperConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BalloonPopperConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        balloonsSingle = config.balloonsSingle();
        balloonsDouble = config.balloonsDouble();
        balloonsTriple = config.balloonsTriple();

        balloonsYellow = config.balloonsYellow();
        balloonsRed = config.balloonsRed();
        balloonsBlue = config.balloonsBlue();
        balloonsGreen = config.balloonsGreen();
        balloonsPurple = config.balloonsPurple();
        balloonsWhite = config.balloonsWhite();

        Type mapType = new TypeToken<Map<Integer, Integer>>() {
        }.getType();
        collectionLog = gson.fromJson(config.collectionLogData(), mapType);
        if (collectionLog == null) {
            collectionLog = new HashMap<>();
        }

        panel = injector.getInstance(BalloonPopperPanel.class);

        navButton = NavigationButton.builder()
                .tooltip("Balloon Popper")
                .icon(ImageUtil.loadImageResource(getClass(), "icon.png"))
                .priority(5)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);

        overlayManager.add(overlay);
        overlayManager.add(logOverlay);
        overlayManager.add(guideOverlay);
        overlayManager.add(levelUpOverlay);

        mouseManager.registerMouseListener(inputListener);
        mouseManager.registerMouseWheelListener(inputListener);

        lastLevel = Experience.getLevelForXp((int) Math.min(Experience.MAX_SKILL_XP, getTotalXp()));

        log.info("Balloon Popper started! Last level: {}", lastLevel);
    }

    @Override
    protected void shutDown() throws Exception {
        clientToolbar.removeNavigation(navButton);

        overlayManager.remove(overlay);
        overlayManager.remove(logOverlay);
        overlayManager.remove(guideOverlay);
        overlayManager.remove(levelUpOverlay);

        mouseManager.unregisterMouseListener(inputListener);
        mouseManager.unregisterMouseWheelListener(inputListener);

        log.info("Balloon Popper stopped!");
    }

    @Subscribe
    public void onGameStateChanged(net.runelite.api.events.GameStateChanged event) {
        // Populate database with items on client thread when logged in
        if (client.getGameState() == net.runelite.api.GameState.LOGGED_IN) {
            BalloonLogDatabase.populateItems(client, itemManager);
            lastLevel = Experience.getLevelForXp((int) getTotalXp());
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        // Debug logging to find the exact option/target
        log.info("Menu Click: Option='{}', Target='{}', ID={}, WidgetId={}, Param0={}, Param1={}",
                event.getMenuOption(), event.getMenuTarget(), event.getId(), event.getWidgetId(),
                event.getParam0(), event.getParam1());

        // Intercept Guide click on Sailing - open our custom interface instead
        String option = event.getMenuOption().replaceAll("<[^>]*>", ""); // Strip HTML tags tags
        if ((option.contains("Sailing") && option.contains("guide")) || event.getWidgetId() == 20971544) {
            event.consume();
            // Open our custom in-game interface
            guideVisible = true;
            return;
        }

        if (event.getMenuOption().equals("Burst") && event.getMenuTarget().contains("Balloon")) {
            // ... existing burst logic ...
            int id = event.getId();
            boolean burst = false;
            lastBurstPoint = WorldPoint.fromScene(client, event.getParam0(), event.getParam1(), client.getPlane());

            switch (id) {
                case 115:
                    balloonsYellow++;
                    balloonsSingle++;
                    burst = true;
                    break;
                case 116:
                    balloonsRed++;
                    balloonsSingle++;
                    burst = true;
                    break;
                case 117:
                    balloonsBlue++;
                    balloonsSingle++;
                    burst = true;
                    break;
                case 118:
                    balloonsGreen++;
                    balloonsSingle++;
                    burst = true;
                    break;
                case 119:
                    balloonsPurple++;
                    balloonsSingle++;
                    burst = true;
                    break;
                case 120:
                    balloonsWhite++;
                    balloonsSingle++;
                    burst = true;
                    break;
                case 121:
                    balloonsDouble++;
                    burst = true;
                    break;
                case 122:
                    balloonsTriple++;
                    burst = true;
                    break;
            }

            if (burst) {
                configManager.setConfiguration("balloonpopper", "balloonsSingle", balloonsSingle);
                configManager.setConfiguration("balloonpopper", "balloonsDouble", balloonsDouble);
                configManager.setConfiguration("balloonpopper", "balloonsTriple", balloonsTriple);
                updateBalloonConfig(id);

                int balloonItemId = getBalloonItemId(id);
                if (balloonItemId != -1) {
                    addToLog(balloonItemId, 1);
                }

                pendingSelfPops.put(lastBurstPoint, System.currentTimeMillis());
                tileSnapshots.put(lastBurstPoint, getItemSnapshot(lastBurstPoint));
                accountedItems.put(lastBurstPoint, new HashMap<>());

                log.info("Click registered: Burst balloon ID {} at {}. Current XP: {}", id, lastBurstPoint,
                        getTotalXp());

                checkLevelUp();
            }
        }
    }

    private Map<Integer, Integer> getItemSnapshot(WorldPoint wp) {
        Map<Integer, Integer> snapshot = new HashMap<>();
        net.runelite.api.Tile tile = getTileAt(wp);
        if (tile != null && tile.getGroundItems() != null) {
            for (net.runelite.api.TileItem item : tile.getGroundItems()) {
                snapshot.put(item.getId(), snapshot.getOrDefault(item.getId(), 0) + item.getQuantity());
            }
        }
        return snapshot;
    }

    private net.runelite.api.Tile getTileAt(WorldPoint wp) {
        if (wp.getPlane() != client.getPlane())
            return null;

        int sceneX = wp.getX() - client.getBaseX();
        int sceneY = wp.getY() - client.getBaseY();

        if (sceneX < 0 || sceneX >= 104 || sceneY < 0 || sceneY >= 104)
            return null;

        return client.getScene().getTiles()[client.getPlane()][sceneX][sceneY];
    }

    private void checkLevelUp() {
        int currentLevel = Experience.getLevelForXp((int) getTotalXp());
        if (lastLevel != -1 && currentLevel > lastLevel) {
            Player localPlayer = client.getLocalPlayer();
            if (localPlayer != null) {
                localPlayer.setGraphic(199); // LEVEL_UP GFX
            }
            client.playSoundEffect(2655); // LEVEL_UP Sound

            String message = String.format(
                    "Congratulations, you've just advanced your Party Skill level. Your level is now %d.",
                    currentLevel);
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);

            levelUpLevel = currentLevel;
            levelUpVisible = true;
            log.info("Level up! New level: {}", currentLevel);
        }
        lastLevel = currentLevel;
    }

    public void hideLevelUp() {
        levelUpVisible = false;
    }

    @Subscribe
    public void onGameObjectDespawned(net.runelite.api.events.GameObjectDespawned event) {
        net.runelite.api.GameObject obj = event.getGameObject();
        int id = obj.getId();

        // Check if this is a balloon (IDs 115-122)
        if (id >= 115 && id <= 122) {
            WorldPoint loc = obj.getWorldLocation();
            Long pendingTime = pendingSelfPops.get(loc);

            // If we recently clicked this balloon, it's a valid pop for us
            // Increased timeout to 10s to allow for walking to the balloon
            if (pendingTime != null && System.currentTimeMillis() - pendingTime < 10000) {
                validPops.put(loc, client.getTickCount());
                pendingSelfPops.remove(loc);

                // Refresh snapshot at time of pop for better accuracy
                tileSnapshots.put(loc, getItemSnapshot(loc));

                log.info("Self-pop confirmed: Balloon popped at {} on tick {}", loc, client.getTickCount());
            } else {
                log.debug("Balloon popped at {} by someone else (or no recent click match)", loc);
            }
        }
    }

    private int getBalloonItemId(int burstId) {
        switch (burstId) {
            case 115:
                return net.runelite.api.ItemID.YELLOW_BALLOON;
            case 116:
                return net.runelite.api.ItemID.RED_BALLOON;
            case 117:
                return net.runelite.api.ItemID.BLUE_BALLOON;
            case 118:
                return net.runelite.api.ItemID.GREEN_BALLOON;
            case 119:
                return net.runelite.api.ItemID.PURPLE_BALLOON;
            case 120:
                return net.runelite.api.ItemID.ORIGAMI_BALLOON;
            case 121:
                return net.runelite.api.ItemID.BIRTHDAY_BALLOONS;
            case 122:
                return net.runelite.api.ItemID._10TH_BIRTHDAY_BALLOONS;
            default:
                return -1;
        }
    }

    private void addToLog(int itemId, int quantity) {
        // Normalize noted items to unnoted versions
        net.runelite.api.ItemComposition itemComposition = itemManager.getItemComposition(itemId);
        if (itemComposition.getNote() != -1) {
            itemId = itemComposition.getLinkedNoteId();
        }

        int count = collectionLog.getOrDefault(itemId, 0) + quantity;
        collectionLog.put(itemId, count);
        configManager.setConfiguration("balloonpopper", "collectionLogData", gson.toJson(collectionLog));
        SwingUtilities.invokeLater(() -> panel.updateCollectionLog());
    }

    private void updateBalloonConfig(int id) {
        // Simple helper to avoid massive switch in replacement
        switch (id) {
            case 115:
                configManager.setConfiguration("balloonpopper", "balloonsYellow", balloonsYellow);
                break;
            case 116:
                configManager.setConfiguration("balloonpopper", "balloonsRed", balloonsRed);
                break;
            case 117:
                configManager.setConfiguration("balloonpopper", "balloonsBlue", balloonsBlue);
                break;
            case 118:
                configManager.setConfiguration("balloonpopper", "balloonsGreen", balloonsGreen);
                break;
            case 119:
                configManager.setConfiguration("balloonpopper", "balloonsPurple", balloonsPurple);
                break;
            case 120:
                configManager.setConfiguration("balloonpopper", "balloonsWhite", balloonsWhite);
                break;
        }
    }

    @Subscribe
    public void onItemSpawned(ItemSpawned event) {
        WorldPoint itemLoc = event.getTile().getWorldLocation();
        Integer popTick = validPops.get(itemLoc);
        Long clickTime = pendingSelfPops.get(itemLoc);

        if (popTick != null || clickTime != null) {
            long timeDiff = clickTime != null ? System.currentTimeMillis() - clickTime : -1;
            int tickDiff = popTick != null ? client.getTickCount() - popTick : -1;

            if ((tickDiff >= 0 && tickDiff <= 10) || (timeDiff >= 0 && timeDiff < 10000)) {
                int itemId = event.getItem().getId();
                int qty = event.getItem().getQuantity();

                addToLog(itemId, qty);

                // Record this item as accounted for on this tile
                Map<Integer, Integer> accounted = accountedItems.getOrDefault(itemLoc, new HashMap<>());
                accounted.put(itemId, accounted.getOrDefault(itemId, 0) + qty);
                accountedItems.put(itemLoc, accounted);

                log.info("[EVENT MATCH] Item {} x{} at {} (Tick Diff: {}, Time Diff: {})",
                        itemId, qty, itemLoc, tickDiff, timeDiff);
                return;
            }
        }

        log.debug("STRAY ITEM: {} x{} at {} - No pop match (Tick: {}, Last Pop Tick: {}, Click Time: {})",
                event.getItem().getId(), event.getItem().getQuantity(), itemLoc,
                client.getTickCount(), popTick, clickTime);
    }

    private void scanTile(WorldPoint wp) {
        Map<Integer, Integer> currentItems = getItemSnapshot(wp);
        Map<Integer, Integer> snapshot = tileSnapshots.getOrDefault(wp, new HashMap<>());
        Map<Integer, Integer> accounted = accountedItems.getOrDefault(wp, new HashMap<>());

        currentItems.forEach((id, qty) -> {
            int beforeQty = snapshot.getOrDefault(id, 0);
            int alreadyAccounted = accounted.getOrDefault(id, 0);
            int newQty = qty - beforeQty - alreadyAccounted;

            if (newQty > 0) {
                addToLog(id, newQty);
                log.info("[SCAN MATCH] Found item {} x{} at {} (Current: {}, Before: {}, Accounted: {})",
                        id, newQty, wp, qty, beforeQty, alreadyAccounted);

                // Update accounted to avoid triple-counting if scan runs again
                accounted.put(id, alreadyAccounted + newQty);
            }
        });
        accountedItems.put(wp, accounted);
    }

    @Subscribe
    public void onGameTick(net.runelite.api.events.GameTick event) {
        // Cleanup maps periodically
        if (client.getTickCount() % 50 == 0) {
            pendingSelfPops.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue() > 15000);
            validPops.entrySet().removeIf(entry -> client.getTickCount() - entry.getValue() > 20);
            tileSnapshots.entrySet().removeIf(
                    entry -> !pendingSelfPops.containsKey(entry.getKey()) && !validPops.containsKey(entry.getKey()));
            accountedItems.entrySet().removeIf(entry -> !validPops.containsKey(entry.getKey()));
        }

        // Scan logic: check tiles a few moments after pop
        for (Map.Entry<WorldPoint, Integer> entry : validPops.entrySet()) {
            if (client.getTickCount() == entry.getValue() + 2) {
                scanTile(entry.getKey());
            }
        }

        // Aggressively hide the skill guide if our guide is visible
        if (guideVisible) {
            Widget guideWidget = client.getWidget(InterfaceID.SKILL_GUIDE, 0);
            if (guideWidget != null && !guideWidget.isHidden()) {
                Widget title = client.getWidget(InterfaceID.SKILL_GUIDE, 1);
                if (title != null && title.getText().contains("Sailing")) {
                    guideWidget.setHidden(true);
                }
            }
        }
    }

    @Subscribe
    public void onScriptPreFired(ScriptPreFired event) {
        // Log script args to find the new ID if 915 changed
        if (event.getScriptId() == 915) {
            log.info("Script 915 fired with args: {}",
                    java.util.Arrays.toString(event.getScriptEvent().getArguments()));
            Object[] args = event.getScriptEvent().getArguments();
            if (args != null && args.length > 0 && args[0] instanceof Integer && (Integer) args[0] == 28) {
                guideVisible = true;
                // event.getScriptEvent().setArguments(new Object[0]); // Attempt to kill it?
            }
        }
    }
}
