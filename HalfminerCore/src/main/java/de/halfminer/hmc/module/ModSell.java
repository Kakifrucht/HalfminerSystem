package de.halfminer.hmc.module;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.halfminer.hmc.module.sell.*;
import de.halfminer.hms.cache.CustomitemCache;
import de.halfminer.hms.cache.CustomtextCache;
import de.halfminer.hms.cache.exceptions.CachingException;
import de.halfminer.hms.cache.exceptions.ItemCacheException;
import de.halfminer.hms.handler.hooks.HookException;
import de.halfminer.hms.handler.menu.MenuClickHandler;
import de.halfminer.hms.handler.menu.MenuContainer;
import de.halfminer.hms.handler.menu.MenuCreator;
import de.halfminer.hms.handler.storage.DataType;
import de.halfminer.hms.handler.storage.PlayerNotFoundException;
import de.halfminer.hms.manageable.Disableable;
import de.halfminer.hms.manageable.Sweepable;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.StringArgumentSeparator;
import de.halfminer.hms.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * - Full dynamic sell system
 *   - Cycle based selling, every given minutes new items will be chosen and all prices will reset
 *     - Cycles are persistent through restarts
 *     - Broadcast message one minute before and when a new cycle starts
 *       - Broadcasts player that sold most of a single item, and which it was
 *     - Cycle time is dependent on current player count on server, more players - more cycles
 *       - Define a max/min time and playercount for min time in config
 *   - Reads items and their groups from config
 *     - Items are defined by their Material, durability/id, base price per unit and name of item
 *     - Items must be put into groups, which define how many items of said group will be put into a cycle
 *       - Price will be adjusted by a configurable amount every given amount (also configurable per group)
 *         - Shows original base price
 *         - Variance can be added to base price for more dynamic pricing
 *   - Includes GUI, must be accessed via /sell command
 *     - First line in GUI can be fully configured via customitems.txt and config to set a custom command per slot
 *       - By default line will be filled with stained glass pane
 *       - For example a custom button to toggle auto selling can be added
 * - Custom revenue multiplier per player level (hms.level)
 *   - Randomly (depending on revenue) sends message about how much more revenue could have been made with higher rank
 * - Auto sells chests on inventory close
 *   - Needs to be toggled
 * - Items with any item meta won't be sold
 */
@SuppressWarnings("unused")
public class ModSell extends HalfminerModule implements Disableable, Listener, Sweepable, MenuCreator {

    private Map<Integer, String> menuCommands;
    private List<Double> levelRewardMultipliers;
    private final SellableMap sellableMap = new DefaultSellableMap();

    private BukkitTask menuRefreshTask;

    private final Cache<UUID, Double> potentialRevenueLostCache = CacheBuilder.newBuilder()
            .expireAfterWrite(20, TimeUnit.MINUTES)
            .concurrencyLevel(1)
            .build();

    private final Cache<Player, Boolean> autoSellingCache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .weakKeys()
            .concurrencyLevel(1)
            .build();


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMenuClose(InventoryCloseEvent e) {

        // auto selling of chests
        if (e.getPlayer() instanceof Player
                && autoSellingCache.getIfPresent(e.getPlayer()) != null
                && (e.getInventory().getHolder() instanceof Chest
                || e.getInventory().getHolder() instanceof DoubleChest
                || e.getInventory().getHolder() instanceof ShulkerBox)) {

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

        refreshActiveMenus();

        for (Player p : server.getOnlinePlayers()) {
            if (!p.hasPermission("hmc.bypass.sellcyclesound")) {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_FLUTE, 1.0f, 0.8f);
            }
        }

        MessageBuilder.create("modSellNewCycleBroadcast", hmc, "Sell")
                .addPlaceholderReplace("%TIME%", e.getTimeUntilNextCycle() / 60)
                .broadcastMessage(true);

        // broadcast player who sold most in last cycle (with delay)
        Sellable mostSoldLastCycle = e.getSellableMostSoldLastCycle();
        if (mostSoldLastCycle != null) {

            // only broadcast if player sold at least 1k units of the item
            int amountSold = e.getAmountSoldMostLastCycle();
            if (amountSold >= 1000) {

                UUID uuidMostSoldLastCycle = e.getUuidSoldMostLastCycle();
                scheduler.runTaskLater(hmc, () -> {
                    try {
                        MessageBuilder.create("modSellMostSoldBroadcast", hmc, "Sell")
                                .addPlaceholderReplace("%PLAYER%", storage.getPlayer(uuidMostSoldLastCycle).getName())
                                .addPlaceholderReplace("%ITEMNAME%", mostSoldLastCycle.getMessageName())
                                .addPlaceholderReplace("%ITEMAMOUNT%", amountSold)
                                .broadcastMessage(true);
                    } catch (PlayerNotFoundException ignored) {}
                }, 500L);
            }
        }
    }

    public void showSellMenu(Player player) {

        if (!sellableMap.hasCycle()) {
            MessageBuilder.create("modSellDisabled", hmc, "Sell").sendMessage(player);
            return;
        }

        SellCycle currentCycle = sellableMap.getCurrentCycle();
        ItemStack[] menuItems = new ItemStack[45];

        // top line (menu controls), first prefill first line with stained glass
        ItemStack spacer = new ItemStack(Material.STAINED_GLASS_PANE);
        Utils.setDisplayName(spacer, ChatColor.RESET.toString());
        for (int slot = 0; slot < 9; slot++) {
            menuItems[slot] = spacer;
        }

        // read menu items in first line from customitem cache
        try {
            CustomtextCache cache = coreStorage.getCache("customitems.txt");
            CustomitemCache itemCache = new CustomitemCache(cache);
            long timeLeftCycle = Math.max(currentCycle.getSecondsTillExpiry() / 60, 0);
            Map<String, String> placeholders = Collections.singletonMap("%CYCLEMINUTES%", String.valueOf(timeLeftCycle));

            for (int i = 0; i < 9; i++) {
                String key = "sellmenu-" + (i + 1);
                try {
                    menuItems[i] = itemCache.getItem(key, player, 1, placeholders);
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
        List<Sellable> sellables = currentCycle.getSellables();
        String multiplier = String.valueOf(getMultiplier(player));
        for (int i = 0; i < sellables.size(); i++) {

            Sellable sellable = sellables.get(i);
            ItemStack currentItem = sellable.getItemStack();

            long currentUnitAmount = sellable.getCurrentUnitAmount(player);
            int baseUnitAmount = sellable.getBaseUnitAmount();

            String unitAmount;
            if (currentUnitAmount != baseUnitAmount) {
                unitAmount = MessageBuilder.create("modSellMenuStackAmountFormat", hmc)
                        .addPlaceholderReplace("%CURRENT%", currentUnitAmount)
                        .addPlaceholderReplace("%BASE%", baseUnitAmount)
                        .returnMessage();
            } else {
                unitAmount = String.valueOf(currentUnitAmount);
            }

            MessageBuilder stackNameAndLore = MessageBuilder.create("modSellMenuStack", hmc)
                    .addPlaceholderReplace("%NAME%", sellable.getMessageName())
                    .addPlaceholderReplace("%MULTIPLIER%", multiplier)
                    .addPlaceholderReplace("%AMOUNT%", unitAmount)
                    .addPlaceholderReplace("%NEXTINCREASE%", sellable.getAmountUntilNextIncrease(player))
                    .addPlaceholderReplace("%SOLDTOTAL%", sellable.getAmountSoldTotal());

            Utils.applyLocaleToItemStack(currentItem, stackNameAndLore);
            menuItems[i + 18] = currentItem;
        }

        String menuTitle = MessageBuilder.returnMessage("modSellMenuTitle", hmc);
        MenuClickHandler menuClickHandler = (e, rawSlot) -> {

            if (e.getRawSlot() != e.getSlot()) {
                return;
            }

            boolean closeInventory = false;
            if (menuCommands.containsKey(rawSlot)) {

                // execute/close on next tick, to allow other menus to be opened via command
                scheduler.runTask(hmc, () -> {
                    player.closeInventory();
                    player.chat(menuCommands.get(rawSlot));
                });

            } else if (rawSlot >= 18 && sellMaterialAndReward(rawSlot - 18, player)) {
                menuHandler.closeMenu(player);
            }
        };

        MenuContainer menuContainer = new MenuContainer(this, player, menuTitle, menuItems, menuClickHandler);
        menuHandler.openMenu(menuContainer);
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
        sellableMap.createNewCycle();
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
        double highestMultiplier = levelRewardMultipliers.get(levelRewardMultipliers.size() - 1);

        long unitAmount = sold.getCurrentUnitAmount(toReward);
        double baseRevenue = sold.getRevenue(toReward, amount);

        // notify of unit amount change
        long unitAmountAfterSell = sold.getCurrentUnitAmount(toReward);
        if (unitAmount < unitAmountAfterSell) {
            toReward.playSound(toReward.getLocation(), Sound.BLOCK_NOTE_HARP, 1.0f, 1.2f);

            MessageBuilder.create("modSellAmountIncreased", hmc, "Sell")
                    .addPlaceholderReplace("%NAME%", sold.getMessageName())
                    .addPlaceholderReplace("%NEWAMOUNT%", unitAmountAfterSell)
                    .sendMessage(toReward);
        }

        double revenue = baseRevenue * multiplier;
        double potentialRevenueLost = (highestMultiplier * baseRevenue) - revenue;

        try {
            hookHandler.addMoney(toReward, revenue);
        } catch (HookException e) {
            // This should not happen under normal circumstances, print stacktrace just in case
            Exception toPrint = e;
            if (e.hasParentException()) {
                toPrint = e.getParentException();
            }

            hmc.getLogger().log(Level.WARNING, "Could not add money to player " + toReward.getName() + ", amount " + revenue, toPrint);
            MessageBuilder.create("errorOccurred", "Sell").sendMessage(toReward);
            return;
        }

        storage.getPlayer(toReward).incrementDouble(DataType.REVENUE, revenue);
        revenue = Utils.roundDouble(revenue);

        MessageBuilder.create("modSellSuccess", hmc, "Sell")
                .addPlaceholderReplace("%MATERIAL%", sold.getMessageName())
                .addPlaceholderReplace("%MONEY%", revenue)
                .addPlaceholderReplace("%AMOUNT%", amount)
                .sendMessage(toReward);

        MessageBuilder.create("modSellSuccessLog", hmc)
                .addPlaceholderReplace("%PLAYER%", toReward.getName())
                .addPlaceholderReplace("%MATERIAL%", sold.getMessageName())
                .addPlaceholderReplace("%MONEY%", revenue)
                .addPlaceholderReplace("%AMOUNT%", amount)
                .logMessage(Level.INFO);

        // messaging about possible sell revenue
        if (potentialRevenueLost > 0.0d) {

            UUID uuid = toReward.getUniqueId();
            Double revenueLostBoxed = potentialRevenueLostCache.getIfPresent(uuid);

            double revenueLostTotal = potentialRevenueLost;
            if (revenueLostBoxed != null) {

                if (revenueLostBoxed == Double.MIN_VALUE) {
                    return;
                }

                revenueLostTotal += revenueLostBoxed;
            }

            if (revenueLostTotal > 20.0d && Utils.random(Math.min((int) revenueLostTotal * 5, 500))) {
                double revenueLostRounded = Utils.roundDouble(revenueLostTotal);
                scheduler.runTaskLater(hmc, () -> {
                    if (toReward.isOnline()) {

                        MessageBuilder.create("modSellSuccessPossibleAmountLog", hmc)
                                .addPlaceholderReplace("%PLAYER%", toReward.getName())
                                .addPlaceholderReplace("%REVENUELOST%", revenueLostRounded)
                                .logMessage(Level.INFO);

                        MessageBuilder.create("modSellSuccessPossibleAmount", hmc, "Sell")
                                .addPlaceholderReplace("%REVENUELOST%", revenueLostRounded)
                                .sendMessage(toReward);
                        toReward.playSound(toReward.getLocation(), Sound.BLOCK_NOTE_PLING, 0.7f, 1.4f);
                    }
                }, 300L);
                potentialRevenueLostCache.put(uuid, Double.MIN_VALUE);
            } else {
                potentialRevenueLostCache.put(uuid, revenueLostTotal);
            }
        }
    }

    private double getMultiplier(Player player) {
        return levelRewardMultipliers.get(Math.min(storage.getPlayer(player).getLevel(), levelRewardMultipliers.size() - 1));
    }

    private void refreshActiveMenus() {
        menuHandler.getViewingPlayers(this).forEach(this::showSellMenu);
    }

    @Override
    public void onDisable() {
        sellableMap.storeCurrentCycle();
    }

    @Override
    public void loadConfig() {

        menuHandler.closeAllMenus(this);

        FileConfiguration config = hmc.getConfig();

        // read sell multipliers
        StringArgumentSeparator multipliers =
                new StringArgumentSeparator(config.getString("sell.multipliersPerLevel"), ',');

        levelRewardMultipliers = new ArrayList<>();
        levelRewardMultipliers.add(1.0d);
        for (int i = 0; multipliers.meetsLength(i + 1); i++) {
            levelRewardMultipliers.add(multipliers.getArgumentDoubleMinimum(i, 1.0d));
        }

        // read sell menu commands
        menuCommands = new HashMap<>();
        ConfigurationSection commandSection = config.getConfigurationSection("sell.menuCommands");
        for (String slot : commandSection.getKeys(false)) {
            try {
                int slotInt = Integer.parseInt(slot);
                if (slotInt >= 0 && slotInt < 9) {
                    String command = commandSection.getString(slot, "");
                    if (command.length() > 0) {
                        menuCommands.put(slotInt, "/" + command);
                    }
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

        if (menuRefreshTask == null) {
            menuRefreshTask = scheduler.runTaskTimer(hmc, this::refreshActiveMenus, 1200L, 1200L);
        }
    }

    @Override
    public void sweep() {
        potentialRevenueLostCache.cleanUp();
        autoSellingCache.cleanUp();
    }
}
