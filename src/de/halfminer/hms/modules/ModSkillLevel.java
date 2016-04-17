package de.halfminer.hms.modules;

import de.halfminer.hms.enums.DataType;
import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.handlers.HanTitles;
import de.halfminer.hms.interfaces.Disableable;
import de.halfminer.hms.interfaces.Sweepable;
import de.halfminer.hms.util.HalfminerPlayer;
import de.halfminer.hms.util.Language;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * - Stores skilllevel/elo/groupname in storage
 * - Shows skilllevel in tablist
 * - Colors players name, depending on skillgroup
 * - Calculates new ELO after a kill
 */
@SuppressWarnings("unused")
public class ModSkillLevel extends HalfminerModule implements Disableable, Listener, Sweepable {

    private final HanTitles titleHandler = (HanTitles) hms.getHandler(HandlerType.TITLES);

    private final Scoreboard scoreboard = server.getScoreboardManager().getMainScoreboard();
    private final Map<String, Long> lastKill = new HashMap<>();
    private Objective skillObjective = scoreboard.getObjective("skill");
    private String[] teams;
    private int derankThreshold;
    private int timeUntilDerankSeconds;
    private int timeUntilKillCountAgainSeconds;
    private int derankLossAmount;

    @EventHandler
    public void joinRecalculate(PlayerJoinEvent e) {

        Player player = e.getPlayer();
        HalfminerPlayer hPlayer = storage.getPlayer(player);
        if (player.hasPermission("hms.bypass.skilllevel")) return;

        // Check for derank, if certain skilllevel has been met and no pvp has been made for a certain time
        if (hPlayer.getInt(DataType.SKILL_LEVEL) >= derankThreshold
                && hPlayer.getInt(DataType.LASTKILL) + timeUntilDerankSeconds < (System.currentTimeMillis() / 1000)) {

            hPlayer.set(DataType.LASTKILL, System.currentTimeMillis() / 1000);
            updateSkill(player, derankLossAmount);
            player.sendMessage(Language.getMessagePlaceholders("modSkillLevelDerank", true, "%PREFIX%", "PvP"));

        } else updateSkill(player, 0);

    }

    @EventHandler
    public void killUpdateSkill(PlayerDeathEvent e) {

        Player killer = e.getEntity().getKiller();
        Player victim = e.getEntity().getPlayer();

        if (killer != null && !killer.hasPermission("hms.bypass.skilllevel") && !victim.hasPermission("hms.bypass.skilllevel")) {

            HalfminerPlayer hKiller = storage.getPlayer(killer);
            hKiller.set(DataType.LASTKILL, System.currentTimeMillis() / 1000);

            // Prevent grinding
            String uuidCat = killer.getUniqueId().toString() + victim.getUniqueId().toString();
            if (!killDoesCount(uuidCat)) return;

            // Calculate skill modifier
            int killerLevel = hKiller.getInt(DataType.SKILL_LEVEL);
            int victimLevel = storage.getPlayer(victim).getInt(DataType.SKILL_LEVEL);
            int killerVictimDifference = killerLevel - victimLevel;
            int modifier = ((killerVictimDifference * 3) - 65) * -1;
            if (killerVictimDifference >= 10 && modifier >= 4) modifier /= 4;

            updateSkill(killer, modifier);
            updateSkill(victim, -modifier);
            lastKill.put(uuidCat, System.currentTimeMillis() / 1000);
        }
    }

    public void updateSkill(Player player, int modifier) {

        HalfminerPlayer hPlayer = storage.getPlayer(player);

        int elo = hPlayer.getInt(DataType.SKILL_ELO) + modifier;
        int level = hPlayer.getInt(DataType.SKILL_LEVEL);
        int kdRatio = hPlayer.getInt(DataType.KD_RATIO);

        // Bounds for levels and elo
        if (elo < 0) elo = 0;
        else if (elo > 4200) elo = 4200;
        if (level < 1) level = 1;
        else if (level > 22) level = 22;

        // Equation to determine new level based on ELO (skillnumber)
        int newLevel = level;
        double calc = ((1.9d * elo - (0.0002d * (elo * elo))) / 212) + 1;

        int newLevelUp = (int) Math.ceil(calc);
        int newLevelDown = (int) Math.floor(calc);

        /*
           Example: Player is Level 4, calc is 3.4, you stay level 4 when calc is 3.0 - 4.9, rank down occurs when player
           is lower than 3.0 and rank up when player has reached 5.0. Only rank down when the ceiling of the calc value
           is actually lower than the players level and only rank up when the flooring of the calc is already higher
           than the new level. If modifier is 0, allow both upranking and downranking, otherwise do not rank down on kill
           and do not rank up on death
        */
        if (newLevelDown > level && modifier >= 0) newLevel = newLevelDown;     //rank up
        else if (newLevelUp < level && modifier <= 0) newLevel = newLevelUp;    //rank down

        // Make sure kdratio constraints are met, lower ELO if they are not met
        if (newLevel > 11 && kdRatio < 1.0d) {
            newLevel = 11;
            elo -= 100;
        }
        else if (newLevel > 16 && kdRatio < 3.0d) {
            newLevel = 16;
            elo -= 200;
        }
        else if (newLevel == 22 && kdRatio < 5.0d) {
            newLevel = 21;
            elo -= 300;
        }

        // Set the new values
        String teamName = teams[newLevel - 1];

        skillObjective.getScore(player.getName()).setScore(newLevel);
        scoreboard.getTeam(teamName).addEntry(player.getName());

        teamName = teamName.substring(2); //remove sorting id

        hPlayer.set(DataType.SKILL_ELO, elo);
        hPlayer.set(DataType.SKILL_LEVEL, newLevel);
        hPlayer.set(DataType.SKILL_GROUP, teamName);

        // Send title/log if necessary
        if (newLevel != level) {

            String sendTitle;
            if (newLevel > level) {
                sendTitle = Language.getMessagePlaceholders("modSkillLevelUprankTitle", false, "%SKILLLEVEL%",
                        String.valueOf(newLevel), "%SKILLGROUP%", teamName);
            } else {
                sendTitle = Language.getMessagePlaceholders("modSkillLevelDerankTitle", false, "%SKILLLEVEL%",
                        String.valueOf(newLevel), "%SKILLGROUP%", teamName);
            }
            titleHandler.sendTitle(player, sendTitle, 10, 50, 10);
            hms.getLogger().info(Language.getMessagePlaceholders("modSkillLevelLog", false, "%PLAYER%", player.getName(),
                    "%SKILLOLD%", String.valueOf(level), "%SKILLNEW%", String.valueOf(newLevel), "%SKILLNO%", String.valueOf(elo)));
        }
    }

    private boolean killDoesCount(String uuidCat) {

        return !lastKill.containsKey(uuidCat)
                || lastKill.get(uuidCat) + timeUntilKillCountAgainSeconds
                < System.currentTimeMillis() / 1000;
    }

    @Override
    public void loadConfig() {

        derankThreshold = config.getInt("skillLevel.derankThreshold", 16);
        timeUntilDerankSeconds = config.getInt("skillLevel.timeUntilDerankDays", 4) * 24 * 60 * 60;
        timeUntilKillCountAgainSeconds = config.getInt("skillLevel.timeUntilKillCountAgainMinutes", 10) * 60;
        derankLossAmount = -config.getInt("skillLevel.derankLossAmount", 250);

        List<String> skillGroupConfig = config.getStringList("skillLevel.skillGroups");

        // Ensure that teams are being removed on reload
        if (teams != null) onDisable();

        teams = new String[22]; // First character of String is colorcode, second and third sorting id, rest name
        int sortId = 1;
        for (String skillGroup : skillGroupConfig) {

            int levelsPerGroup = 5;
            if (sortId > 22) break;
            if (sortId == 1 || sortId == 22) levelsPerGroup = 1;

            for (int i = 0; i < levelsPerGroup; i++) {
                String sortString = String.valueOf(sortId);
                if (sortId < 10) sortString = '0' + sortString;
                teams[22 - sortId] = skillGroup.substring(0, 1) + sortString + skillGroup.substring(1);
                sortId++;
            }

        }

        for (int i = 0; i < teams.length; i++) {
            String teamName = teams[i].substring(1);
            String colorCode = teams[i].substring(0, 1);
            if (scoreboard.getTeam(teamName) == null) {
                Team registered = scoreboard.registerNewTeam(teamName);
                registered.setPrefix(ChatColor.COLOR_CHAR + colorCode);
                registered.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            }
            teams[i] = teamName; // Remove color code
        }

        if (skillObjective == null) {
            skillObjective = scoreboard.registerNewObjective("skill", "dummy");
            skillObjective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        }

        for (Player player : server.getOnlinePlayers()) {
            if (!player.hasPermission("hms.bypass.skilllevel")) updateSkill(player, 0);
        }

    }

    @Override
    public void onDisable() {
        // Unregister all registered teams
        Team currentTeam;
        for (String team : teams) {
            if ((currentTeam = scoreboard.getTeam(team)) != null) {
                currentTeam.unregister();
            }
        }
    }

    @Override
    public void sweep() {

        Iterator<Map.Entry<String, Long>> it = lastKill.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> next = it.next();
            if (killDoesCount(next.getKey())) it.remove();
        }
    }
}
