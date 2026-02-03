package com.atapifire.balloonpopper.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.ItemManager;
import com.atapifire.balloonpopper.BalloonPopperPlugin;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;
import com.atapifire.balloonpopper.BalloonLogDatabase;
import com.atapifire.balloonpopper.BalloonLogDatabase.LogCategory;
import com.atapifire.balloonpopper.BalloonLogDatabase.LogSubCategory;

public class BalloonPopperPanel extends PluginPanel {
    private final BalloonPopperPlugin plugin;
    private final ItemManager itemManager;
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final JPanel overviewPanel = new JPanel();
    private final JPanel collectionLogPanel = new JPanel();
    private final JPanel categoryListPanel = new JPanel();
    private final JPanel itemGrid = new JPanel();
    private final JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));

    private final Map<LogCategory, JPanel> categoryTabs = new HashMap<>();
    private final Map<LogSubCategory, JPanel> subCategoryButtons = new HashMap<>();

    private LogCategory currentCategory = LogCategory.F2P;
    private LogSubCategory currentSubCategory = LogSubCategory.MELEE;

    @Inject
    public BalloonPopperPanel(BalloonPopperPlugin plugin, ItemManager itemManager) {
        super();
        this.plugin = plugin;
        this.itemManager = itemManager;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        tabbedPane.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        tabbedPane.setForeground(Color.WHITE);

        setupOverview();
        setupCollectionLogLayout(); // Build structure once

        tabbedPane.addTab("Overview", overviewPanel);
        tabbedPane.addTab("Collection Log", collectionLogPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // Initial populate
        selectCategory(LogCategory.F2P);
    }

    private void setupOverview() {
        overviewPanel.setLayout(new BorderLayout());
        overviewPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        overviewPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Balloon Popping Guide", SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(16f));
        overviewPanel.add(title, BorderLayout.NORTH);

        String text = "<html><body style='color: #DDDDDD; width: 180px;'>" +
                "<br><b>Party Pete's Tradition</b><br>" +
                "Falador is home to the world-famous Party Room, where Party Pete hosts the most exciting events in Gielinor!<br><br>"
                +
                "<b>How to Train</b><br>" +
                "1. Head to the Falador Party Room.<br>" +
                "2. Deposit items or wait for a drop.<br>" +
                "3. Pull the lever and watch balloons fall!<br>" +
                "4. Burst balloons to earn Balloon Popping XP.<br><br>" +
                "<b>XP Rates</b><br>" +
                "• Single Balloon: 3 XP<br>" +
                "• Double Balloon: 6 XP<br>" +
                "• Triple Balloon: 9 XP<br><br>" +
                "<i>Pop 'em all and become the Party Legend!</i>" +
                "</body></html>";

        JLabel content = new JLabel(text);
        overviewPanel.add(content, BorderLayout.CENTER);
    }

    private void setupCollectionLogLayout() {
        collectionLogPanel.setLayout(new BorderLayout());
        collectionLogPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // --- Top Tabs (Categories) ---
        tabPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        tabPanel.setBorder(new EmptyBorder(0, 0, 1, 0));
        for (LogCategory category : LogCategory.values()) {
            JPanel tab = new JPanel();
            tab.setBackground(ColorScheme.DARK_GRAY_COLOR);
            tab.setBorder(new EmptyBorder(5, 8, 5, 8));

            JLabel tabLabel = new JLabel(category.getName());
            tabLabel.setForeground(Color.WHITE);
            tab.add(tabLabel);

            tab.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    selectCategory(category);
                }

                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    if (category != currentCategory) {
                        tab.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
                    }
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    if (category != currentCategory) {
                        tab.setBackground(ColorScheme.DARK_GRAY_COLOR);
                    }
                }
            });

            categoryTabs.put(category, tab);
            tabPanel.add(tab);
        }
        collectionLogPanel.add(tabPanel, BorderLayout.NORTH);

        // --- Left Sidebar (Subcategories) ---
        categoryListPanel.setLayout(new BoxLayout(categoryListPanel, BoxLayout.Y_AXIS));
        categoryListPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        categoryListPanel.setBorder(new EmptyBorder(2, 2, 2, 2));

        JScrollPane categoryScroll = new JScrollPane(categoryListPanel);
        categoryScroll.setPreferredSize(new Dimension(85, 0));
        categoryScroll.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        categoryScroll.setBorder(new javax.swing.border.MatteBorder(0, 0, 0, 1, ColorScheme.DARK_GRAY_COLOR));
        categoryScroll.getVerticalScrollBar().setUnitIncrement(16);
        collectionLogPanel.add(categoryScroll, BorderLayout.WEST);

        // --- Center Grid (Items) ---
        itemGrid.setLayout(new GridLayout(0, 4, 3, 3));
        itemGrid.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        itemGrid.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel gridWrapper = new JPanel(new BorderLayout());
        gridWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        gridWrapper.add(itemGrid, BorderLayout.NORTH);

        JScrollPane itemScroll = new JScrollPane(gridWrapper);
        itemScroll.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        itemScroll.getVerticalScrollBar().setUnitIncrement(16);
        itemScroll.setBorder(null);
        itemScroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        collectionLogPanel.add(itemScroll, BorderLayout.CENTER);
    }

    private void selectCategory(LogCategory category) {
        currentCategory = category;

        // Update top tabs styling
        for (Map.Entry<LogCategory, JPanel> entry : categoryTabs.entrySet()) {
            boolean selected = entry.getKey() == category;
            entry.getValue().setBackground(selected ? ColorScheme.BRAND_ORANGE : ColorScheme.DARK_GRAY_COLOR);
        }

        // Rebuild subcategory list
        categoryListPanel.removeAll();
        subCategoryButtons.clear();

        LogSubCategory firstAvailable = null;

        for (LogSubCategory sub : LogSubCategory.values()) {
            java.util.Set<Integer> items = BalloonLogDatabase.ITEMS.get(category).get(sub);

            // Special handling for OTHERS to always show it in MISC
            boolean isOthers = (category == LogCategory.MISC && sub == LogSubCategory.OTHERS);

            if (!isOthers && (items == null || items.isEmpty()))
                continue;

            if (firstAvailable == null)
                firstAvailable = sub;

            JPanel subPanel = new JPanel(new BorderLayout());
            subPanel.setMinimumSize(new Dimension(80, 24));
            subPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 24));
            subPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
            subPanel.setBorder(new EmptyBorder(0, 5, 0, 5));

            JLabel label = new JLabel(sub.getName());
            label.setFont(label.getFont().deriveFont(11f));
            label.setForeground(Color.WHITE);
            subPanel.add(label, BorderLayout.CENTER);

            subPanel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    selectSubCategory(sub);
                }

                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    if (sub != currentSubCategory) {
                        subPanel.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
                    }
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    if (sub != currentSubCategory) {
                        subPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
                    }
                }
            });

            subCategoryButtons.put(sub, subPanel);
            categoryListPanel.add(subPanel);
            categoryListPanel.add(Box.createRigidArea(new Dimension(0, 1)));
        }

        categoryListPanel.revalidate();
        categoryListPanel.repaint();

        if (firstAvailable != null) {
            selectSubCategory(firstAvailable);
        }
    }

    private void selectSubCategory(LogSubCategory sub) {
        currentSubCategory = sub;

        // Update sidebar styling
        for (Map.Entry<LogSubCategory, JPanel> entry : subCategoryButtons.entrySet()) {
            boolean selected = entry.getKey() == sub;
            entry.getValue().setBackground(selected ? ColorScheme.BRAND_ORANGE : ColorScheme.DARK_GRAY_COLOR);
        }

        updateCollectionLog();
    }

    public void updateCollectionLog() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::updateCollectionLog);
            return;
        }

        itemGrid.removeAll();
        Map<Integer, Integer> logData = plugin.getCollectionLog();

        if (currentSubCategory == LogSubCategory.TOTALS) {
            itemGrid.setLayout(new GridLayout(0, 1, 5, 5));

            JLabel header = new JLabel("Balloon Statistics");
            header.setForeground(ColorScheme.BRAND_ORANGE);
            header.setHorizontalAlignment(SwingConstants.CENTER);
            itemGrid.add(header);

            addStatLabel("Single Bursts", plugin.getBalloonsSingle());
            addStatLabel("Double Bursts", plugin.getBalloonsDouble());
            addStatLabel("Triple Bursts", plugin.getBalloonsTriple());

            itemGrid.add(Box.createRigidArea(new Dimension(0, 10)));
            addStatLabel("Total XP", plugin.getTotalXp());

        } else if (currentSubCategory == LogSubCategory.OTHERS) {
            itemGrid.setLayout(new GridLayout(0, 4, 3, 3));

            // Find all items in logData that AREN'T in any category
            java.util.Set<Integer> catalogedItems = new java.util.HashSet<>();
            for (Map<LogSubCategory, java.util.Set<Integer>> subMap : BalloonLogDatabase.ITEMS.values()) {
                for (java.util.Set<Integer> set : subMap.values()) {
                    catalogedItems.addAll(set);
                }
            }

            java.util.List<Integer> otherItems = new java.util.ArrayList<>();
            for (Integer id : logData.keySet()) {
                if (!catalogedItems.contains(id)) {
                    otherItems.add(id);
                }
            }

            if (otherItems.isEmpty()) {
                JLabel empty = new JLabel("No other items collected", SwingConstants.CENTER);
                empty.setForeground(Color.GRAY);
                itemGrid.add(empty);
            } else {
                for (int itemId : otherItems) {
                    Integer count = logData.get(itemId);
                    itemGrid.add(createItemPanel(itemId, count, true));
                }
            }
        } else {
            itemGrid.setLayout(new GridLayout(0, 4, 3, 3));
            java.util.Set<Integer> categoryItems = BalloonLogDatabase.ITEMS.get(currentCategory)
                    .get(currentSubCategory);

            if (categoryItems != null && !categoryItems.isEmpty()) {
                for (int itemId : categoryItems) {
                    Integer count = logData.get(itemId);
                    boolean obtained = count != null;
                    itemGrid.add(createItemPanel(itemId, count, obtained));
                }
            }
        }
        itemGrid.revalidate();
        itemGrid.repaint();

        // Force complete hierarchy validation
        if (itemGrid.getParent() != null) {
            itemGrid.getParent().revalidate();
            itemGrid.getParent().repaint();
        }
        collectionLogPanel.revalidate();
        collectionLogPanel.repaint();
    }

    private JPanel createItemPanel(int itemId, Integer count, boolean obtained) {
        JPanel itemPanel = new JPanel(new BorderLayout());
        itemPanel.setPreferredSize(new Dimension(42, 42));

        if (obtained) {
            itemPanel.setBackground(new Color(50, 45, 40));
            itemPanel.setBorder(new javax.swing.border.LineBorder(new Color(90, 75, 55), 1));
        } else {
            itemPanel.setBackground(new Color(25, 25, 25));
            itemPanel.setBorder(new javax.swing.border.LineBorder(new Color(50, 50, 50), 1));
        }

        // Use ItemManager to get the image with quantity text
        AsyncBufferedImage icon = itemManager.getImage(itemId, obtained ? count : 1, obtained);
        JLabel iconLabel = new JLabel();
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        icon.addTo(iconLabel);

        if (!obtained) {
            iconLabel.setToolTipText("Not obtained");
            iconLabel.setEnabled(false);
        } else {
            iconLabel.setToolTipText("Obtained: " + count);
        }

        itemPanel.add(iconLabel, BorderLayout.CENTER);
        return itemPanel;
    }

    private void addStatLabel(String name, long value) {
        JLabel label = new JLabel(name + ": " + String.format("%,d", value));
        label.setForeground(Color.WHITE);
        label.setBorder(new EmptyBorder(2, 5, 2, 5));
        itemGrid.add(label);
    }
}
