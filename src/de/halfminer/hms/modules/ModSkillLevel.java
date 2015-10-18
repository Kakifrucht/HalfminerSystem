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
                if (lastKillLong > 0 && lastKillLong + timeUntilKillCountAgainSeconds > System.currentTimeMillis() / 1000) return;
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

    private void updateSkill(Player p, int modifier) {

        int levelNo = storage.incrementPlayerInt(p, "skillnumber", modifier);
        int level = storage.getPlayerInt(p, "skilllevel");

        //bounds for levels and elo
        if (levelNo < 0) levelNo = 0;
        else if (levelNo > 4200) levelNo = 4200;
        if (level < 1) level = 1;
        else if (level > 22) level = 22;
        storage.setPlayer(p, "skillnumber", levelNo);
        storage.setPlayer(p, "skilllevel", level);

        //function to determine new level based on elo (skillnumber)
        int newLevel;
        double calc = ((1.9d * levelNo - (0.0002d * (levelNo * levelNo))) / 212) + 1;
        if (modifier < 0) newLevel = (int) Math.ceil(calc);
        else newLevel = (int) Math.floor(calc);

        if (newLevel != level) {

            String teamName = teams[newLevel - 1].substring(1);
            storage.setPlayer(p, "skilllevel", newLevel);
            storage.setPlayer(p, "skillgroup", teamName);

            skillObjective.getScore(p.getName()).setScore(newLevel);
            scoreboard.getTeam(teamName).addEntry(teamName);

            String sendTitle;
            if (newLevel > level) {
                sendTitle = Language.getMessagePlaceholderReplace("modSkillLevelUprankTitle", false, "%SKILLLEVEL%",
                        String.valueOf(newLevel), "%SKILLGROUP%", teamName);
            } else {
                sendTitle = Language.getMessagePlaceholderReplace("modSkillLevelDerankTitle", false, "%SKILLLEVEL%",
                        String.valueOf(newLevel), "%SKILLGROUP%", teamName);
            }
            TitleSender.sendTitle(p, sendTitle);
            hms.getLogger().info(Language.getMessagePlaceholderReplace("modSkillLevelLog", false, "%PLAYER%", p.getName(),
                    "%SKILLOLD%", String.valueOf(level), "%SKILLNEW%", String.valueOf(newLevel), "%SKILLNO%", String.valueOf(levelNo)));
        }
    }

    @Override
    public void reloadConfig() {

        timeUntilDerankSeconds = hms.getConfig().getInt("skillLevel.timeUntilDerankDays", 4) * 24 * 60 * 60;
        timeUntilKillCountAgainSeconds = hms.getConfig().getInt("skillLevel.timeUntilKillCountAgainMinutes", 10) * 60;
        derankLossAmount = -hms.getConfig().getInt("skillLevel.derankLossAmount", 250);

        //setup scoreboards, TODO do not hardcode teams, more config usage
        teams = new String[]{ //first character is colorcode
                "7Noob",
                "8Eisen", "8Eisen", "8Eisen", "8Eisen", "8Eisen",
                "6Gold", "6Gold", "6Gold", "6Gold", "6Gold",
                "bDiamant", "bDiamant", "bDiamant", "bDiamant", "bDiamant",
                "aEmerald", "aEmerald", "aEmerald", "aEmerald", "aEmerald",
                "0Pro"
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

}
