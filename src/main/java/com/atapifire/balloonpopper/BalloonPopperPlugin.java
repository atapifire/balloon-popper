package com.atapifire.balloonpopper;

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
import com.atapifire.balloonpopper.ui.BalloonPopperPanel;
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
                log.info("Click registered: Burst balloon ID {} at {}. Current XP: {}", id, lastBurstPoint,
                        getTotalXp());

                checkLevelUp();
            }
        }
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
            if (pendingTime != null && System.currentTimeMillis() - pendingTime < 2000) {
                validPops.put(loc, client.getTickCount());
                pendingSelfPops.remove(loc);
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

        if (popTick != null) {
            int tickDiff = client.getTickCount() - popTick;
            // 0-3 ticks is reasonable for lag
            if (tickDiff >= 0 && tickDiff <= 3) {
                int itemId = event.getItem().getId();
                addToLog(itemId, event.getItem().getQuantity());
                log.info("[DESPAWN MATCH] Item {} x{} at {} (Tick Diff: {})",
                        itemId, event.getItem().getQuantity(), itemLoc, tickDiff);
                validPops.remove(itemLoc);
                return;
            }
        }

        // Fallback: If no despawn record, check if we clicked a balloon here recently
        if (clickTime != null) {
            long timeDiff = System.currentTimeMillis() - clickTime;
            if (timeDiff < 3000) {
                int itemId = event.getItem().getId();
                addToLog(itemId, event.getItem().getQuantity());
                log.info("[CLICK FALLBACK] Item {} x{} at {} (Time Diff: {}ms)",
                        itemId, event.getItem().getQuantity(), itemLoc, timeDiff);
                pendingSelfPops.remove(itemLoc);
                return;
            }
        }

        log.debug("STRAY ITEM: {} x{} at {} - No pop match (Tick: {}, Last Pop Tick: {}, Click Time: {})",
                event.getItem().getId(), event.getItem().getQuantity(), itemLoc,
                client.getTickCount(), popTick, clickTime);
    }

    @Subscribe
    public void onGameTick(net.runelite.api.events.GameTick event) {
        // Cleanup maps periodically
        if (client.getTickCount() % 50 == 0) {
            pendingSelfPops.entrySet().removeIf(entry -> System.currentTimeMillis() - entry.getValue() > 5000);
            validPops.entrySet().removeIf(entry -> client.getTickCount() - entry.getValue() > 10);
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
