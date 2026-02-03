package com.atapifire.balloonpopper;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import net.runelite.api.ItemID;

public class BalloonLogDatabase {
        public enum LogCategory {
                F2P("F2P"),
                MEMBER("Member"),
                CLUES("Clues"),
                STATS("Stats"),
                MISC("Misc");

                @Getter
                private final String name;

                LogCategory(String name) {
                        this.name = name;
                }
        }

        public enum LogSubCategory {
                // Combat
                MELEE("Melee"),
                MAGIC("Magic"),
                RANGE("Range"),

                // Skilling
                RESOURCES("Resources"),

                // Clues
                BEGINNER("Beginner"),
                EASY("Easy"),
                MEDIUM("Medium"),
                HARD("Hard"),
                ELITE("Elite"),
                MASTER("Master"),

                // Misc
                RARES("Rares"),
                CURRENCY("Currency"),
                OTHERS("Others"),

                // Stats
                TOTALS("Totals"),
                BALLOONS("Balloons");

                @Getter
                private final String name;

                LogSubCategory(String name) {
                        this.name = name;
                }
        }

        public static final Map<LogCategory, Map<LogSubCategory, Set<Integer>>> ITEMS = new HashMap<>();

        private static boolean itemsPopulated = false;

        static {
                // Initialize maps immediately so they are never null
                for (LogCategory cat : LogCategory.values()) {
                        ITEMS.put(cat, new HashMap<>());
                        for (LogSubCategory sub : LogSubCategory.values()) {
                                ITEMS.get(cat).put(sub, new LinkedHashSet<>());
                        }
                }

                // Add placeholder for subcategories to ensure they show in sidebar
                add(LogCategory.STATS, LogSubCategory.TOTALS, 713); // CLUE_SCROLL as placeholder
                add(LogCategory.STATS, LogSubCategory.BALLOONS, 9935); // YELLOW_BALLOON as placeholder
                add(LogCategory.MISC, LogSubCategory.CURRENCY, 995); // COINS as placeholder
                add(LogCategory.MEMBER, LogSubCategory.MELEE, 1155); // BRONZE_FULL_HELM as placeholder
                add(LogCategory.MEMBER, LogSubCategory.RANGE, 1129); // LEATHER_BODY as placeholder
                add(LogCategory.MEMBER, LogSubCategory.MAGIC, 577); // WIZARD_ROBE as placeholder
        }

        /**
         * Populate the collection log database with all tradeable items.
         * This MUST be called on the client thread (e.g., in GameStateChanged event).
         */
        public static void populateItems(net.runelite.api.Client client,
                        net.runelite.client.game.ItemManager itemManager) {
                if (itemsPopulated) {
                        return;
                }

                // Use categorizer to populate all tradeable items
                BalloonPopperItemCategorizer categorizer = new BalloonPopperItemCategorizer(client, itemManager);
                categorizer.categorizeAllItems();

                itemsPopulated = true;
        }

        private static void add(LogCategory cat, LogSubCategory sub, Integer... ids) {
                for (Integer id : ids) {
                        ITEMS.get(cat).get(sub).add(id);
                }
        }
}
