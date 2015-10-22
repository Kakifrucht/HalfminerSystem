package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
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

        if (storage.getPlayerInt(player, "skilllevel") >= derankThreshold &&
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
        int newLevel = level;
        double calc = ((1.9d * elo - (0.0002d * (elo * elo))) / 212) + 1; //TODO optimize function

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

        storage.setPlayer(player, "skillelo", elo);
        storage.setPlayer(player, "skilllevel", newLevel);
        storage.setPlayer(player, "skillgroup", teamName);

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

        //setup scoreboards, TODO find good way for move to config
        teams = new String[]{ //first character is colorcode, second and third sorting id
                "722Noob",
                "821Eisen", "820Eisen", "819Eisen", "818Eisen", "817Eisen",
                "616Gold", "615Gold", "614Gold", "613Gold", "612Gold",
                "b11Diamant", "b10Diamant", "b09Diamant", "b08Diamant", "b07Diamant",
                "a06Emerald", "a05Emerald", "a04Emerald", "a03Emerald", "a02Emerald",
                "001Pro"
        };

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
            if ((currentTeam = scoreboard.getTeam(team.substring(1))) != null) {
                currentTeam.unregister();
            }
        }
    }

}
