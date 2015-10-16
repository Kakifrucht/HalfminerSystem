package de.halfminer.hms.modules;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class ModSkillLevel extends HalfminerModule implements Listener {

    private final ModStorage storage = hms.getModStorage();

    private int timeUntilDerankSeconds;
    private int derankLossAmount;

    public ModSkillLevel() {
        reloadConfig();
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        if (player.hasPermission("hms.bypass.skilllevel")) return;
        if (storage.getPlayerInt(e.getPlayer(), "skilllevel") == 0) {
            updateSkill(player, 0);
        }
        if (storage.getPlayerInt(player, "lastkill") + timeUntilDerankSeconds < (System.currentTimeMillis() / 1000)) {
            updateSkill(player, -1 * derankLossAmount);
        }
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onKill(PlayerDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        Player victim = e.getEntity().getPlayer();
        if (killer != null && !killer.hasPermission("hms.bypass.skilllevel") && !victim.hasPermission("hms.bypass.skilllevel")) {

            int modifier;
            int killerLevel = storage.getPlayerInt(killer, "skilllevel");
            int victimLevel = storage.getPlayerInt(victim, "skilllevel");
            if (victimLevel == 1 && killerLevel > 11) modifier = 1;
            else modifier = (((killerLevel - victimLevel) * 3) - 65) * -1;

            updateSkill(killer, modifier);
            updateSkill(victim, modifier * -1);
        }

    }

    private void updateSkill(Player p, int modifier) {

        int levelNo = storage.incrementPlayerInt(p, "skillnumber", modifier);
        int level = storage.getPlayerInt(p, "skilllevel");


        //TODO find clever way of implementing this

    }

    @Override
    public void reloadConfig() {

        timeUntilDerankSeconds = hms.getConfig().getInt("skillLevel.timeUntilDerankDays", 4) * 24 * 60 * 60;
        derankLossAmount = hms.getConfig().getInt("skillLevel.derankLossAmount", 250);

    }

}
