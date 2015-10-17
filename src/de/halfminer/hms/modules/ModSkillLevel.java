package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
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

        if (storage.getPlayerInt(player, "skilllevel") > 15 &&
                storage.getPlayerInt(player, "lastkill") + timeUntilDerankSeconds < (System.currentTimeMillis() / 1000)) {
            //derank due to inactivity
            updateSkill(player, derankLossAmount);
            player.sendMessage(Language.getMessagePlaceholderReplace("modSkillLevelDerank", true, "%PREFIX%", "PvP"));

        } else updateSkill(player, 0);

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
            updateSkill(victim, -modifier);
        }

    }

    private void updateSkill(Player p, int modifier) {

        int levelNo = storage.incrementPlayerInt(p, "skillnumber", modifier);
        int level = storage.getPlayerInt(p, "skilllevel");

        int newLevel = getLevel(levelNo);

        if (newLevel != level) {

        }




    }

    private int getLevel(int skillNo) {
        //TODO find clever way of implementing this
        return 1;
    }

    @Override
    public void reloadConfig() {

        timeUntilDerankSeconds = hms.getConfig().getInt("skillLevel.timeUntilDerankDays", 4) * 24 * 60 * 60;
        derankLossAmount = -hms.getConfig().getInt("skillLevel.derankLossAmount", 250);

    }

}
