package de.halfminer.hmc.module;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.halfminer.hmc.module.sell.SellCycleRefreshEvent;
import de.halfminer.hmc.module.sell.Sellable;
import de.halfminer.hmc.module.sell.SellableMap;
import de.halfminer.hms.cache.CustomitemCache;
import de.halfminer.hms.cache.CustomtextCache;
import de.halfminer.hms.exceptions.CachingException;
import de.halfminer.hms.exceptions.HookException;
import de.halfminer.hms.exceptions.ItemCacheException;
import de.halfminer.hms.handler.storage.DataType;
import de.halfminer.hms.manageable.Disableable;
import de.halfminer.hms.manageable.Sweepable;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.StringArgumentSeparator;
import de.halfminer.hms.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * - Full dynamic sell system
 *   - Cycle based selling, every given minutes new items will be chosen and all prices will reset
 *     - Cycles are persistent through restarts
 *     - Broadcast message when new cycle starts
 *   - Reads items to sell from config: Their Material, durability/id, base price per unit and name of item
 *     - Items need to be grouped, group name determines how many of given items will land in a given cycle
 *       - For example out of 20 items in group '5', 5 will be randomly selected
 *     - Variance can be added to base price of items via config for more dynamic pricing
 *   - Includes GUI, must be accessed via /sell command
 *     - First line in GUI can be fully configured via customitems.txt and config to set a custom command per slot
 *       - By default line will be filled with stained glass pane
 *       - For example a custom button to toggle auto selling can be added
 *   - Price will be adjusted by a configurable amount every given amount (also configurable)
 *   - Custom revenue multiplier per player level (hms.level)
 * - Auto sells chests on inventory close
 *   - Needs to be toggled
 * - Items with any item meta won't be sold
 */
@SuppressWarnings("unused")
public class ModSell extends HalfminerModule implements Disableable, Listener, Sweepable {

    private final Cache<Player, Boolean> autoSellingCache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .weakKeys()
            .concurrencyLevel(1)
            .build();

    private Map<Inventory, Player> activeMenus;

    private Map<Integer, String> menuCommands;
    private SellableMap sellableMap;
    private List<Double> levelRewardMultipliers;


    @EventHandler(ignoreCancelled = true)
    public void onMenuClick(InventoryClickEvent e) {

        Inventory invClicked = e.getInventory();
        if (activeMenus.containsKey(invClicked)) {

            e.setCancelled(true);
            int slot = e.getRawSlot();

            if (e.getRawSlot() != e.getSlot()) {
                return;
            }

            Player player = (Player) e.getWhoClicked();

            if (menuCommands.containsKey(slot)) {
                player.chat(menuCommands.get(slot));
                player.closeInventory();
            } else if (slot >= 18 && sellMaterialAndReward(slot - 18, player)) {
                player.closeInventory();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMenuClose(InventoryCloseEvent e) {

        activeMenus.remove(e.getInventory());

        if (e.getPlayer() instanceof Player
                && autoSellingCache.getIfPresent(e.getPlayer()) != null
                && (e.getInventory().getHolder() instanceof Chest
                || e.getInventory().getHolder() instanceof DoubleChest)) {

            Inventory chest = e.getInventory();
            ItemStack item = chest.getItem(0);
            if (item != null) {
                Sellable toBeSold = sellableMap.getSellableFromItemStack(item);
                if (toBeSold != null) {
                    Player seller = (Player) e.getPlayer();
                    int amountSold = sellMaterial(toBeSold, e.getInventory());
                    rewardPlayer(seller, toBeSold, amountSold);
                }
            }
        }
    }

    @EventHandler
    public void onCycleRefresh(SellCycleRefreshEvent e) {
        closeActiveInventories();
    }

    public void showSellMenu(Player player) {

        Inventory inv = server.createInventory(player, 45, MessageBuilder.returnMessage("modSellMenuTitle", hmc));

        // top line (menu controls), first prefill first line with stained glass
        ItemStack spacer = new ItemStack(Material.STAINED_GLASS_PANE);
        Utils.setDisplayName(spacer, ChatColor.RESET.toString());
        for (int slot = 0; slot < 9; slot++) {
            inv.setItem(slot, spacer);
        }

        // read menu items in first line from customitem cache
        try {
            CustomtextCache cache = coreStorage.getCache("customitems.txt");
            CustomitemCache itemCache = new CustomitemCache(cache);
            long timeLeftCycle = (sellableMap.getCycleTimeLeft() / 60) + 1;
            Map<String, String> placeholders = Collections.singletonMap("%CYCLEMINUTES%", String.valueOf(timeLeftCycle));

            for (int i = 0; i < 9; i++) {
                String key = "sellmenu-" + (i + 1);
                try {
                    inv.setItem(i, itemCache.getItem(key, player, 1, placeholders));
                } catch (ItemCacheException e) {
                    if (e.getReason().equals(ItemCacheException.Reason.ITEM_SYNTAX_ERROR)) {
                        MessageBuilder.create("modSellMenuSyntaxError")
                                .addPlaceholderReplace("%ITEM%", key)
                                .logMessage(Level.WARNING);
                    }
                }
            }
        } catch (CachingException e) {
            MessageBuilder.create("modSellMenuCustomItemsError", hmc).logMessage(Level.WARNING);
        }

        // sellables section (starts in 3rd menu row)
        String multiplier = String.valueOf(getMultiplier(player));
        List<Sellable> sellables = sellableMap.getCycleSellables();
        for (int i = 0; i < sellables.size(); i++) {

            Sellable sellable = sellables.get(i);
            ItemStack currentItem = sellable.getItemStack();

            int amountUntilNextIncrease = sellable.getAmountUntilNextIncrease();

            String stackData = MessageBuilder.create("modSellMenuStack", hmc)
                    .addPlaceholderReplace("%NAME%", sellable.getMessageName())
                    .addPlaceholderReplace("%MULTIPLIER%", multiplier)
                    .addPlaceholderReplace("%AMOUNT%", String.valueOf(sellable.getCurrentUnitAmount()))
                    .addPlaceholderReplace("%NEXTINCREASE%", String.valueOf(sellable.getAmountUntilNextIncrease()))
                    .returnMessage();

            // itemname - revenue lore - increase lore
            String[] stackDataSplit = stackData.split(Pattern.quote("|"));

            List<String> lore = new ArrayList<>();
            lore.addAll(Arrays.asList(stackDataSplit).subList(1, stackDataSplit.length));

            Utils.setDisplayName(currentItem, stackDataSplit[0]);
            Utils.setItemLore(currentItem, lore);

            inv.setItem(i + 18, currentItem);
        }

        player.openInventory(inv);
        activeMenus.put(inv, player);
    }

    public boolean toggleAutoSell(Player player) {

        boolean contains = autoSellingCache.getIfPresent(player) != null;

        if (contains) {
            autoSellingCache.invalidate(player);
        } else {
            autoSellingCache.put(player, true);
        }

        return !contains;
    }

    public void startNewCycle() {
        sellableMap.startNewCycle();
    }

    public boolean sellMaterialAndReward(int index, Player toReward) {

        Sellable toSell = sellableMap.getSellableAtSlot(index);
        if (toSell != null) {
            int sold = sellMaterial(toSell, toReward.getInventory());
            if (sold == 0) {
                MessageBuilder.create("modSellNotInInv", hmc, "Sell")
                        .addPlaceholderReplace("%NAME%", toSell.getMessageName())
                        .sendMessage(toReward);
            }
            rewardPlayer(toReward, toSell, sold);
            return true;
        } return false;
    }

    private int sellMaterial(Sellable toSell, Inventory inventory) {

        int sellCount = 0;
        for (int i = 0; i < inventory.getContents().length; i++) {
            ItemStack stack = inventory.getItem(i);
            if (toSell.isMatchingStack(stack)) {
                sellCount += stack.getAmount();
                inventory.setItem(i, null);
            }
        }
        return sellCount;
    }

    private void rewardPlayer(Player toReward, Sellable sold, int amount) {

        if (amount == 0) {
            return;
        }

        double multiplier = getMultiplier(toReward);
        double revenue = sold.getRevenue(toReward, amount) * multiplier;

        try {
            hookHandler.addMoney(toReward, revenue);
        } catch (HookException e) {
            // This should not happen under normal circumstances, print stacktrace just in case
            Exception toPrint = e;
            if (e.hasParentException()) {
                toPrint = e.getParentException();
            }

            hmc.getLogger().log(Level.WARNING, "Could not add money to player " + toReward.getName(), toPrint);
            MessageBuilder.create("errorOccurred", "Sell").sendMessage(toReward);
            return;
        }

        storage.getPlayer(toReward).incrementDouble(DataType.REVENUE, revenue);
        revenue = Utils.roundDouble(revenue);

        MessageBuilder.create("modSellSuccess", hmc, "Sell")
                .addPlaceholderReplace("%MATERIAL%", sold.getMessageName())
                .addPlaceholderReplace("%MONEY%", String.valueOf(revenue))
                .addPlaceholderReplace("%AMOUNT%", String.valueOf(amount))
                .sendMessage(toReward);

        MessageBuilder.create("modSellSuccessLog", hmc)
                .addPlaceholderReplace("%PLAYER%", toReward.getName())
                .addPlaceholderReplace("%MATERIAL%", sold.getMessageName())
                .addPlaceholderReplace("%MONEY%", String.valueOf(revenue))
                .addPlaceholderReplace("%AMOUNT%", String.valueOf(amount))
                .logMessage(Level.INFO);
    }

    private double getMultiplier(Player player) {
        return levelRewardMultipliers.get(Math.min(storage.getPlayer(player).getLevel(), levelRewardMultipliers.size() - 1));
    }

    private void closeActiveInventories() {
        for (Map.Entry<Inventory, Player> inventoryPlayerEntry : activeMenus.entrySet()) {
            inventoryPlayerEntry.getValue().closeInventory();
        }
        activeMenus.clear();
    }

    @Override
    public void onDisable() {
        closeActiveInventories();
        sellableMap.storeCurrentCycle();
    }

    @Override
    public void loadConfig() {

        if (activeMenus == null) {
            activeMenus = new HashMap<>();
        }
        closeActiveInventories();

        FileConfiguration config = hmc.getConfig();

        StringArgumentSeparator multipliers =
                new StringArgumentSeparator(config.getString("sell.multipliersPerLevel"), ',');

        levelRewardMultipliers = new ArrayList<>();
        levelRewardMultipliers.add(1.0d);
        for (int i = 0; multipliers.meetsLength(i + 1); i++) {
            levelRewardMultipliers.add(multipliers.getArgumentDoubleMinimum(i, 1.0d));
        }

        menuCommands = new HashMap<>();
        ConfigurationSection commandSection = config.getConfigurationSection("sell.menuCommands");
        for (String slot : commandSection.getKeys(false)) {
            try {
                int slotInt = Integer.parseInt(slot);
                if (slotInt >= 0 && slotInt < 9) {
                    menuCommands.put(slotInt, "/" + commandSection.getString(slot));
                } else {
                    MessageBuilder.create("modSellMenuInvalidCommandFormat", hmc)
                            .addPlaceholderReplace("%KEY%", slot)
                            .logMessage(Level.WARNING);
                }
            } catch (NumberFormatException e) {
                MessageBuilder.create("modSellMenuInvalidCommandFormat", hmc)
                        .addPlaceholderReplace("%KEY%", slot)
                        .logMessage(Level.WARNING);
            }
        }

        int cycleTimeSeconds = config.getInt("sell.cycleTimeMinutes", 180) * 60;
        double priceAdjustMultiplier = config.getDouble("sell.priceAdjustMultiplier", 1.5d);
        double priceVarianceFactor = config.getDouble("sell.priceVarianceFactor", 0.1d);
        int unitsUntilIncrease = config.getInt("sell.unitsUntilIncrease");

        if (sellableMap == null) {
            sellableMap = new SellableMap();
        }

        sellableMap.configReloaded(config.getConfigurationSection("sell.sellables"),
                cycleTimeSeconds, priceAdjustMultiplier, priceVarianceFactor, unitsUntilIncrease);
    }

    @Override
    public void sweep() {
        autoSellingCache.cleanUp();
    }
}
