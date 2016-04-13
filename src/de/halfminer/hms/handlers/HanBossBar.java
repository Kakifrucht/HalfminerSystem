package de.halfminer.hms.handlers;

import de.halfminer.hms.interfaces.Disableable;
import de.halfminer.hms.util.Pair;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

/**
 * - Send bar to specific player or broadcast
 * - Set time until bar fades out
 * - Broadcast bar and player bar seperate, only one at a time of each
 */
@SuppressWarnings("SameParameterValue")
public class HanBossBar extends HalfminerHandler implements Disableable {

    // ensure that only one instance of broadcast bar is present
    private final BossBar broadcastBar = server.createBossBar("", BarColor.BLUE, BarStyle.SOLID);
    private final Runnable removalRunnable = new Runnable() {
        @Override
        public void run() {
            removeBar();
        }
    };
    private BukkitTask broadcastRemoveTask;

    private final Map<Player, Pair<BossBar, BukkitTask>> currentBar = new HashMap<>();

    /**
     * Broadcast the bossbar to every player. There can only be one broadcast bossbar at a time.
     * The progression is proportional to the time left.
     * @param text String that will be displayed
     * @param color Color that the bar will be in
     * @param style Style that the bar will be in
     * @param time Int in seconds until the bar will be removed
     */
    public void broadcastBar(final String text, final BarColor color, final BarStyle style, final int time) {

        setBroadcastBar(text, color, style, 1.0d);

        if (broadcastRemoveTask != null) broadcastRemoveTask.cancel();
        broadcastRemoveTask = scheduler.runTaskTimer(hms, new Runnable() {

            int timeLeft = time;

            @Override
            public void run() {

                setProgress(broadcastBar, (double) timeLeft / time);
                if (timeLeft-- < 0) removeBar();
            }
        }, 0L, 20L);
    }

    /**
     * Broadcast the bossbar to every player. There can only be one broadcast bossbar at a time.
     * @param text String that will be displayed
     * @param color Color that the bar will be in
     * @param style Style that the bar will be in
     * @param time Int in seconds until the bar will be removed
     * @param progression amount of progress between 0.00 and 1.00
     */
    public void broadcastBar(final String text, final BarColor color, final BarStyle style, final int time, double progression) {

        setBroadcastBar(text, color, style, progression);

        if (broadcastRemoveTask != null) broadcastRemoveTask.cancel();
        broadcastRemoveTask = scheduler.runTaskLater(hms, removalRunnable, 20 * time);
    }

    private void setBroadcastBar(String text, BarColor color, BarStyle style, double progression) {
        broadcastBar.setTitle(text);
        broadcastBar.setColor(color);
        broadcastBar.setStyle(style);
        setProgress(broadcastBar, progression);
        for (Player online : server.getOnlinePlayers()) broadcastBar.addPlayer(online);
    }

    /**
     * Removes the broadcasted bar
     */
    public void removeBar() {

        broadcastBar.removeAll();
        if (broadcastRemoveTask != null) {
            broadcastRemoveTask.cancel();
            broadcastRemoveTask = null;
        }
    }

    /**
     * Shows a bossbar to one player only. A player can only see one bar at a time. If a bar is still active, it will
     * update the already shown one instead of creating a new one, to also show the bossbar animation.
     * @param player to show the bar to
     * @param text string that will be shown
     * @param color Color that the bar will be in
     * @param style Style that the bar will be in
     * @param time in seconds the bar will be shown
     */
    public void sendBar(final Player player, String text, BarColor color, BarStyle style, final int time) {

        final BossBar bar = getBar(player, text, color, style);

        final BukkitTask cancel = scheduler.runTaskTimer(hms, new Runnable() {
            int currentTime = time;
            @Override
            public void run() {

                setProgress(bar, (double) currentTime / time);
                if (currentTime-- < 0) removeBar(player);
            }
        }, 0L, 20L);
        currentBar.put(player, new Pair<>(bar, cancel));
    }

    /**
     * Shows a bossbar to one player only. A player can only see one bar at a time. If a bar is still active, it will
     * update the already shown one instead of creating a new one, to also show the bossbar animation.
     * @param player to show the bar to
     * @param text string that will be shown
     * @param color Color that the bar will be in
     * @param style Style that the bar will be in
     * @param time in seconds the bar will be shown
     * @param progression double in percent, how filled should the bar be
     */
    public void sendBar(final Player player, String text, BarColor color, BarStyle style, int time, final double progression) {

        final BossBar bar = getBar(player, text, color, style);

        setProgress(bar, progression);

        BukkitTask remove = scheduler.runTaskLater(hms, new Runnable() {

            @Override
            public void run() {
                    removeBar(player);
            }
        }, time * 20);
        currentBar.put(player, new Pair<>(bar, remove));
    }

    /**
     * Remove a players bar
     * @param player player to remove private bossbar
     */
    public void removeBar(Player player) {

        if (currentBar.containsKey(player)) {
            Pair<BossBar, BukkitTask> pair = currentBar.get(player);
            pair.getLeft().removeAll();
            pair.getRight().cancel();
            currentBar.remove(player);
        }
    }

    private BossBar getBar(Player player, String text, BarColor color, BarStyle style) {

        BossBar toReturn;
        if (currentBar.containsKey(player)) {

            toReturn = currentBar.get(player).getLeft();
            toReturn.setTitle(text);
            toReturn.setColor(color);
            toReturn.setStyle(style);
            currentBar.get(player).getRight().cancel();
        } else toReturn = server.createBossBar(text, color, style);

        toReturn.addPlayer(player);
        return toReturn;
    }

    private void setProgress(BossBar bar, double progress) {
        if (progress < 0.0d) bar.setProgress(0.0d);
        else if (progress > 1.0d) bar.setProgress(1.0d);
        else bar.setProgress(progress);
    }

    @Override
    public void onDisable() {
        broadcastBar.removeAll();
        for (Pair<BossBar, BukkitTask> pair : currentBar.values()) pair.getLeft().removeAll();
    }
}
