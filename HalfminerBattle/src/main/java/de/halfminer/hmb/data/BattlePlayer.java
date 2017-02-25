package de.halfminer.hmb.data;

import de.halfminer.hmb.HalfminerBattle;
import de.halfminer.hmb.arena.abs.Arena;
import de.halfminer.hmb.enums.BattleModeType;
import de.halfminer.hmb.enums.BattleState;
import de.halfminer.hmb.mode.GlobalMode;
import de.halfminer.hms.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.potion.PotionEffect;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Encapsulating player specific battle data, such as his state, inventory, survival data (inventory, health..) and
 * the players arena and game partners
 */
class BattlePlayer {

    private static final HalfminerBattle hmb = HalfminerBattle.getInstance();
    private static final Object inventoryWriteLock = new Object();

    private final UUID baseUUID;

    private BattleState state = BattleState.IDLE;
    private BattleModeType battleModeType;
    private long lastStateChange = System.currentTimeMillis();
    private PlayerData data = null;
    private boolean hasDisconnected = false;

    private Arena arena = null;
    private List<BattlePlayer> gamePartners = null;

    BattlePlayer(Player p) {
        this.baseUUID = p.getUniqueId();
    }

    Player getBase() {
        return Bukkit.getPlayer(baseUUID);
    }

    BattleState getState() {
        if (state.equals(BattleState.QUEUE_COOLDOWN)) {
            GlobalMode global = (GlobalMode) HalfminerBattle.getInstance().getBattleMode(BattleModeType.GLOBAL);
            if (lastStateChange + (global.getQueueCooldownSeconds() * 1000) < System.currentTimeMillis()) {
                setState(BattleState.IDLE);
            }
        }
        return state;
    }

    void setState(BattleState state) {
        setState(state, null);
    }

    void setState(BattleState state, BattleModeType battleMode) {
        this.state = state;
        this.battleModeType = battleMode;
        this.lastStateChange = System.currentTimeMillis();

        if (state.equals(BattleState.IDLE) || state.equals(BattleState.QUEUE_COOLDOWN)) {

            // remove partners if idling or in queue cooldown
            if (gamePartners != null) {

                for (BattlePlayer p : gamePartners) {
                    List<BattlePlayer> partnersOfPartner = p.getGamePartners();
                    if (partnersOfPartner != null) {
                        partnersOfPartner.remove(this);
                    }
                }
                gamePartners = null;
            }
        }
    }

    void storeData() {
        data = new PlayerData();
    }

    void restorePlayer(boolean restoreInventory) {

        Player player = getBase();
        if (data == null)
            throw new RuntimeException("Could not restore player " + player.getName() + " as data was not set");

        // if dead (and still online) respawn with delay to prevent damage immunity loss glitch
        if (player.isDead() && !hasDisconnected) {
            try {
                Bukkit.getScheduler().runTaskLater(hmb, () -> {
                    // don't restore if already ocurred due to logout in between death and task execution
                    if (data != null) {
                        player.spigot().respawn();
                        restore(player, restoreInventory);
                    }
                }, 2L);
            } catch (IllegalPluginAccessException e) {
                // exception is thrown when trying to respawn dead player while shutting down
                restore(player, restoreInventory);
            }
        } else {
            restore(player, restoreInventory);
        }
    }

    private void restore(Player player, boolean restoreInventory) {

        try {
            player.setHealth(data.health);
            player.setFoodLevel(data.foodLevel);
            player.setSaturation(data.foodSaturation);
            player.setExhaustion(data.foodExhaustion);
            player.setFireTicks(0);
            for (PotionEffect effect : player.getActivePotionEffects())
                player.removePotionEffect(effect.getType());
            player.addPotionEffects(data.potionEffects);
        } catch (Exception e) {
            hmb.getLogger().warning("Player " + player.getName()
                    + " could not be healed properly, see stacktrace for information");
            e.printStackTrace();
        }

        player.setWalkSpeed(data.walkSpeed);
        player.setGameMode(data.gameMode);

        if (restoreInventory) restoreInventory(player);

        if (!player.teleport(data.loc)) {
            hmb.getLogger().warning("Player " + player.getName()
                    + " could not be teleported to his original location at " + Utils.getStringFromLocation(data.loc));
        }
        data = null;
        arena = null;
        hasDisconnected = false;
    }

    void restoreInventory(Player player) {
        if (data != null) {

            // before restoring, check if non arena items were dropped during battle and add them after restoring
            List<ItemStack> itemStacks = new ArrayList<>();
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null) {

                    // don't ever keep glass bottles
                    if (item.getType().equals(Material.GLASS_BOTTLE)) {
                        continue;
                    }

                    boolean keep = true;
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null && meta.hasLore()) {
                        for (String str : meta.getLore()) {
                            if (str.contains(arena.getName())) {
                                keep = false;
                                break;
                            }
                        }
                    }

                    if (keep) {
                        itemStacks.add(item);
                    }
                }
            }

            player.closeInventory();
            player.getInventory().setContents(data.inventory);
            player.getInventory().addItem(itemStacks.toArray(new ItemStack[itemStacks.size()]));
            player.updateInventory();
        }
    }

    void setHasDisconnected() {
        hasDisconnected = true;
    }

    void setBattlePartners(List<BattlePlayer> players) {
        gamePartners = players;
    }

    List<BattlePlayer> getGamePartners() {
        return gamePartners;
    }

    void setArena(Arena arena) {
        this.arena = arena;
        this.battleModeType = arena.getBattleModeType();
    }

    Arena getArena() {
        return arena;
    }

    BattleModeType getBattleModeType() {
        return battleModeType;
    }

    private class PlayerData {

        private final Location loc;
        private final ItemStack[] inventory;

        private final double health;
        private final int foodLevel;
        private final float foodSaturation;
        private final float foodExhaustion;
        private final Collection<PotionEffect> potionEffects;

        private final GameMode gameMode;
        private final float walkSpeed;

        PlayerData() {

            Player player = getBase();

            player.leaveVehicle();
            loc = player.getLocation();

            player.closeInventory();
            inventory = player.getInventory().getContents();

            if (((GlobalMode) hmb.getBattleMode(BattleModeType.GLOBAL)).isSaveInventoryToDisk()) {

                hmb.getServer().getScheduler().runTaskAsynchronously(hmb, () -> {

                    synchronized (inventoryWriteLock) {
                        String fileName = String.valueOf(System.currentTimeMillis() / 1000) + "-" + player.getName() + ".yml";

                        File path = new File(hmb.getDataFolder(), "inventories");
                        boolean pathExists = path.exists();
                        if (!pathExists) {
                            pathExists = path.mkdir();
                        }

                        if (pathExists && path.isDirectory()) {
                            File file = new File(path, fileName);
                            try {
                                if (file.createNewFile()) {
                                    YamlConfiguration configFile = new YamlConfiguration();
                                    configFile.set("inventory", inventory);
                                    configFile.set("uuid", player.getUniqueId().toString());
                                    configFile.save(file);
                                }
                            } catch (IOException e) {
                                hmb.getLogger().warning("Could not write inventory to disk with filename " + fileName);
                                e.printStackTrace();
                            }
                        } else hmb.getLogger().warning("Could not create sub folder in plugin directory");
                    }
                });
            }

            health = Math.min(20.0d, player.getHealth());
            foodLevel = player.getFoodLevel();
            foodSaturation = player.getSaturation();
            foodExhaustion = player.getExhaustion();
            potionEffects = player.getActivePotionEffects();

            gameMode = player.getGameMode();
            walkSpeed = player.getWalkSpeed();
        }
    }
}
