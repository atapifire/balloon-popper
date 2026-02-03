package com.atapifire.balloonpopper;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import com.atapifire.balloonpopper.BalloonLogDatabase.LogCategory;
import com.atapifire.balloonpopper.BalloonLogDatabase.LogSubCategory;
import net.runelite.client.game.ItemManager;

/**
 * Automatically categorizes tradeable items for the Balloon Popper collection
 * log
 * using the RuneLite ItemComposition API.
 */
@Slf4j
public class BalloonPopperItemCategorizer {

    private static final int MAX_ITEM_ID = 50000;

    // Balloon item IDs (these are special and go in Stats category)
    private static final int[] BALLOON_IDS = {
            9935, // YELLOW_BALLOON
            9937, // RED_BALLOON
            9936, // BLUE_BALLOON
            9939, // GREEN_BALLOON
            9940, // PURPLE_BALLOON
            9942, // ORIGAMI_BALLOON (was BLACK_BALLOON)
            9938, // BIRTHDAY_BALLOONS
            10034 // _10TH_BIRTHDAY_BALLOONS
    };

    private final Client client;
    private final ItemManager itemManager;

    public BalloonPopperItemCategorizer(Client client, ItemManager itemManager) {
        this.client = client;
        this.itemManager = itemManager;
    }

    /**
     * Scans all items and categorizes tradeable ones into the collection log.
     * This is the main entry point for populating the database.
     */
    public void categorizeAllItems() {
        log.info("Starting item categorization scan...");
        int categorizedCount = 0;
        int tradeableCount = 0;

        java.util.Map<LogCategory, Integer> categoryCounts = new java.util.HashMap<>();
        for (LogCategory cat : LogCategory.values()) {
            categoryCounts.put(cat, 0);
        }

        for (int itemId = 0; itemId < MAX_ITEM_ID; itemId++) {
            ItemComposition composition = itemManager.getItemComposition(itemId);

            if (composition == null || composition.getName() == null || composition.getName().equals("null")) {
                continue;
            }

            // EXCLUDE noted items, placeholders, and links
            if (composition.getNote() != -1 || composition.getPlaceholderTemplateId() != -1) {
                continue;
            }

            // Special handling for balloons - do this BEFORE tradeable check
            if (isBalloon(itemId)) {
                BalloonLogDatabase.ITEMS.get(LogCategory.STATS).get(LogSubCategory.BALLOONS).add(itemId);
                categorizedCount++;
                categoryCounts.put(LogCategory.STATS, categoryCounts.get(LogCategory.STATS) + 1);
                continue;
            }

            // Special handling for currencies - do this BEFORE tradeable check (e.g. Coins)
            String name = composition.getName().toLowerCase();
            if (isCurrency(itemId, name)) {
                BalloonLogDatabase.ITEMS.get(LogCategory.MISC).get(LogSubCategory.CURRENCY).add(itemId);
                categorizedCount++;
                categoryCounts.put(LogCategory.MISC, categoryCounts.get(LogCategory.MISC) + 1);
                continue;
            }

            // Check if item is tradeable (can appear in Party Room drops)
            // or if it's a member item (which might return false for tradeable on F2P
            // worlds)
            if (!composition.isTradeable() && !composition.isMembers()) {
                continue;
            }

            tradeableCount++;

            // Categorize the item
            CategoryResult result = categorizeItem(itemId, composition);
            if (result != null) {
                BalloonLogDatabase.ITEMS.get(result.category).get(result.subCategory).add(itemId);
                categorizedCount++;
                categoryCounts.put(result.category, categoryCounts.get(result.category) + 1);
            }
        }

        log.info("Categorization complete: {} tradeable items found, {} categorized", tradeableCount, categorizedCount);
        for (java.util.Map.Entry<LogCategory, Integer> entry : categoryCounts.entrySet()) {
            log.info("Category {}: {} items", entry.getKey(), entry.getValue());
        }
    }

    /**
     * Categorizes a single item based on its properties.
     */
    private CategoryResult categorizeItem(int itemId, ItemComposition composition) {
        String name = composition.getName().toLowerCase();

        // Improved member detection
        boolean isMembers = composition.isMembers() || isExplicitMemberItem(name);

        // Rare items (party hats, holiday items)
        if (isRare(name)) {
            return new CategoryResult(LogCategory.MISC, LogSubCategory.RARES);
        }

        // Clue scroll rewards
        CategoryResult clueResult = categorizeClueReward(name);
        if (clueResult != null) {
            return clueResult;
        }

        // Equipment and resources
        LogCategory category = isMembers ? LogCategory.MEMBER : LogCategory.F2P;

        // Check if it's equipment
        LogSubCategory equipmentType = getEquipmentType(composition, name);
        if (equipmentType != null) {
            return new CategoryResult(category, equipmentType);
        }

        // Check if it's a resource
        if (isResource(name)) {
            return new CategoryResult(category, LogSubCategory.RESOURCES);
        }

        // Default to OTHERS within the correct category (F2P/MEMBER)
        // If it was tradeable but we couldn't find a subcategory, put it in OTHERS
        return new CategoryResult(category, LogSubCategory.OTHERS);
    }

    private boolean isExplicitMemberItem(String name) {
        return name.contains("dragon ") || name.contains("abyssal") ||
                name.contains("barrows") || name.contains("bandos") ||
                name.contains("armadyl") || name.contains("godsword") ||
                name.contains("whip") || name.contains("dark bow") ||
                name.contains("trident") || name.contains("blowpipe") ||
                name.contains("anguish") || name.contains("torture") ||
                name.contains("tormented") || name.contains("zenyte") ||
                name.contains("obsidian") || name.contains("fighter torso") ||
                name.contains("granite") || name.contains("rune pouch") ||
                name.contains("toxic") || name.contains("serpentine") ||
                name.contains("magma") || name.contains("tanzanite") ||
                name.contains("super combat") || name.contains("prayer potion") ||
                name.contains("stamina") || name.contains("saradomin brew") ||
                name.contains("super restore") || name.contains("ranarr") ||
                name.contains("snapdragon") || name.contains("torstol") ||
                name.contains("angelfish") || name.contains("karambwan") ||
                name.contains("manta ray") || name.contains("shark") ||
                name.contains("poison") || name.contains("venom") ||
                name.contains("cannonball") || name.contains("zulrah") ||
                name.contains("vorkath") || name.contains("raids") ||
                name.contains("theatre") || name.contains("verzik") ||
                name.contains("justiciar") || name.contains("ghrazi") ||
                name.contains("sang") || name.contains("tbow") ||
                name.contains("twisted bow") || name.contains("ancestral") ||
                name.contains("masori") || name.contains("torva") ||
                name.contains("nihil") || name.contains("zaryte") ||
                name.contains("tumeken") || name.contains("elidinis") ||
                name.contains("osmumten") || name.contains("fang") ||
                name.contains("lightbearer") || name.contains("bellator") ||
                name.contains("magus") || name.contains("ultor") ||
                name.contains("venator") || name.contains("virtus") ||
                name.contains("soulreaper") || name.contains("scythe") ||
                name.contains("kodai") || name.contains("elder maul") ||
                name.contains("avernic") || name.contains("blighted") ||
                name.contains("super combat") || name.contains("anti-venom") ||
                name.contains("extended") || name.contains("divine") ||
                name.contains("bastion") || name.contains("battlemage") ||
                name.contains("stamina");
    }

    private boolean isBalloon(int itemId) {
        for (int balloonId : BALLOON_IDS) {
            if (itemId == balloonId) {
                return true;
            }
        }
        return false;
    }

    private boolean isCurrency(int itemId, String name) {
        return itemId == 995 || // Coins
                itemId == 13204 || // Platinum token
                itemId == 6306 || // Trading sticks
                itemId == 1464 || // Archery ticket
                itemId == 21555 || // Numulite
                itemId == 23204 || // Survival token
                name.equals("coins") || name.equals("platinum token") ||
                name.equals("trading sticks") || name.equals("numulite");
    }

    private boolean isRare(String name) {
        return name.contains("partyhat") || name.contains("party hat") ||
                name.contains("christmas cracker") || name.contains("pumpkin") ||
                name.contains("easter egg") || name.contains("disk of returning") ||
                name.contains("half full wine jug");
    }

    private CategoryResult categorizeClueReward(String name) {
        // Clue caskets
        if (name.contains("reward casket")) {
            if (name.contains("beginner"))
                return new CategoryResult(LogCategory.CLUES, LogSubCategory.BEGINNER);
            if (name.contains("easy"))
                return new CategoryResult(LogCategory.CLUES, LogSubCategory.EASY);
            if (name.contains("medium"))
                return new CategoryResult(LogCategory.CLUES, LogSubCategory.MEDIUM);
            if (name.contains("hard"))
                return new CategoryResult(LogCategory.CLUES, LogSubCategory.HARD);
            if (name.contains("elite"))
                return new CategoryResult(LogCategory.CLUES, LogSubCategory.ELITE);
            if (name.contains("master"))
                return new CategoryResult(LogCategory.CLUES, LogSubCategory.MASTER);
        }

        // Known clue rewards by tier
        if (isBeginnerClueReward(name))
            return new CategoryResult(LogCategory.CLUES, LogSubCategory.BEGINNER);
        if (isEasyClueReward(name))
            return new CategoryResult(LogCategory.CLUES, LogSubCategory.EASY);
        if (isMediumClueReward(name))
            return new CategoryResult(LogCategory.CLUES, LogSubCategory.MEDIUM);
        if (isHardClueReward(name))
            return new CategoryResult(LogCategory.CLUES, LogSubCategory.HARD);
        if (isEliteClueReward(name))
            return new CategoryResult(LogCategory.CLUES, LogSubCategory.ELITE);
        if (isMasterClueReward(name))
            return new CategoryResult(LogCategory.CLUES, LogSubCategory.MASTER);

        return null;
    }

    private boolean isBeginnerClueReward(String name) {
        return name.contains("mole slipper") || name.contains("frog slipper") ||
                name.contains("bear feet") || name.contains("sandwich lady");
    }

    private boolean isEasyClueReward(String name) {
        return name.contains("flared trousers") || name.contains("bob's") ||
                name.contains("wizard boots") || name.contains("powdered wig") ||
                (name.contains("amulet") && name.contains("(t)"));
    }

    private boolean isMediumClueReward(String name) {
        return name.contains("ranger boots") || name.contains("holy sandals") ||
                name.contains("climbing boots (g)") || name.contains("cabbage cape") ||
                (name.contains("adamant") && name.contains("(g)"));
    }

    private boolean isHardClueReward(String name) {
        return name.contains("robin hood hat") ||
                (name.contains("dhide")
                        && (name.contains("zamorak") || name.contains("saradomin") || name.contains("guthix")))
                ||
                (name.contains("rune") && name.contains("(g)"));
    }

    private boolean isEliteClueReward(String name) {
        return name.contains("ranger gloves") ||
                (name.contains("dragon") && name.contains("(g)")) ||
                (name.contains("3rd age") && (name.contains("range") || name.contains("mage")));
    }

    private boolean isMasterClueReward(String name) {
        return name.contains("spectacles") || name.contains("eye patch") ||
                (name.contains("3rd age")
                        && (name.contains("pickaxe") || name.contains("platebody") || name.contains("platelegs")));
    }

    private LogSubCategory getEquipmentType(ItemComposition composition, String name) {
        String[] actions = composition.getInventoryActions();
        boolean isEquipment = false;

        // Check if item can be equipped
        if (actions != null) {
            for (String action : actions) {
                if (action != null && (action.equals("Wield") || action.equals("Wear") || action.equals("Equip"))) {
                    isEquipment = true;
                    break;
                }
            }
        }

        if (!isEquipment) {
            // Check based on name if inventory actions are missing (rare for tradeable
            // gear)
            if (name.contains("platebody") || name.contains("platelegs") || name.contains("sword") ||
                    name.contains("bow") || name.contains("staff")) {
                isEquipment = true;
            } else {
                return null;
            }
        }

        // Determine equipment type based on name
        if (isMeleeEquipment(name)) {
            return LogSubCategory.MELEE;
        } else if (isMagicEquipment(name)) {
            return LogSubCategory.MAGIC;
        } else if (isRangeEquipment(name)) {
            return LogSubCategory.RANGE;
        }

        return null;
    }

    private boolean isMeleeEquipment(String name) {
        return name.contains("sword") || name.contains("scimitar") || name.contains("dagger") ||
                name.contains("whip") || name.contains("platebody") || name.contains("platelegs") ||
                name.contains("plateskirt") || name.contains("chainbody") || name.contains("full helm") ||
                name.contains("kiteshield") || name.contains("defender") || name.contains("tassets") ||
                name.contains("chestplate") || name.contains("2h") || name.contains("godsword") ||
                name.contains("halberd") || name.contains("spear") || name.contains("mace") ||
                name.contains("warhammer") || name.contains("battleaxe") || name.contains("maul") ||
                name.contains("hasta") || name.contains("claws") || name.contains("partisan") ||
                name.contains("axe") || name.contains("pickaxe") || name.contains("halberd") ||
                name.contains("lance") || name.contains("rapier") || name.contains("cudgel") ||
                name.contains("bulwark") || name.contains("shield");
    }

    private boolean isMagicEquipment(String name) {
        return name.contains("robe") || name.contains("staff") || name.contains("wand") ||
                name.contains("wizard") || name.contains("mystic") || name.contains("ahrim") ||
                name.contains("ancestral") || name.contains("infinity") || name.contains("eternal") ||
                name.contains("occult") || name.contains("tome") || name.contains("book of") ||
                name.contains("virtus") || name.contains("blue robe") || name.contains("zamorak robe") ||
                name.contains("sanguinesti") || name.contains("trident") || name.contains("shadow") ||
                name.contains("kodai") || name.contains("brimstone") || name.contains("magus");
    }

    private boolean isRangeEquipment(String name) {
        return name.contains("bow") || name.contains("crossbow") || name.contains("arrow") ||
                name.contains("bolt") || name.contains("dhide") || name.contains("d'hide") ||
                name.contains("leather") || name.contains("coif") || name.contains("armadyl") ||
                name.contains("karil") || name.contains("blowpipe") || name.contains("chinchompa") ||
                name.contains("pegasian") || name.contains("masori") || name.contains("vamb") ||
                name.contains("knives") || name.contains("dart") || name.contains("thrownaxe") ||
                name.contains("ballista") || name.contains("venator") || name.contains("zaryte") ||
                name.contains("cannon") || name.contains("godhide");
    }

    private boolean isResource(String name) {
        return name.contains("logs") || name.contains("ore") || name.contains("bar") ||
                name.contains("raw ") || name.contains("fish") || name.contains("herb") ||
                name.contains("seed") || name.contains("potion") || name.contains("essence") ||
                name.contains("rune") || name.contains("arrow") || name.contains("bolt") ||
                name.contains("plank") || name.contains("hide") || name.contains("leather") ||
                name.contains("gem") || name.contains("grimy");
    }

    /**
     * Helper class to store categorization results
     */
    private static class CategoryResult {
        final LogCategory category;
        final LogSubCategory subCategory;

        CategoryResult(LogCategory category, LogSubCategory subCategory) {
            this.category = category;
            this.subCategory = subCategory;
        }
    }
}
