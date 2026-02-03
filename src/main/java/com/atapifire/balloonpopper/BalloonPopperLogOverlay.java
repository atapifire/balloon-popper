package com.atapifire.balloonpopper;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.widgets.Widget;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.ImageUtil;
import com.atapifire.balloonpopper.BalloonLogDatabase.LogCategory;
import com.atapifire.balloonpopper.BalloonLogDatabase.LogSubCategory;

public class BalloonPopperLogOverlay extends Overlay {
    private final Client client;
    private final BalloonPopperPlugin plugin;
    private final ItemManager itemManager;
    private final SpriteManager spriteManager;

    private LogCategory currentCategory = LogCategory.F2P;
    private LogSubCategory currentSubCategory = LogSubCategory.MELEE;

    @Inject
    public BalloonPopperLogOverlay(Client client, BalloonPopperPlugin plugin, ItemManager itemManager,
            SpriteManager spriteManager) {
        this.client = client;
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.spriteManager = spriteManager;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return null;
        }

        Widget guideWidget = client.getWidget(InterfaceID.SKILL_GUIDE, 0);
        if (guideWidget == null || guideWidget.isHidden()) {
            return null;
        }

        // Check if it's the Sailing guide (Overview)
        Widget title = client.getWidget(InterfaceID.SKILL_GUIDE, 1);
        if (title == null || !title.getText().contains("Sailing")) {
            return null;
        }

        // Hide all children of the guide widget recursively
        hideWidget(guideWidget);

        Rectangle bounds = guideWidget.getBounds();

        // 1. Draw Background (Collection Log style)
        // Sprite 455 is the standard scroll background
        drawBackground(graphics, bounds);

        // 2. Draw Tabs
        drawTabs(graphics, bounds);

        // 3. Draw Sidebar
        drawSidebar(graphics, bounds);

        // 4. Draw Content
        drawContent(graphics, bounds);

        return null;
    }

    private void hideWidget(Widget widget) {
        if (widget == null)
            return;

        Widget[] children = widget.getStaticChildren();
        if (children != null) {
            for (Widget child : children) {
                child.setHidden(true);
                hideWidget(child);
            }
        }

        Widget[] dynamicChildren = widget.getDynamicChildren();
        if (dynamicChildren != null) {
            for (Widget child : dynamicChildren) {
                child.setHidden(true);
                hideWidget(child);
            }
        }

        Widget[] nestedChildren = widget.getNestedChildren();
        if (nestedChildren != null) {
            for (Widget child : nestedChildren) {
                child.setHidden(true);
                hideWidget(child);
            }
        }
    }

    private void drawBackground(Graphics2D graphics, Rectangle bounds) {
        graphics.setColor(new Color(64, 54, 44));
        graphics.fill(bounds);
        graphics.setColor(Color.BLACK);
        graphics.draw(bounds);
    }

    private void drawTabs(Graphics2D graphics, Rectangle bounds) {
        int xStart = (int) bounds.getX() + 50;
        int yStart = (int) bounds.getY() + 35;
        int width = 70;
        int height = 18;

        graphics.setFont(net.runelite.client.ui.FontManager.getRunescapeFont());

        for (int i = 0; i < LogCategory.values().length; i++) {
            LogCategory cat = LogCategory.values()[i];
            Rectangle tabBounds = new Rectangle(xStart + (i * (width + 2)), yStart, width, height);

            boolean hovered = tabBounds.contains(client.getMouseCanvasPosition().getX(),
                    client.getMouseCanvasPosition().getY());

            if (cat == currentCategory) {
                graphics.setColor(new Color(95, 82, 65));
            } else if (hovered) {
                graphics.setColor(new Color(85, 72, 55));
            } else {
                graphics.setColor(new Color(65, 52, 35));
            }
            graphics.fill(tabBounds);

            graphics.setColor(new Color(0, 0, 0, 150));
            graphics.draw(tabBounds);

            graphics.setColor(cat == currentCategory ? Color.WHITE : new Color(200, 200, 200));
            int stringWidth = graphics.getFontMetrics().stringWidth(cat.getName());
            graphics.drawString(cat.getName(), (int) tabBounds.getX() + (width - stringWidth) / 2,
                    (int) tabBounds.getY() + 14);

            // Handle clicks
            if (hovered && client.getMouseCurrentButton() == 1) {
                currentCategory = cat;
                // Reset subcategory to default when changing main category
                currentSubCategory = BalloonLogDatabase.ITEMS.get(currentCategory).keySet().iterator().next();
            }
        }
    }

    private void drawSidebar(Graphics2D graphics, Rectangle bounds) {
        int xStart = (int) bounds.getX() + 25;
        int yStart = (int) bounds.getY() + 65;
        int width = 110;
        int height = (int) bounds.getHeight() - 100;

        graphics.setColor(new Color(0, 0, 0, 100));
        graphics.fillRect(xStart, yStart, width, height);
        graphics.setColor(new Color(60, 50, 40));
        graphics.drawRect(xStart, yStart, width, height);

        int subY = yStart + 5;
        for (LogSubCategory sub : LogSubCategory.values()) {
            java.util.Set<Integer> items = BalloonLogDatabase.ITEMS.get(currentCategory).get(sub);
            if (items == null || items.isEmpty())
                continue;

            Rectangle subBounds = new Rectangle(xStart + 3, subY, width - 6, 18);
            boolean hovered = subBounds.contains(client.getMouseCanvasPosition().getX(),
                    client.getMouseCanvasPosition().getY());

            if (sub == currentSubCategory) {
                graphics.setColor(new Color(255, 152, 31, 100));
                graphics.fill(subBounds);
            }

            graphics.setColor(sub == currentSubCategory ? new Color(255, 152, 31)
                    : (hovered ? Color.WHITE : new Color(200, 180, 150)));
            graphics.drawString(sub.getName(), (int) subBounds.getX() + 5, (int) subBounds.getY() + 14);

            if (hovered && client.getMouseCurrentButton() == 1) {
                currentSubCategory = sub;
            }
            subY += 20;
        }
    }

    private void drawContent(Graphics2D graphics, Rectangle bounds) {
        int xStart = (int) bounds.getX() + 145;
        int yStart = (int) bounds.getY() + 65;
        int width = (int) (bounds.getWidth() - 170);
        int height = (int) (bounds.getHeight() - 100);

        graphics.setColor(new Color(0, 0, 0, 80));
        graphics.fillRect(xStart, yStart, width, height);
        graphics.setColor(new Color(60, 50, 40));
        graphics.drawRect(xStart, yStart, width, height);

        java.util.Set<Integer> items = BalloonLogDatabase.ITEMS.get(currentCategory).get(currentSubCategory);
        Map<Integer, Integer> logData = plugin.getCollectionLog();

        if (items == null || items.isEmpty()) {
            graphics.setColor(Color.LIGHT_GRAY);
            graphics.drawString("No items in this category", xStart + 20, yStart + 30);
            return;
        }

        int x = xStart + 10;
        int y = yStart + 10;
        int itemSize = 36;
        int padding = 6;
        int itemsPerRow = (width - 20) / (itemSize + padding);

        for (Integer itemId : items) {
            BufferedImage itemImage = itemManager.getImage(itemId);
            if (itemImage != null) {
                boolean obtained = logData.containsKey(itemId);
                BufferedImage displayImage = itemImage;

                if (!obtained) {
                    // Create a darker, desaturated version for unobtained items
                    displayImage = ImageUtil.alphaOffset(itemImage, 0.25f);
                    displayImage = ImageUtil.grayscaleImage(displayImage);
                }

                // Draw item slot background
                Rectangle itemBounds = new Rectangle(x, y, itemSize, itemSize);
                graphics.setColor(obtained ? new Color(50, 40, 30) : new Color(30, 25, 20));
                graphics.fill(itemBounds);
                graphics.setColor(obtained ? new Color(100, 85, 65) : new Color(60, 50, 40));
                graphics.draw(itemBounds);

                // Draw item image centered
                int imgX = x + (itemSize - displayImage.getWidth()) / 2;
                int imgY = y + (itemSize - displayImage.getHeight()) / 2;
                graphics.drawImage(displayImage, imgX, imgY, null);

                // Draw count if obtained
                if (obtained) {
                    int count = logData.get(itemId);
                    graphics.setColor(new Color(255, 255, 0));
                    graphics.setFont(net.runelite.client.ui.FontManager.getRunescapeSmallFont());
                    String countStr = count > 1 ? String.valueOf(count) : "";
                    if (!countStr.isEmpty()) {
                        int strWidth = graphics.getFontMetrics().stringWidth(countStr);
                        // Draw shadow
                        graphics.setColor(Color.BLACK);
                        graphics.drawString(countStr, x + itemSize - strWidth - 1, y + itemSize - 1);
                        // Draw text
                        graphics.setColor(new Color(255, 255, 0));
                        graphics.drawString(countStr, x + itemSize - strWidth - 2, y + itemSize - 2);
                    }
                    graphics.setFont(net.runelite.client.ui.FontManager.getRunescapeFont());
                }
            }

            x += itemSize + padding;
            if (x + itemSize > xStart + width - 10) {
                x = xStart + 10;
                y += itemSize + padding;
            }
        }

        // Draw collection progress at the bottom
        int obtainedCount = 0;
        for (Integer itemId : items) {
            if (logData.containsKey(itemId)) {
                obtainedCount++;
            }
        }

        graphics.setColor(new Color(255, 152, 31));
        String progressText = "Obtained: " + obtainedCount + " / " + items.size();
        int progressY = (int) bounds.getY() + (int) bounds.getHeight() - 25;
        graphics.drawString(progressText, xStart + 10, progressY);
    }
}
