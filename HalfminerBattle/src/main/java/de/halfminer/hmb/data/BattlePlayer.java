package de.halfminer.hmb.data;

import de.halfminer.hmb.HalfminerBattle;
import de.halfminer.hmb.arena.abs.Arena;
import de.halfminer.hmb.enums.BattleState;
import de.halfminer.hmb.enums.GameModeType;
import de.halfminer.hmb.mode.GlobalMode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.potion.PotionEffect;

import java.util.List;
import java.util.UUID;

/**
 * Encapsulating player specific battle data, such as his state, inventory, survival data (inventory, health..) and
 * the players arena and game partners
 */
class BattlePlayer {

    private final UUID baseUUID;

    private BattleState state = BattleState.IDLE;
    private GameModeType gameMode;
    private long lastStateChange = System.currentTimeMillis();
    private PlayerData data = null;

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
            GlobalMode global = (GlobalMode) HalfminerBattle.getInstance().getGameMode(GameModeType.GLOBAL);
            if (lastStateChange + (global.getQueueCooldownSeconds() * 1000) < System.currentTimeMillis()) {
                setState(BattleState.IDLE);
            }
        }
        return state;
    }

    void setState(BattleState state) {
        this.state = state;
        this.lastStateChange = System.currentTimeMillis();

        if (state.equals(BattleState.IDLE) || state.equals(BattleState.QUEUE_COOLDOWN)) {

            gameMode = null;
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

    void setGameMode(GameModeType type) {
        this.gameMode = type;
    }

    void storeData() {
        data = new PlayerData(getBase());
    }

    void restorePlayer() {
        if (data != null) data.restorePlayer();
    }

    void setBattlePartners(List<BattlePlayer> players) {
        gamePartners = players;
    }

    List<BattlePlayer> getGamePartners() {
        return gamePartners;
    }

    void setArena(Arena arena) {
        this.arena = arena;
    }

    Arena getArena() {
        return arena;
    }

    GameModeType getGameMode() {
        return gameMode;
    }

    private class PlayerData {

        private final HalfminerBattle hmb = HalfminerBattle.getInstance();

        private final Player player;

        private final Location loc;
        private final ItemStack[] inventory;

        private final double health;
        private final int foodLevel;
        private final float foodSaturation;
        private final float foodExhaustion;

        PlayerData(Player player) {

            this.player = player;

            player.leaveVehicle();
            loc = player.getLocation();

            player.closeInventory();
            inventory = player.getInventory().getContents();

            health = player.getHealth();
            foodLevel = player.getFoodLevel();
            foodSaturation = player.getSaturation();
            foodExhaustion = player.getExhaustion();
        }

        void restorePlayer() {

            if (player.isDead() && player.isOnline()) {
                try {
                    Bukkit.getScheduler().runTaskLater(hmb, () -> {
                        player.spigot().respawn();
                        restore();
                    }, 2L);
                } catch (IllegalPluginAccessException e) {
                    // exception is thrown when trying to respawn dead player while shutting down
                    restore();
                }
            } else {
                restore();
            }
        }

        private void restore() {

            player.setHealth(health);
            player.setFoodLevel(foodLevel);
            player.setSaturation(foodSaturation);
            player.setExhaustion(foodExhaustion);
            player.setFireTicks(0);
            for (PotionEffect effect : player.getActivePotionEffects())
                player.removePotionEffect(effect.getType());

            player.closeInventory();
            player.getInventory().setContents(inventory);
            player.updateInventory();
            player.teleport(loc);
            player.setWalkSpeed(0.2F);
        }
    }
}
