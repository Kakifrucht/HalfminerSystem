package de.halfminer.hml.land;

import de.halfminer.hml.LandClass;
import de.halfminer.hml.data.LandPlayer;
import de.halfminer.hms.handler.HanHooks;
import de.halfminer.hms.handler.hooks.HookException;
import de.halfminer.hms.manageable.Disableable;
import de.halfminer.hms.manageable.Reloadable;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Pair;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;

public class FlyBoard extends LandClass implements Listener, Disableable, Reloadable {

    private final Map<Player, Pair<Long, BukkitTask>> flyMap;

    private double flyCost;
    private int flyDurationSeconds;


    FlyBoard() {
        this.flyMap = new HashMap<>();
    }

    public boolean isPlayerFlying(Player player) {
        return flyMap.containsKey(player);
    }

    public boolean togglePlayerFlying(Player player) {

        LandPlayer landPlayer = hml.getLandStorage().getLandPlayer(player);
        if (flyMap.containsKey(player)) {

            setFlyEnabled(player, false);
            landPlayer.setFlyTimeLeft(getFlyTimeLeft(player));
            flyMap.remove(player).getRight().cancel();

        } else {

            int timeLeftFlying = getFlyTimeLeft(player);
            if (timeLeftFlying < 1 && !takePayment(player)) {
                return false;
            }

            long interval = flyDurationSeconds * 20L;
            if (timeLeftFlying < 1) {
                timeLeftFlying = flyDurationSeconds;
            }

            BukkitTask task = scheduler.runTaskTimer(hml, () -> {

                if (hms.getHooksHandler().isAfk(player)) {
                    togglePlayerFlying(player);
                    MessageBuilder.create("flyBoardDisableAfk", hml).sendMessage(player);
                    return;
                }

                if (takePayment(player)) {
                    setFlyTimeLeft(player, flyDurationSeconds, null);
                } else {
                    togglePlayerFlying(player);
                    MessageBuilder.create("flyBoardDisableNotEnoughMoney", hml).sendMessage(player);
                }

            }, timeLeftFlying * 20L, interval);

            setFlyEnabled(player, true);
            setFlyTimeLeft(player, timeLeftFlying, task);
        }

        return true;
    }

    public void updatePlayerAllowFlight(Player player, boolean isOwner) {
        if (isPlayerFlying(player)) {
            if (!player.getAllowFlight() && isOwner) {
                setFlyEnabled(player, true);
            } else if (player.getAllowFlight() && !isOwner) {
                setFlyEnabled(player, false);
            }
        }
    }

    public int getFlyTimeLeft(Player player) {
        if (isPlayerFlying(player)) {
            return (int) (flyMap.get(player).getLeft() - (System.currentTimeMillis() / 1000L));
        } else {
            return hml.getLandStorage().getLandPlayer(player).getFlyTimeLeft();
        }
    }

    public double getCost() {
        return flyCost;
    }

    public int getFlyDurationSeconds() {
        return flyDurationSeconds;
    }

    private boolean takePayment(Player player) {

        HanHooks hooksHandler = hms.getHooksHandler();
        if (hooksHandler.getMoney(player) < flyCost) {
            return false;
        }

        try {
            hms.getHooksHandler().addMoney(player, -flyCost);
            MessageBuilder.create("flyBoardRenewed", hml)
                    .addPlaceholderReplace("%TIME%", flyDurationSeconds)
                    .addPlaceholderReplace("%COST%", flyCost)
                    .sendMessage(player);
            return true;
        } catch (HookException e) {
            hml.getLogger().log(Level.WARNING, "Couldn't take money from " + player.getName() + " for flying", e);
            return false;
        }
    }

    private void setFlyEnabled(Player player, boolean enabled) {
        player.setAllowFlight(enabled);
        player.setFlying(enabled);
    }

    private void setFlyTimeLeft(Player player, int timeLeftFlying, BukkitTask task) {

        BukkitTask bukkitTask = task;
        if (bukkitTask == null) {
            bukkitTask = flyMap.get(player).getRight();
        }

        flyMap.put(player, new Pair<>((System.currentTimeMillis() / 1000L) + timeLeftFlying, bukkitTask));
        hml.getLandStorage().getLandPlayer(player).setFlyTimeLeft(timeLeftFlying);
    }

    @Override
    public void loadConfig() {
        this.flyCost = hml.getConfig().getDouble("fly.cost", 10d);
        this.flyDurationSeconds = hml.getConfig().getInt("fly.durationSeconds", 600);
    }

    @Override
    public void onDisable() {
        for (Player player : new HashSet<>(flyMap.keySet())) {
            togglePlayerFlying(player);
            MessageBuilder.create("flyBoardDisableForce", hml).sendMessage(player);
        }
    }
}
