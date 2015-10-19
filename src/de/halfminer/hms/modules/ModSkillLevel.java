package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.TitleSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;

public class ModSkillLevel extends HalfminerModule implements Listener {

    private final ModStorage storage = hms.getModStorage();

    private final Scoreboard scoreboard = hms.getServer().getScoreboardManager().getMainScoreboard();
    private Objective skillObjective = scoreboard.getObjective("Skill");

    private final Map<String, Long> lastKill = new HashMap<>();
    private String[] teams;
    private int timeUntilDerankSeconds;
    private int timeUntilKillCountAgainSeconds;
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
            storage.setPlayer(player, "lastkill", System.currentTimeMillis() / 1000);
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

            storage.setPlayer(killer, "lastkill", System.currentTimeMillis() / 1000);

            if (lastKill.containsKey(killer.getName() + victim.getName())) {
                long lastKillLong = lastKill.get(killer.getName() + victim.getName());
                if (lastKillLong > 0 && lastKillLong + timeUntilKillCountAgainSeconds > System.currentTimeMillis() / 1000)
                    return;
            }

            int modifier;
            int killerLevel = storage.getPlayerInt(killer, "skilllevel");
            int victimLevel = storage.getPlayerInt(victim, "skilllevel");
            if (victimLevel == 1 && killerLevel > 11) modifier = 1;
            else modifier = (((killerLevel - victimLevel) * 3) - 65) * -1;

            updateSkill(killer, modifier);
            updateSkill(victim, -modifier);
            lastKill.put(killer.getName() + victim.getName(), System.currentTimeMillis() / 1000);
        }

    }

    public void updateSkill(Player player, int modifier) {

        int elo = storage.getPlayerInt(player, "skillelo") + modifier;
        int level = storage.getPlayerInt(player, "skilllevel");
        int kdRatio = storage.getPlayerInt(player, "kdratio");

        //bounds for levels and elo
        if (elo < 0) elo = 0;
        else if (elo > 4200) elo = 4200;
        if (level < 1) level = 1;
        else if (level > 22) level = 22;

        //function to determine new level based on elo (skillnumber)
        int newLevel;
        double calc = ((1.9d * elo - (0.0002d * (elo * elo))) / 212) + 1;
        if (modifier < 0) newLevel = (int) Math.ceil(calc);
        else newLevel = (int) Math.floor(calc);

        //make sure kdratio constraints are met
        if (newLevel > 11 && kdRatio < 1.0d) newLevel = 11;
        else if (newLevel > 16 && kdRatio < 3.0d) newLevel = 16;
        else if (newLevel == 22 && kdRatio < 5.0d) newLevel = 21;

        //Set the new values
        String teamName = teams[newLevel - 1].substring(1);
        storage.setPlayer(player, "skillelo", elo);
        storage.setPlayer(player, "skilllevel", newLevel);
        storage.setPlayer(player, "skillgroup", teamName.substring(1));

        skillObjective.getScore(player.getName()).setScore(newLevel);
        scoreboard.getTeam(teamName).addEntry(player.getName());

        //Send title/log if necessary
        if (newLevel != level) {

            String sendTitle;
            if (newLevel > level) {
                sendTitle = Language.getMessagePlaceholderReplace("modSkillLevelUprankTitle", false, "%SKILLLEVEL%",
                        String.valueOf(newLevel), "%SKILLGROUP%", teamName.substring(1));
            } else {
                sendTitle = Language.getMessagePlaceholderReplace("modSkillLevelDerankTitle", false, "%SKILLLEVEL%",
                        String.valueOf(newLevel), "%SKILLGROUP%", teamName.substring(1));
            }
            TitleSender.sendTitle(player, sendTitle, 10, 50, 10);
            hms.getLogger().info(Language.getMessagePlaceholderReplace("modSkillLevelLog", false, "%PLAYER%", player.getName(),
                    "%SKILLOLD%", String.valueOf(level), "%SKILLNEW%", String.valueOf(newLevel), "%SKILLNO%", String.valueOf(elo)));
        }
    }

    @Override
    public void reloadConfig() {

        timeUntilDerankSeconds = hms.getConfig().getInt("skillLevel.timeUntilDerankDays", 4) * 24 * 60 * 60;
        timeUntilKillCountAgainSeconds = hms.getConfig().getInt("skillLevel.timeUntilKillCountAgainMinutes", 10) * 60;
        derankLossAmount = -hms.getConfig().getInt("skillLevel.derankLossAmount", 250);

        //setup scoreboards, TODO find good way for move to config
        teams = new String[]{ //first character is colorcode, second sorting id
                "76Noob",
                "85Eisen", "85Eisen", "85Eisen", "85Eisen", "85Eisen",
                "64Gold", "64Gold", "64Gold", "64Gold", "64Gold",
                "b3Diamant", "b3Diamant", "b3Diamant", "b3Diamant", "b3Diamant",
                "a2Emerald", "a2Emerald", "a2Emerald", "a2Emerald", "a2Emerald",
                "01Pro"
        };

        for (String team : teams) {
            if (scoreboard.getTeam(team.substring(1)) == null) {
                Team registered = scoreboard.registerNewTeam(team.substring(1));
                registered.setPrefix('§' + team.substring(0, 1));
            }
        }

        if (skillObjective == null) {
            skillObjective = scoreboard.registerNewObjective("Skill", "dummy");
            skillObjective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        }

    }

    @Override
    public void onDisable() {
        //unregister all registered teams to clean up
        Team currentTeam;
        for (String team : teams) {
            if ((currentTeam = scoreboard.getTeam(team.substring(1))) != null) {
                currentTeam.unregister();
            }
        }
    }

}
