package de.halfminer.hmh;

import de.halfminer.hmh.data.HaroConfig;
import de.halfminer.hmh.data.HaroPlayer;
import de.halfminer.hmh.data.HaroStorage;
import de.halfminer.hms.manageable.Disableable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

/**
 * Manages player health, both updating the max health and healing a player,
 * while storing the data in storage after calling {@link #storeCurrentHealth(Player)}.
 *
 * Ensures that if the plugin gets disabled all custom health of players is stored and then recovered.
 */
public class HealthManager extends HaroClass implements Disableable {

    private final HaroStorage haroStorage = hmh.getHaroStorage();


    double getMinecraftMaxHealth(Player player) {
        return player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
    }

    void updateHealth(Player player) {

        HaroPlayer haroPlayer = haroStorage.getHaroPlayer(player);
        double health = haroPlayer.getCurrentHealth();
        double maxHealth = haroPlayer.getMaxHealth();

        updateMaxHealth(player, maxHealth);
        player.setHealth(Math.min(health, maxHealth));
    }

    boolean increaseHealth(Player player) {
        HaroConfig haroConfig = haroStorage.getHaroConfig();
        double healthMax = haroConfig.getHealthMax();
        double healthGain = haroConfig.getHealthGain();

        double newHealthMax = Math.min(getMinecraftMaxHealth(player) + healthGain, healthMax);
        return updateMaxHealth(player, newHealthMax);
    }

    boolean reduceHealth(Player player) {
        HaroConfig haroConfig = haroStorage.getHaroConfig();
        double healthMin = haroConfig.getHealthMin();
        double healthLoss = haroConfig.getHealthLoss();

        double newHealthMax = Math.max(getMinecraftMaxHealth(player) - healthLoss, healthMin);
        return updateMaxHealth(player, newHealthMax);
    }

    void storeCurrentHealth(Player player) {

        HaroPlayer haroPlayer = haroStorage.getHaroPlayer(player);
        haroPlayer.setHealth(player.getHealth(), player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
    }

    public void resetPlayerHealth(Player player) {

        double defaultHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue();
        updateMaxHealth(player, defaultHealth);
    }

    private boolean updateMaxHealth(Player player, double newValue) {
        AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);

        if (maxHealthAttribute.getValue() != newValue) {
            maxHealthAttribute.setBaseValue(newValue);
            hmh.getLogger().info("Health set to " + newValue + " for player " + player.getName());
            return true;
        }

        return false;
    }

    @Override
    public void onDisable() {
        if (haroStorage.isGameRunning()) {
            server.getOnlinePlayers()
                    .stream()
                    .filter(p -> !p.hasPermission("hmh.admin"))
                    .forEach(p -> {
                        storeCurrentHealth(p);
                        resetPlayerHealth(p);
                    });
        }
    }
}
