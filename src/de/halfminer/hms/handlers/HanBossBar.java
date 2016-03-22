package de.halfminer.hms.handlers;

import de.halfminer.hms.interfaces.Reloadable;
import de.halfminer.hms.util.Pair;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler to send bossbar messages
 * - Set time until fade out
 * - Only show one bar at a time (player specific)
 * - Broadcast bar to all players (besides player specific one)
 */
@SuppressWarnings("SameParameterValue")
public class HanBossBar extends HalfminerHandler implements Reloadable {

    private final BossBar currentBroadcast = server.createBossBar("", BarColor.BLUE, BarStyle.SOLID);
    // ensure that only one instance is present
    private final Runnable timeoutRunnable = new Runnable() {
        @Override
        public void run() {
            removeBroadcastBar();
        }
    };
    private BukkitTask timeoutCall;
    private Map<Player, Pair<BossBar, Long>> currentBar;

    /**
     * Broadcast the bossbar to every player. There can only be one broadcast bossbar at a time, in order to ensure
     * that it will be removed you can specify a timeout, which will ensure that after the last update and the timeout
     * in seconds every player will be removed from the public broadcast. Caller should also call removeBroadcastBar()
     * to remove the bar after it will no longer be used.
     * @param text String that will be displayed
     * @param color Color that the bar will be in
     * @param style Style that the bar will be in
     * @param timeout Int in seconds until the bar will be removed for sure
     * @param progression double in percent, how filled should the bar be
     */
    public void broadcastBar(String text, BarColor color, BarStyle style, int timeout, double progression) {

        currentBroadcast.setTitle(text);
        currentBroadcast.setColor(color);
        currentBroadcast.setStyle(style);
        currentBroadcast.setProgress(progression);

        for (Player online : server.getOnlinePlayers()) currentBroadcast.addPlayer(online);

        if (timeoutCall != null) timeoutCall.cancel();
        timeoutCall = scheduler.runTaskLater(hms, timeoutRunnable, timeout * 20);
    }

    /**
     * Removes the broadcasted bar from display, also cancelling the timeout, if still active. Should be called
     * manually after a bar has been broadcasted, will also be called after the specified timeout.
     */
    public void removeBroadcastBar() {

        currentBroadcast.removeAll();
        if (timeoutCall != null) {
            timeoutCall.cancel();
            timeoutCall = null;
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
     * @param progression double in percent, how filled should the bar be
     */
    public void showBar(final Player player, String text, BarColor color, BarStyle style, int time, final double progression) {

        final BossBar bar;
        if (currentBar.containsKey(player)) {
            bar = currentBar.get(player).getLeft();
            bar.setTitle(text);
            bar.setColor(color);
            bar.setStyle(style);
        } else bar = server.createBossBar(text, color, style);

        bar.setProgress(progression);
        bar.addPlayer(player);

        final long currentTime = System.currentTimeMillis();
        currentBar.put(player, new Pair<>(bar, currentTime));

        scheduler.runTaskLater(hms, new Runnable() {

            @Override
            public void run() {

                if (currentBar.containsKey(player) && currentBar.get(player).getRight() == currentTime) {

                    bar.removeAll();
                    currentBar.remove(player);
                }
            }
        }, time * 20);
    }

    @Override
    public void reloadConfig() {

        if (currentBar != null) for (Pair<BossBar, Long> barPair : currentBar.values()) barPair.getLeft().removeAll();
        currentBar = new HashMap<>();
    }
}
