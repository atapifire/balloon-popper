package net.runelite.client.plugins.balloonpopper;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.ItemID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.game.SpriteManager;

public class BalloonPopperOverlay extends Overlay {
    private final Client client;
    private final BalloonPopperPlugin plugin;
    private final ItemManager itemManager;
    private final TooltipManager tooltipManager;
    private final SpriteManager spriteManager;
    private final BufferedImage balloonIcon;

    @Inject
    public BalloonPopperOverlay(Client client, BalloonPopperPlugin plugin, ItemManager itemManager,
            TooltipManager tooltipManager, SpriteManager spriteManager) {
        this.client = client;
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.tooltipManager = tooltipManager;
        this.spriteManager = spriteManager;
        this.balloonIcon = ImageUtil.loadImageResource(getClass(), "icon.png");
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(net.runelite.client.ui.overlay.OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return null;
        }

        // Sailing skill widget (320:24)
        Widget sailingWidget = client.getWidget(InterfaceID.STATS, 24);
        if (sailingWidget == null || sailingWidget.isHidden()) {
            return null;
        }

        Rectangle bounds = sailingWidget.getBounds();
        if (bounds == null || bounds.width <= 0) {
            return null;
        }

        long totalXp = plugin.getTotalXp();
        int level = Experience.getLevelForXp((int) Math.min(Experience.MAX_SKILL_XP, totalXp));

        // Hide original Sailing icon and text children
        // Also clear Name to suppress engine tooltip "Sailing: Members Only"
        sailingWidget.setName("");

        Widget[] children = sailingWidget.getStaticChildren();
        if (children != null) {
            for (Widget child : children) {
                child.setHidden(true);
            }
        }
        Widget[] dynamicChildren = sailingWidget.getDynamicChildren();
        if (dynamicChildren != null) {
            for (Widget child : dynamicChildren) {
                child.setHidden(true);
            }
        }
        Widget[] nestedChildren = sailingWidget.getNestedChildren();
        if (nestedChildren != null) {
            for (Widget child : nestedChildren) {
                child.setHidden(true);
            }
        }

        // Draw standard OSRS skill slot background
        final net.runelite.api.Point mouse = client.getMouseCanvasPosition();
        final boolean hovered = bounds.contains(mouse.getX(), mouse.getY());

        // Standard OSRS skill hover is slightly brighter.
        // 174/176 are unhovered. 177/178 are RED (members).
        // 494/495 seem to be the standard hover sprites in some versions.
        // Let's use 174/176 as base and add a highlight if hovered.
        BufferedImage leftTile = spriteManager.getSprite(174, 0);
        BufferedImage rightTile = spriteManager.getSprite(176, 0);

        if (leftTile != null && rightTile != null) {
            graphics.drawImage(leftTile, (int) bounds.getX(), (int) bounds.getY(), null);
            graphics.drawImage(rightTile, (int) bounds.getX() + (bounds.width / 2), (int) bounds.getY(), null);
        }

        // Draw Balloon icon aligned to the left
        if (balloonIcon != null) {
            int x = (int) bounds.getX() + 5;
            int y = (int) bounds.getY() + (bounds.height / 2) - (balloonIcon.getHeight() / 2);
            graphics.drawImage(balloonIcon, x, y, null);
        }

        // Draw levels with shadow for authentic OSRS feel
        String levelStr = String.valueOf(level);
        int x1 = (int) bounds.getX() + 32;
        int y1 = (int) bounds.getY() + 15;
        int x2 = (int) bounds.getX() + 44;
        int y2 = (int) bounds.getY() + 28;

        // Draw shadow
        graphics.setColor(Color.BLACK);
        graphics.drawString(levelStr, x1 + 1, y1 + 1);
        graphics.drawString(levelStr, x2 + 1, y2 + 1);

        // Draw real level
        graphics.setColor(Color.YELLOW);
        graphics.drawString(levelStr, x1, y1);
        graphics.drawString(levelStr, x2, y2);

        // Tooltip tracking - Match Image 2 style
        if (hovered) {
            int nextLevel = Math.min(99, level + 1);
            int xpForNextLevel = Experience.getXpForLevel(nextLevel);
            int remainingXp = xpForNextLevel - (int) totalXp;

            StringBuilder sb = new StringBuilder();
            sb.append("Party XP: ").append(String.format("%,d", (long) totalXp)).append("<br>");
            sb.append("Next level at: ").append(String.format("%,d", xpForNextLevel)).append("<br>");
            sb.append("Remaining XP: ").append(String.format("%,d", Math.max(0, remainingXp)));

            tooltipManager.add(new Tooltip(sb.toString()));
        }

        return null;
    }
}
