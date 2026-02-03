package com.atapifire.balloonpopper;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.ImageUtil;

public class BalloonPopperLevelUpOverlay extends Overlay {
    private final Client client;
    private final BalloonPopperPlugin plugin;
    private final BufferedImage balloonIcon;

    @Inject
    public BalloonPopperLevelUpOverlay(Client client, BalloonPopperPlugin plugin) {
        this.client = client;
        this.plugin = plugin;
        this.balloonIcon = ImageUtil.loadImageResource(getClass(), "icon.png");
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (client.getGameState() != GameState.LOGGED_IN || !plugin.isLevelUpVisible()) {
            return null;
        }

        // Standard level-up dialog dimensions and position (bottom center above chat)
        int width = 512;
        int height = 160;
        int x = (client.getCanvasWidth() - width) / 2;
        // Position it roughly where the chatbox is
        int y = client.getCanvasHeight() - height - 40;

        // Draw the background (darker brown with border)
        graphics.setColor(new Color(72, 64, 52));
        graphics.fillRect(x, y, width, height);
        graphics.setColor(new Color(40, 36, 28));
        graphics.drawRect(x, y, width, height);
        graphics.drawRect(x + 1, y + 1, width - 2, height - 2);

        // Draw the icon
        if (balloonIcon != null) {
            int iconSize = 32;
            graphics.drawImage(balloonIcon, x + 20, y + (height - iconSize) / 2, iconSize, iconSize, null);
        }

        // Draw the text
        graphics.setColor(Color.BLACK);
        graphics.setFont(net.runelite.client.ui.FontManager.getRunescapeBoldFont());

        String title = "Congratulations!";
        String line1 = "You've just advanced your Party Skill level.";
        String line2 = "Your level is now " + plugin.getLevelUpLevel() + ".";

        FontMetrics fm = graphics.getFontMetrics();
        graphics.drawString(title, x + (width - fm.stringWidth(title)) / 2, y + 40);

        graphics.setFont(net.runelite.client.ui.FontManager.getRunescapeFont());
        fm = graphics.getFontMetrics();
        graphics.drawString(line1, x + (width - fm.stringWidth(line1)) / 2, y + 80);
        graphics.drawString(line2, x + (width - fm.stringWidth(line2)) / 2, y + 100);

        graphics.setColor(new Color(0, 0, 128)); // Blue "Click here to continue"
        String footer = "Click here to continue";
        graphics.drawString(footer, x + (width - fm.stringWidth(footer)) / 2, y + 140);

        return null;
    }

    public boolean isMouseOverOverlay(java.awt.Point point) {
        if (!plugin.isLevelUpVisible()) {
            return false;
        }
        int width = 512;
        int height = 160;
        int x = (client.getCanvasWidth() - width) / 2;
        int y = client.getCanvasHeight() - height - 40;
        Rectangle bounds = new Rectangle(x, y, width, height);
        return bounds.contains(point);
    }
}
