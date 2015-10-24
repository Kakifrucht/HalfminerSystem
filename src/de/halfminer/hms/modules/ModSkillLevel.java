package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.StatsType;
import de.halfminer.hms.util.TitleSender;
import org.bukkit.ChatColor;
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
import java.util.List;
import java.util.Map;

public class ModSkillLevel extends HalfminerModule implements Listener {

    private final Scoreboard scoreboard = hms.getServer().getScoreboardManager().getMainScoreboard();
    private final Map<String, Long> lastKill = new HashMap<>();
    private Objective skillObjective = scoreboard.getObjective("skill");
    private String[] teams;
    private int derankThreshold;
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

        //check for derank, if certain skilllevel has been met and no pvp has been made for a certain time
        if (storage.getStatsInt(player, StatsType.SKILL_LEVEL) >= derankThreshold
                && storage.getStatsInt(player, StatsType.LASTKILL) + timeUntilDerankSeconds < (System.currentTimeMillis() / 1000)) {

            storage.setStats(player, StatsType.LASTKILL, System.currentTimeMillis() / 1000);
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

            storage.setStats(killer, StatsType.LASTKILL, System.currentTimeMillis() / 1000);

            //Check if last kill has passed a certain time, otherwise do not count towards skilllevel (prevent grinding)
            if (lastKill.containsKey(killer.getName() + victim.getName())) {
                long lastKillLong = lastKill.get(killer.getName() + victim.getName());
                if (lastKillLong + timeUntilKillCountAgainSeconds > System.currentTimeMillis() / 1000) return;
            }

            //calculate skill modifier
            int killerLevel = storage.getStatsInt(killer, StatsType.SKILL_LEVEL);
            int victimLevel = storage.getStatsInt(victim, StatsType.SKILL_LEVEL);
            int modifier = (((killerLevel - victimLevel) * 3) - 65) * -1;

            updateSkill(killer, modifier);
            updateSkill(victim, -modifier);
            lastKill.put(killer.getName() + victim.getName(), System.currentTimeMillis() / 1000);

        }
    }

    public void updateSkill(Player player, int modifier) {

        int elo = storage.getStatsInt(player, StatsType.SKILL_ELO) + modifier;
        int level = storage.getStatsInt(player, StatsType.SKILL_LEVEL);
        int kdRatio = storage.getStatsInt(player, StatsType.KD_RATIO);

        //bounds for levels and elo
        if (elo < 0) elo = 0;
        else if (elo > 4200) elo = 4200;
        if (level < 1) level = 1;
        else if (level > 22) level = 22;

        //function to determine new level based on elo (skillnumber)
        int newLevel = level;
        double calc = ((1.9d * elo - (0.0002d * (elo * elo))) / 212) + 1;

        int newLevelUp = (int) Math.ceil(calc);
        int newLevelDown = (int) Math.floor(calc);
        /*
           Example: Player is Level 4, calc is 3.4, you stay level 4 when calc is 3.0 - 4.9, rank down occurs when player
           is lower than 3.0 and rank up when player has reached 5.0. Only rank down when the ceiling of the calc value
           is actually lower than the players level and only rank up when the flooring of the calc is already higher
           than the new level. If modifier is 0, allow both upranking and downranking, otherwise do not rank down on kill
           and no not rank up on death
        */
        if (newLevelDown > level && modifier >= 0) newLevel = newLevelDown;     //rank up
        else if (newLevelUp < level && modifier <= 0) newLevel = newLevelUp;    //rank down

        //make sure kdratio constraints are met
        if (newLevel > 11 && kdRatio < 1.0d) newLevel = 11;
        else if (newLevel > 16 && kdRatio < 3.0d) newLevel = 16;
        else if (newLevel == 22 && kdRatio < 5.0d) newLevel = 21;

        //Set the new values
        String teamName = teams[newLevel - 1];

        skillObjective.getScore(player.getName()).setScore(newLevel);
        scoreboard.getTeam(teamName).addEntry(player.getName());

        teamName = teamName.substring(2); //remove sorting id

        storage.setStats(player, StatsType.SKILL_ELO, elo);
        storage.setStats(player, StatsType.SKILL_LEVEL, newLevel);
        storage.setStats(player, StatsType.SKILL_GROUP, teamName);

        //Send title/log if necessary
        if (newLevel != level) {

            String sendTitle;
            if (newLevel > level) {
                sendTitle = Language.getMessagePlaceholderReplace("modSkillLevelUprankTitle", false, "%SKILLLEVEL%",
                        String.valueOf(newLevel), "%SKILLGROUP%", teamName);
            } else {
                sendTitle = Language.getMessagePlaceholderReplace("modSkillLevelDerankTitle", false, "%SKILLLEVEL%",
                        String.valueOf(newLevel), "%SKILLGROUP%", teamName);
            }
            TitleSender.sendTitle(player, sendTitle, 10, 50, 10);
            hms.getLogger().info(Language.getMessagePlaceholderReplace("modSkillLevelLog", false, "%PLAYER%", player.getName(),
                    "%SKILLOLD%", String.valueOf(level), "%SKILLNEW%", String.valueOf(newLevel), "%SKILLNO%", String.valueOf(elo)));
        }
    }

    @Override
    public void reloadConfig() {

        derankThreshold = hms.getConfig().getInt("skillLevel.derankThreshold", 16);
        timeUntilDerankSeconds = hms.getConfig().getInt("skillLevel.timeUntilDerankDays", 4) * 24 * 60 * 60;
        timeUntilKillCountAgainSeconds = hms.getConfig().getInt("skillLevel.timeUntilKillCountAgainMinutes", 10) * 60;
        derankLossAmount = -hms.getConfig().getInt("skillLevel.derankLossAmount", 250);

        List<String> skillGroupConfig = hms.getConfig().getStringList("skillLevel.skillGroups");

        //ensure that teams are being removed on reload
        if (teams != null) onDisable();

        teams = new String[22]; //first character of String is colorcode, second and third sorting id, rest name
        int sortId = 1;
        for (String skillGroup : skillGroupConfig) {

            //Highest level and lowest level only once
            if (sortId == 1) {

                teams[21] = skillGroup.substring(0, 1) + "01" + skillGroup.substring(1);
                sortId++;

            } else if (sortId == 22) {

                teams[0] = skillGroup.substring(0, 1) + "22" + skillGroup.substring(1);
                sortId++;

            } else if (sortId > 22) {
                break;
            } else {
                for (int i = 0; i < 5; i++) {
                    String sortString = String.valueOf(sortId);
                    if (sortId < 10) sortString = '0' + sortString;
                    teams[22 - sortId] = skillGroup.substring(0, 1) + sortString + skillGroup.substring(1);
                    sortId++;
                }
            }
        }

        for (int i = 0; i < teams.length; i++) {
            String teamName = teams[i].substring(1);
            String colorCode = teams[i].substring(0, 1);
            if (scoreboard.getTeam(teamName) == null) {
                Team registered = scoreboard.registerNewTeam(teamName);
                registered.setPrefix(ChatColor.COLOR_CHAR + colorCode);
            }
            teams[i] = teamName; //Remove color code
        }

        if (skillObjective == null) {
            skillObjective = scoreboard.registerNewObjective("skill", "dummy");
            skillObjective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        }

        for (Player player : hms.getServer().getOnlinePlayers()) {
            if (!player.hasPermission("hms.bypass.skilllevel")) updateSkill(player, 0);
        }

    }

    @Override
    public void onDisable() {
        //unregister all registered teams
        Team currentTeam;
        for (String team : teams) {
            if ((currentTeam = scoreboard.getTeam(team)) != null) {
                currentTeam.unregister();
            }
        }
    }

}
