package de.halfminer.hms.modules;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles the BossBar
 * - Set time until fade out
 * - Only show one bar at a time
 */
@SuppressWarnings("unused")
public class ModBarHandler extends HalfminerModule {

    private Map<Player, BossBar> currentBar;

    public ModBarHandler() {
        reloadConfig();
    }

    public void showBar(Player player, String text, BarColor color, BarStyle style, int time, double progression) {

        if (currentBar.containsKey(player)) currentBar.get(player).removePlayer(player);

        final BossBar bar = hms.getServer().createBossBar(text, color, style);
        bar.setProgress(progression);
        bar.addPlayer(player);
        currentBar.put(player, bar);

        hms.getServer().getScheduler().runTaskLater(hms, new Runnable() {

            @Override
            public void run() {
                bar.removeAll();
            }
        }, time * 20);
    }

    @Override
    public void reloadConfig() {

        if (currentBar != null) for (BossBar bar : currentBar.values()) bar.removeAll();
        currentBar = new HashMap<>();
    }
}
