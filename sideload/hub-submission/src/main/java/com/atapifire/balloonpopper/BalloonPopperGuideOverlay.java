package com.atapifire.balloonpopper;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Singleton;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.PanelComponent;
import com.atapifire.balloonpopper.BalloonLogDatabase.LogCategory;
import com.atapifire.balloonpopper.BalloonLogDatabase.LogSubCategory;

@Singleton
public class BalloonPopperGuideOverlay extends Overlay {
    private final Client client;
    private final BalloonPopperPlugin plugin;
    private final ItemManager itemManager;
    private final PanelComponent panelComponent = new PanelComponent();

    // Interaction bounds
    private Rectangle closeButtonBounds;
    private final Map<LogCategory, Rectangle> tabBounds = new HashMap<>();
    private final Map<LogSubCategory, Rectangle> subCategoryBounds = new HashMap<>();
    private Rectangle bounds;

    private LogCategory currentCategory = LogCategory.F2P;
    private LogSubCategory currentSubCategory = LogSubCategory.MELEE;

    // Scrolling state
    private int scrollOffset = 0;
    private int maxScrollOffset = 0;
    private Rectangle gridBounds;

    @Inject
    public BalloonPopperGuideOverlay(Client client, BalloonPopperPlugin plugin, ItemManager itemManager) {
        this.client = client;
        this.plugin = plugin;
        this.itemManager = itemManager;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.HIGH);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        panelComponent.setPreferredSize(new Dimension(500, 350));
        panelComponent.setBorder(new Rectangle(2, 2, 2, 2));
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!plugin.isGuideVisible()) {
            return null;
        }

        // Window dimensions
        int width = 500;
        int height = 350;

        // Center with offset
        int centerX = (client.getCanvasWidth() / 2 - width / 2) - 50;
        int centerY = (client.getCanvasHeight() / 2 - height / 2) - 50;

        bounds = new Rectangle(centerX, centerY, width, height);

        // Draw main background
        graphics.setColor(new Color(64, 54, 44));
        graphics.fillRect(centerX, centerY, width, height);
        graphics.setColor(Color.BLACK);
        graphics.drawRect(centerX, centerY, width, height);

        // Draw title bar
        graphics.setColor(new Color(50, 40, 30));
        graphics.fillRect(centerX, centerY, width, 25);
        graphics.setColor(Color.BLACK);
        graphics.drawRect(centerX, centerY, width, 25);

        graphics.setColor(new Color(255, 152, 31));
        graphics.setFont(net.runelite.client.ui.FontManager.getRunescapeFont());
        graphics.drawString("Balloon Collection Log", centerX + 10, centerY + 18);

        // Draw close button
        closeButtonBounds = new Rectangle(centerX + width - 20, centerY + 5, 15, 15);
        graphics.setColor(Color.RED);
        graphics.fillRect(closeButtonBounds.x, closeButtonBounds.y, closeButtonBounds.width, closeButtonBounds.height);
        graphics.setColor(Color.WHITE);
        graphics.drawString("X", closeButtonBounds.x + 3, closeButtonBounds.y + 12);

        // Draw category tabs
        drawCategoryTabs(graphics, centerX, centerY, width);

        // Draw subcategory sidebar
        drawSubCategorySidebar(graphics, centerX, centerY, width, height);

        // Draw item grid
        drawItemGrid(graphics, centerX, centerY, width, height);

        return new Dimension(width, height);
    }

    private void drawCategoryTabs(Graphics2D graphics, int x, int y, int width) {
        int tabY = y + 30;
        int tabWidth = 70;
        int tabHeight = 20;
        int tabX = x + 10;

        tabBounds.clear();

        for (LogCategory category : LogCategory.values()) {
            Rectangle tabRect = new Rectangle(tabX, tabY, tabWidth, tabHeight);
            tabBounds.put(category, tabRect);

            boolean selected = category == currentCategory;
            boolean hovered = tabRect.contains(client.getMouseCanvasPosition().getX(),
                    client.getMouseCanvasPosition().getY());

            // Draw tab background
            if (selected) {
                graphics.setColor(new Color(255, 152, 31));
            } else if (hovered) {
                graphics.setColor(new Color(85, 72, 55));
            } else {
                graphics.setColor(new Color(65, 52, 35));
            }
            graphics.fillRect(tabX, tabY, tabWidth, tabHeight);

            // Draw tab border
            graphics.setColor(new Color(0, 0, 0, 150));
            graphics.drawRect(tabX, tabY, tabWidth, tabHeight);

            // Draw tab text
            graphics.setColor(selected ? Color.WHITE : new Color(200, 200, 200));
            String text = category.getName();
            int textWidth = graphics.getFontMetrics().stringWidth(text);
            graphics.drawString(text, tabX + (tabWidth - textWidth) / 2, tabY + 15);

            tabX += tabWidth + 2;
        }
    }

    private void drawSubCategorySidebar(Graphics2D graphics, int x, int y, int width, int height) {
        int sidebarX = x + 10;
        int sidebarY = y + 60;
        int sidebarWidth = 110;
        int sidebarHeight = height - 80;

        // Draw sidebar background
        graphics.setColor(new Color(0, 0, 0, 100));
        graphics.fillRect(sidebarX, sidebarY, sidebarWidth, sidebarHeight);
        graphics.setColor(new Color(60, 50, 40));
        graphics.drawRect(sidebarX, sidebarY, sidebarWidth, sidebarHeight);

        // Draw subcategories
        int subY = sidebarY + 5;
        int subHeight = 18;
        subCategoryBounds.clear();

        for (LogSubCategory subCategory : LogSubCategory.values()) {
            Set<Integer> items = BalloonLogDatabase.ITEMS.get(currentCategory).get(subCategory);

            // Special handling for OTHERS to always show it in MISC
            boolean isOthers = (currentCategory == LogCategory.MISC && subCategory == LogSubCategory.OTHERS);

            if (!isOthers && (items == null || items.isEmpty())) {
                continue;
            }

            Rectangle subRect = new Rectangle(sidebarX + 3, subY, sidebarWidth - 6, subHeight);
            subCategoryBounds.put(subCategory, subRect);

            boolean selected = subCategory == currentSubCategory;
            boolean hovered = subRect.contains(client.getMouseCanvasPosition().getX(),
                    client.getMouseCanvasPosition().getY());

            // Draw selection highlight
            if (selected) {
                graphics.setColor(new Color(255, 152, 31, 100));
                graphics.fillRect(subRect.x, subRect.y, subRect.width, subRect.height);
            }

            // Draw text
            graphics.setColor(selected ? new Color(255, 152, 31)
                    : (hovered ? Color.WHITE : new Color(200, 180, 150)));
            graphics.drawString(subCategory.getName(), subRect.x + 5, subRect.y + 14);

            subY += subHeight + 2;
        }
    }

    private void drawItemGrid(Graphics2D graphics, int x, int y, int width, int height) {
        int gridX = x + 130;
        int gridY = y + 60;
        int gridWidth = width - 150;
        int gridHeight = height - 90; // Leave space for progress

        gridBounds = new Rectangle(gridX, gridY, gridWidth, gridHeight);

        // Special handling for Stats tab
        if (currentCategory == LogCategory.STATS) {
            drawStatsTab(graphics, gridX, gridY, gridWidth, gridHeight);
            return;
        }

        // Draw grid background
        graphics.setColor(new Color(0, 0, 0, 80));
        graphics.fillRect(gridX, gridY, gridWidth, gridHeight);
        graphics.setColor(new Color(60, 50, 40));
        graphics.drawRect(gridX, gridY, gridWidth, gridHeight);

        // Set clipping to grid area
        java.awt.Shape oldClip = graphics.getClip();
        graphics.setClip(gridX, gridY, gridWidth, gridHeight);

        // Get items for current subcategory
        java.util.Set<Integer> itemSet = BalloonLogDatabase.ITEMS.get(currentCategory).get(currentSubCategory);
        java.util.List<Integer> items = (itemSet != null) ? new java.util.ArrayList<>(itemSet)
                : new java.util.ArrayList<>();
        Map<Integer, Integer> logMap = plugin.getCollectionLog();

        if (items.isEmpty()) {
            graphics.setClip(oldClip);
            graphics.setColor(Color.LIGHT_GRAY);
            graphics.drawString("No items in this category", gridX + 20, gridY + 30);
            return;
        }

        // Draw items in grid
        int itemSize = 36;
        int gap = 4;
        int itemsPerRow = (gridWidth - 20) / (itemSize + gap);
        if (itemsPerRow <= 0)
            itemsPerRow = 1;

        int totalRows = (int) Math.ceil((double) items.size() / itemsPerRow);
        int totalContentHeight = totalRows * (itemSize + gap) + 20;
        maxScrollOffset = Math.max(0, totalContentHeight - gridHeight);

        // Ensure scroll offset is valid
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));

        int itemX = gridX + 10;
        int itemY = gridY + 10 - scrollOffset;

        for (Integer itemId : items) {
            // Check if item is within visible range
            if (itemY + itemSize >= gridY && itemY <= gridY + gridHeight) {
                Integer count = logMap.get(itemId);
                boolean obtained = count != null;

                // Draw item slot background
                if (obtained) {
                    graphics.setColor(new Color(60, 55, 50));
                } else {
                    graphics.setColor(new Color(20, 20, 20));
                }
                graphics.fillRect(itemX, itemY, itemSize, itemSize);
                graphics.setColor(new Color(50, 50, 50));
                graphics.drawRect(itemX, itemY, itemSize, itemSize);

                // Draw item image
                BufferedImage image = itemManager.getImage(itemId, obtained ? count : 1, obtained);
                if (image != null) {
                    if (!obtained) {
                        image = net.runelite.client.util.ImageUtil.grayscaleImage(image);
                    }
                    graphics.drawImage(image, itemX + 2, itemY + 2, null);
                }
            }

            itemX += itemSize + gap;
            if (itemX + itemSize > gridX + gridWidth - 10) {
                itemX = gridX + 10;
                itemY += itemSize + gap;
            }
        }

        // Reset clip
        graphics.setClip(oldClip);

        // Draw scrollbar indicator if needed
        if (maxScrollOffset > 0) {
            int scrollbarWidth = 4;
            int scrollbarHeight = (int) (gridHeight * (gridHeight / (double) totalContentHeight));
            int scrollbarY = gridY + (int) (scrollOffset * (gridHeight - scrollbarHeight) / (double) maxScrollOffset);

            graphics.setColor(new Color(100, 100, 100, 150));
            graphics.fillRect(gridX + gridWidth - scrollbarWidth - 2, scrollbarY, scrollbarWidth, scrollbarHeight);
        }

        // Draw progress at bottom
        int obtainedCount = 0;
        for (Integer itemId : items) {
            if (logMap.containsKey(itemId)) {
                obtainedCount++;
            }
        }

        graphics.setColor(new Color(255, 152, 31));
        String progressText = "Obtained: " + obtainedCount + " / " + items.size();
        graphics.drawString(progressText, gridX + 10, y + height - 15);
    }

    public void handleMouseWheel(int rotation) {
        if (maxScrollOffset > 0) {
            scrollOffset += rotation * 20; // 20px per notch
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));
        }
    }

    public void handleClick(Point clickPoint) {
        if (closeButtonBounds != null && closeButtonBounds.contains(clickPoint)) {
            plugin.setGuideVisible(false);
            return;
        }

        // Check tab clicks
        for (Map.Entry<LogCategory, Rectangle> entry : tabBounds.entrySet()) {
            if (entry.getValue().contains(clickPoint)) {
                currentCategory = entry.getKey();
                // Reset to first available subcategory
                scrollOffset = 0;
                for (LogSubCategory sub : LogSubCategory.values()) {
                    java.util.Set<Integer> items = BalloonLogDatabase.ITEMS.get(currentCategory).get(sub);
                    boolean isOthers = (currentCategory == LogCategory.MISC && sub == LogSubCategory.OTHERS);
                    if (isOthers || (items != null && !items.isEmpty())) {
                        currentSubCategory = sub;
                        break;
                    }
                }
                return;
            }
        }

        // Check subcategory clicks
        for (Map.Entry<LogSubCategory, Rectangle> entry : subCategoryBounds.entrySet()) {
            if (entry.getValue().contains(clickPoint)) {
                currentSubCategory = entry.getKey();
                scrollOffset = 0;
                return;
            }
        }
    }

    public boolean isMouseOverOverlay(Point point) {
        return plugin.isGuideVisible() && bounds != null && bounds.contains(point);
    }

    public boolean isMouseOverGrid(Point point) {
        return plugin.isGuideVisible() && gridBounds != null && gridBounds.contains(point);
    }

    private void drawStatsTab(Graphics2D graphics, int x, int y, int width, int height) {
        if (currentSubCategory == LogSubCategory.TOTALS) {
            drawTotalsView(graphics, x, y, width, height);
        } else if (currentSubCategory == LogSubCategory.BALLOONS) {
            drawBalloonsView(graphics, x, y, width, height);
        }
    }

    private void drawTotalsView(Graphics2D graphics, int x, int y, int width, int height) {
        graphics.setColor(Color.WHITE);
        graphics.drawString("Overall Stats", x + 20, y + 30);

        int statY = y + 60;
        drawStatRow(graphics, x + 20, statY, "Total Balloons Popped:",
                String.valueOf(plugin.getBalloonsSingle() + plugin.getBalloonsDouble() + plugin.getBalloonsTriple()));
        drawStatRow(graphics, x + 20, statY + 25, "Total Experience Gained:",
                String.format("%,d XP", plugin.getTotalXp()));
    }

    private void drawBalloonsView(Graphics2D graphics, int x, int y, int width, int height) {
        int statY = y + 20;

        drawBalloonStat(graphics, x + 20, statY, 9935, plugin.getBalloonsYellow(), "Yellow Balloons", Color.YELLOW);
        drawBalloonStat(graphics, x + 20, statY + 50, 9937, plugin.getBalloonsRed(), "Red Balloons", Color.RED);
        drawBalloonStat(graphics, x + 20, statY + 100, 9936, plugin.getBalloonsBlue(), "Blue Balloons",
                new Color(100, 150, 255));
        drawBalloonStat(graphics, x + 20, statY + 150, 9939, plugin.getBalloonsGreen(), "Green Balloons", Color.GREEN);
        drawBalloonStat(graphics, x + 20, statY + 200, 9940, plugin.getBalloonsPurple(), "Purple Balloons",
                new Color(200, 100, 255));
        drawBalloonStat(graphics, x + 20, statY + 250, 9942, plugin.getBalloonsWhite(), "White Balloons", Color.WHITE);
    }

    private void drawStatRow(Graphics2D graphics, int x, int y, String label, String value) {
        graphics.setColor(Color.LIGHT_GRAY);
        graphics.drawString(label, x, y);
        graphics.setColor(new Color(255, 152, 31));
        graphics.drawString(value, x + 160, y);
    }

    private void drawBalloonStat(Graphics2D graphics, int x, int y, int itemId, int count, String name, Color color) {
        BufferedImage icon = itemManager.getImage(itemId);
        if (icon != null) {
            graphics.drawImage(icon, x, y, null);
        }

        graphics.setColor(color);
        graphics.drawString(name, x + 40, y + 15);
        graphics.setColor(Color.WHITE);
        graphics.drawString("Popped: " + count, x + 40, y + 32);
    }
}
