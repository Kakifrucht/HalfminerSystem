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
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;

/**
 * This class keeps track of all players that are currently flying, toggles the fly state,
 * gets the price and fly duration from config and automatically renews the flight when
 * the time runs out. If a player is afk during fly, or flies higher than specified in
 * {@link #FLY_CHECK_TASK_TICKS} the fly is cancelled and the remaining time will be saved.
 */
public class FlyBoard extends LandClass implements Disableable, Reloadable {

    private static final int MAX_FLY_HEIGHT = 320;
    private static final long FLY_CHECK_TASK_TICKS = 5 * 20L;

    private final Map<Player, Pair<Long, BukkitTask>> flyMap;

    private double flyCost;
    private int flyDurationSeconds;

    private BukkitTask flyCheckTask;


    FlyBoard() {
        this.flyMap = new HashMap<>();
    }

    public boolean isPlayerFlying(Player player) {
        return flyMap.containsKey(player);
    }

    public boolean togglePlayerFlying(Player player) {

        LandPlayer landPlayer = hml.getLandStorage().getLandPlayer(player);

        boolean doEnable = !flyMap.containsKey(player);
        int timeLeftFlying = getFlyTimeLeft(player);
        if (doEnable) {

            if (timeLeftFlying < 1 && !takePayment(player)) {
                return false;
            }

            long interval = flyDurationSeconds * 20L;
            if (timeLeftFlying < 1) {
                timeLeftFlying = flyDurationSeconds;
            }

            // run money refresh task
            BukkitTask task = scheduler.runTaskTimer(hml, () -> {

                if (takePayment(player)) {
                    setFlyTimeLeft(player, flyDurationSeconds, null);
                } else {
                    togglePlayerFlying(player);
                    MessageBuilder.create("flyBoardDisableNotEnoughMoney", hml).sendMessage(player);
                }

            }, timeLeftFlying * 20L, interval);

            setFlyEnabled(player, true);
            setFlyTimeLeft(player, timeLeftFlying, task);

        } else {
            setFlyEnabled(player, false);
            landPlayer.setFlyTimeLeft(timeLeftFlying);
            flyMap.remove(player).getRight().cancel();
        }

        String logStateString = doEnable ? "enabled" : "disabled";
        hml.getLogger().info("Fly mode " + logStateString + " for " + player.getName()
                + ", time left: " + timeLeftFlying + " seconds");

        // run fly scheduler task if necessary, stop flying if player is afk and enforce fly height limit
        if (!flyMap.isEmpty() && (flyCheckTask == null || flyCheckTask.isCancelled())) {

            flyCheckTask = scheduler.runTaskTimer(hml, () -> {

                if (flyMap.isEmpty()) {
                    flyCheckTask.cancel();
                    return;
                }

                for (Player flyingPlayer : flyMap.keySet()) {

                    if (hms.getHooksHandler().isAfk(flyingPlayer)) {
                        togglePlayerFlying(flyingPlayer);
                        MessageBuilder.create("flyBoardDisableAfk", hml).sendMessage(player);
                    } else if (flyingPlayer.getLocation().getBlockY() > MAX_FLY_HEIGHT) {
                        togglePlayerFlying(flyingPlayer);
                        MessageBuilder.create("flyBoardFlyHeightLimit", hml).sendMessage(player);
                    }
                }

            }, FLY_CHECK_TASK_TICKS, FLY_CHECK_TASK_TICKS);
        }

        return true;
    }

    public void updatePlayerAllowFlight(Player player, Land land) {
        if (isPlayerFlying(player)) {
            boolean isAllowedFlight = land.hasOwner() && land.hasPermission(player);
            if (!player.getAllowFlight() && isAllowedFlight) {
                setFlyEnabled(player, true);
            } else if (player.getAllowFlight() && !isAllowedFlight) {
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
