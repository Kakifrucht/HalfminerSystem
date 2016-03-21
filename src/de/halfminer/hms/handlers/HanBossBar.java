package de.halfminer.hms.handlers;

import de.halfminer.hms.util.Pair;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler to send bossbar messages
 * - Set time until fade out
 * - Only show one bar at a time (player specific)
 * - Broadcast bar to all players (besides player specific one)
 */
public class HanBossBar extends HalfminerHandler {

    private BossBar currentBroadcast = hms.getServer().createBossBar("", BarColor.BLUE, BarStyle.SOLID);
    private Map<Player, Pair<BossBar, Long>> currentBar;

    @SuppressWarnings("unused")
    public HanBossBar() {
        reloadConfig();
    }

    public void broadcastBar(String text, BarColor color, BarStyle style, int timeout, double progression) {

        currentBroadcast.setTitle(text);
        currentBroadcast.setColor(color);
        currentBroadcast.setStyle(style);
        currentBroadcast.setProgress(progression);

        for (Player online : hms.getServer().getOnlinePlayers()) currentBroadcast.addPlayer(online);

        hms.getServer().getScheduler().runTaskLater(hms, new Runnable() {
            @Override
            public void run() {
                currentBroadcast.removeAll();
            }
        }, timeout * 20);
    }

    public void removePublicBar() {
        currentBroadcast.removeAll();
    }

    public void showBar(final Player player, String text, BarColor color, BarStyle style, int time, final double progression) {

        final BossBar bar;
        if (currentBar.containsKey(player)) {
            bar = currentBar.get(player).getLeft();
            bar.setTitle(text);
            bar.setColor(color);
            bar.setStyle(style);
        } else bar = hms.getServer().createBossBar(text, color, style);

        bar.setProgress(progression);
        bar.addPlayer(player);

        final long currentTime = System.currentTimeMillis();
        currentBar.put(player, new Pair<>(bar, currentTime));

        hms.getServer().getScheduler().runTaskLater(hms, new Runnable() {

            @Override
            public void run() {

                if (currentBar.get(player).getRight() == currentTime) {

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
