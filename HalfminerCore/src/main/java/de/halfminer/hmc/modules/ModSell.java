package de.halfminer.hmc.modules;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.halfminer.hms.enums.DataType;
import de.halfminer.hmc.enums.Sellable;
import de.halfminer.hms.exception.HookException;
import de.halfminer.hms.interfaces.Sweepable;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * - Auto sells chests on inventory close
 *   - Needs to be toggled
 * - Sell items that are sellable
 *   - Custom multiplier per permission
 *   - Price settable via config
 * - Can be accessed via command
 */
@SuppressWarnings("unused")
public class ModSell extends HalfminerModule implements Listener, Sweepable {

    private final Cache<Player, Boolean> autoSellingCache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .weakKeys()
            .concurrencyLevel(1)
            .build();

    private Map<String, Integer> prices;

    @EventHandler(priority = EventPriority.MONITOR,ignoreCancelled = true)
    public void onChestOpen(InventoryCloseEvent e) {

        if (e.getPlayer() instanceof Player
                && autoSellingCache.getIfPresent(e.getPlayer()) != null
                && (e.getInventory().getHolder() instanceof Chest
                || e.getInventory().getHolder() instanceof DoubleChest)) {

            Inventory chest = e.getInventory();
            ItemStack item = chest.getItem(0);
            if (item != null) {
                Sellable sell = Sellable.getFromMaterial(item.getType());
                if (sell != null) {

                    Player seller = (Player) e.getPlayer();
                    int amountSold = sellMaterial(seller, e.getInventory(), sell);
                    rewardPlayer(seller, sell, amountSold);
                }
            }
        }
    }

    public int sellMaterial(Player player, Inventory inventory, Sellable toSell) {

        int sellCount = 0;
        for (int i = 0; i < inventory.getContents().length; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack != null
                    && stack.getType().equals(toSell.getMaterial())
                    && stack.getDurability() == toSell.getItemId()) {

                sellCount += stack.getAmount();
                inventory.setItem(i, null);
            }
        }
        player.updateInventory();
        return sellCount;
    }

    public void rewardPlayer(Player toReward, Sellable sold, int amount) {

        if (amount == 0) return;

        // get rank multiplier
        double multiplier = 1.0d;
        if (toReward.hasPermission("hmc.level.5")) multiplier = 2.5d;
        else if (toReward.hasPermission("hmc.level.4")) multiplier = 2.0d;
        else if (toReward.hasPermission("hmc.level.3")) multiplier = 1.75d;
        else if (toReward.hasPermission("hmc.level.2")) multiplier = 1.5d;
        else if (toReward.hasPermission("hmc.level.1")) multiplier = 1.25d;

        int baseValue = 1000;
        if (prices.containsKey(sold.getClearText()))
            baseValue = prices.get(sold.getClearText());

        // calculate revenue
        double revenue = (amount / (double) baseValue) * multiplier;

        try {
            hookHandler.addMoney(toReward, revenue);
        } catch (HookException e) {
            // This should not happen under normal circumstances, print stacktrace just in case
            Exception toPrint = e;
            if (e.hasParentException()) toPrint = e.getParentException();
            hmc.getLogger().log(Level.WARNING, "Could not add money to player " + toReward.getName(), toPrint);
            MessageBuilder.create("errorOccurred", "Sell").sendMessage(toReward);
            return;
        }

        storage.getPlayer(toReward).incrementDouble(DataType.REVENUE, revenue);
        revenue = Utils.roundDouble(revenue);

        // print message
        String materialFriendly = Utils.makeStringFriendly(sold.name());
        MessageBuilder.create("modSellSuccess", hmc, "Sell")
                .addPlaceholderReplace("%MATERIAL%", materialFriendly)
                .addPlaceholderReplace("%MONEY%", String.valueOf(revenue))
                .addPlaceholderReplace("%AMOUNT%", String.valueOf(amount))
                .sendMessage(toReward);

        MessageBuilder.create("modSellSuccessLog", hmc)
                .addPlaceholderReplace("%PLAYER%", toReward.getName())
                .addPlaceholderReplace("%MATERIAL%", materialFriendly)
                .addPlaceholderReplace("%MONEY%", String.valueOf(revenue))
                .addPlaceholderReplace("%AMOUNT%", String.valueOf(amount))
                .logMessage(Level.INFO);
    }

    public boolean toggleAutoSell(Player player) {

        boolean contains = autoSellingCache.getIfPresent(player) != null;

        if (contains) autoSellingCache.invalidate(player);
        else autoSellingCache.put(player, true);

        return !contains;
    }

    @Override
    public void loadConfig() {

        prices = new HashMap<>();

        ConfigurationSection section = hmc.getConfig().getConfigurationSection("sell");
        for (String key : section.getKeys(false)) {
            prices.put(key, section.getInt(key));
        }
    }

    @Override
    public void sweep() {
        autoSellingCache.cleanUp();
    }
}
