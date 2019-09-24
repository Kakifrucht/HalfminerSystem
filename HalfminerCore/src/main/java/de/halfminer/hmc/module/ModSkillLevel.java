package de.halfminer.hmc.module;

import de.halfminer.hms.handler.storage.DataType;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.manageable.Disableable;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * - PvP based skilllevel system / ELO
 * - Dynamic ELO determination
 *   - Auto derank on inactivity (when rank threshold is met)
 *   - Doesn't count farmed kills
 * - Adds level to scoreboard
 * - Colors name depending on skillgroup
 * - Sorts tablist in descending order
 */
public class ModSkillLevel extends HalfminerModule implements Disableable, Listener {

    private static final int LOWEST_BYPASS_LEVEL = 23;
    private static final int DAYS_IN_SECONDS = 86400;
    private static final double ELO_MULTIPLIER = 0.0155d;
    private static final String SCOREBOARD_OBJECTIVE_NAME = "skill";
    private static final String SCOREBOARD_TEAM_NAME = "Team";

    private Scoreboard scoreboard;

    private Objective scoreboardObjective;
    private List<Team> scoreboardTeamNames;
    private List<Team> scoreboardTeamNamesStaff;

    private int derankLevelThreshold;
    private int timeUntilDerankThreshold;
    private double derankAmountPercent;

    private int eloModifierSameLevel;
    private int maxLevelDifference;

    private String skillgroupNameAdmin;
    private String skillgroupNameNone;


    @EventHandler
    public void joinRecalculate(PlayerJoinEvent e) {

        Player player = e.getPlayer();
        HalfminerPlayer hPlayer = storage.getPlayer(player);
        if (player.hasPermission("hmc.bypass.skilllevel")) {
            updateSkillBypassedPlayer(player);
        } else {
            // Check for derank, if certain skilllevel has been met and elo hasn't changed for a certain time
            if (hPlayer.getInt(DataType.SKILL_LEVEL) >= derankLevelThreshold
                    && hPlayer.getInt(DataType.SKILL_ELO_LAST_CHANGE) + timeUntilDerankThreshold < (System.currentTimeMillis() / 1000)) {

                int skillEloChange = (int) -Math.round((double) hPlayer.getInt(DataType.SKILL_ELO) * derankAmountPercent);
                updateSkill(player, skillEloChange);
                MessageBuilder.create("modSkillLevelDerank", hmc, "PvP")
                        .addPlaceholder("%DAYS%", timeUntilDerankThreshold / DAYS_IN_SECONDS)
                        .sendMessage(player);
            } else {
                updateSkill(player, 0);
            }
        }
    }

    @EventHandler
    public void killUpdateSkill(PlayerDeathEvent e) {

        Player killer = e.getEntity().getKiller();
        Player victim = e.getEntity().getPlayer();

        if (killer != null
                && killer != victim
                && !killer.hasPermission("hmc.bypass.skilllevel.update")
                && !victim.hasPermission("hmc.bypass.skilllevel.update")) {

            HalfminerPlayer hKiller = storage.getPlayer(killer);
            HalfminerPlayer hVictim = storage.getPlayer(victim);

            // Prevent grinding
            boolean isNotRepeatedKill = true;
            try {
                ModAntiKillfarming antiKillfarming = (ModAntiKillfarming) hmc.getModule(ModuleType.ANTI_KILLFARMING);
                isNotRepeatedKill = antiKillfarming.isNotRepeatedKill(killer, victim);
            } catch (ModuleDisabledException ignored) {}
            if (isNotRepeatedKill) {

                // Calculate skill modifier
                int killerElo = hKiller.getInt(DataType.SKILL_ELO);
                int killerLevel = hKiller.getInt(DataType.SKILL_LEVEL);

                int victimElo = hVictim.getInt(DataType.SKILL_ELO);
                int victimLevel = hVictim.getInt(DataType.SKILL_LEVEL);

                int killerVictimDifferenceLevel = killerLevel - victimLevel;
                int killerVictimDifferenceElo = killerElo - victimElo;

                int eloModifier = (((int) Math.round(killerVictimDifferenceElo * ELO_MULTIPLIER)) - eloModifierSameLevel) * -1;
                if (killerVictimDifferenceLevel >= maxLevelDifference) {
                    eloModifier = 1;
                }

                updateSkill(killer, eloModifier);
                updateSkill(victim, -eloModifier);
            }
        }
    }

    public void updateSkill(OfflinePlayer player, int modifier) {

        HalfminerPlayer hPlayer = storage.getPlayer(player);

        int elo = hPlayer.getInt(DataType.SKILL_ELO) + modifier;
        int level = hPlayer.getInt(DataType.SKILL_LEVEL);

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

        // minimum rank when deranking is level 2
        if (level > 1) {
            newLevelUp = Math.max(newLevelUp, 2);
        }

        /*
           Example: Player is Level 4, calc is 3.4, you stay level 4 when calc is 3.0 - 4.9, rank down occurs when player
           is lower than 3.0 and rank up when player has reached 5.0. Only rank down when the ceiling of the calc value
           is actually lower than the players level and only rank up when the flooring of the calc is already higher
           than the new level. If modifier is 0, allow both upranking and deranking, otherwise do not rank down on kill
           and do not rank up on death
        */
        if (newLevelDown > level && modifier >= 0) {        // rank up
            newLevel = newLevelDown;
        } else if (newLevelUp < level && modifier <= 0) {   // rank down
            newLevel = newLevelUp;
        }

        // Set the new values
        Team newTeam = scoreboardTeamNames.get(newLevel - 1);
        newTeam.addEntry(player.getName());
        scoreboardObjective.getScore(player.getName()).setScore(newLevel);

        hPlayer.set(DataType.SKILL_ELO, elo);
        hPlayer.set(DataType.SKILL_LEVEL, newLevel);

        if (modifier != 0) {
            hPlayer.set(DataType.SKILL_ELO_LAST_CHANGE, System.currentTimeMillis() / 1000);
        }

        // Send title/log if necessary
        if (newLevel != level) {

            if (player instanceof Player) {
                titleHandler.sendTitle((Player) player,
                        MessageBuilder.create(newLevel > level ?
                                "modSkillLevelUprankTitle" : "modSkillLevelDerankTitle", hmc)
                                .addPlaceholder("%SKILLLEVEL%", newLevel)
                                .addPlaceholder("%SKILLGROUP%", newTeam.getName().substring(2))
                                .returnMessage(),
                        10, 50, 10, 10);
            }

            MessageBuilder.create("modSkillLevelLog", hmc)
                    .addPlaceholder("%PLAYER%", player.getName())
                    .addPlaceholder("%SKILLOLD%", level)
                    .addPlaceholder("%SKILLNEW%", newLevel)
                    .addPlaceholder("%SKILLNO%", elo)
                    .logMessage(Level.INFO);
        }
    }

    private void updateSkillBypassedPlayer(Player p) {

        HalfminerPlayer hPlayer = storage.getPlayer(p);
        int playerLevel = hPlayer.getInt(DataType.SKILL_LEVEL);
        if (playerLevel < LOWEST_BYPASS_LEVEL) {
            playerLevel = LOWEST_BYPASS_LEVEL;
            hPlayer.set(DataType.SKILL_LEVEL, LOWEST_BYPASS_LEVEL);
        }
        scoreboardObjective.getScore(p.getName()).setScore(playerLevel);

        // first team level is 23, set it to at least this value
        int maxIndex = scoreboardTeamNamesStaff.size() - 1;
        int staffArrayIndex = Math.max(0, maxIndex - (playerLevel - LOWEST_BYPASS_LEVEL));
        scoreboardTeamNamesStaff.get(staffArrayIndex).addEntry(p.getName());
    }

    public String getSkillgroup(OfflinePlayer player) {

        int skillLevel = storage.getPlayer(player).getInt(DataType.SKILL_LEVEL);

        if (skillLevel <= 22 && skillLevel > 0) {
            return scoreboardTeamNames.get(skillLevel - 1).getName().substring(2);
        } else if (skillLevel < 1) {
            return skillgroupNameNone;
        } else {
            return skillgroupNameAdmin;
        }
    }

    @Override
    public void loadConfig() {

        ConfigurationSection config = hmc.getConfig().getConfigurationSection("skillLevel");
        derankLevelThreshold = config.getInt("derankThreshold", 16);
        timeUntilDerankThreshold = config.getInt("timeUntilDerankDays", 4) * 24 * 60 * 60;
        derankAmountPercent = config.getDouble("derankAmountPercent", .15d);

        eloModifierSameLevel = config.getInt("eloModifierSameLevel", 80);
        maxLevelDifference = config.getInt("maxLevelDifference", 10);

        skillgroupNameAdmin = MessageBuilder.returnMessage("modSkillLevelAdmingroupName", hmc);
        skillgroupNameNone = MessageBuilder.returnMessage("modSkillLevelNoGroup", hmc);

        if (scoreboard == null) {
            scoreboard = server.getScoreboardManager().getMainScoreboard();
        } else {
            // if reloaded re-register everything
            onDisable();
        }

        // register skilllevel objective
        scoreboardObjective = scoreboard.getObjective(SCOREBOARD_OBJECTIVE_NAME);
        if (scoreboardObjective == null) {
            scoreboardObjective = scoreboard.registerNewObjective(SCOREBOARD_OBJECTIVE_NAME, "dummy", "SkillLevel");
        }
        scoreboardObjective.setDisplaySlot(DisplaySlot.PLAYER_LIST);

        // register player teams
        scoreboardTeamNames = new ArrayList<>(Collections.nCopies(22, null));
        int sortId = 1;
        for (String skillGroup : config.getStringList("skillGroups")) {

            if (sortId > 22) break;

            // first character is the color code
            String colorCode = skillGroup.substring(0, 1);
            String groupName = skillGroup.substring(1);
            int levelsPerGroup = sortId == 1 || sortId == 22 ? 1 : 5;

            for (int i = 0; i < levelsPerGroup; i++) {
                String sortString = String.valueOf(sortId);
                if (sortId < 10) sortString = '0' + sortString;

                // add the sortstring to the team name to order the tablist
                String teamName = sortString + groupName;
                Team registered;
                if ((registered = scoreboard.getTeam(teamName)) == null) {
                    registered = scoreboard.registerNewTeam(teamName);
                }
                registered.setPrefix(ChatColor.COLOR_CHAR + colorCode);
                registered.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
                scoreboardTeamNames.set(22 - sortId, registered);
                sortId++;
            }
        }

        // register staff teams
        int amountTeamGroups = config.getInt("amountTeamGroups", 3);
        scoreboardTeamNamesStaff = new ArrayList<>(Collections.nCopies(amountTeamGroups, null));

        for (int i = 0; i < amountTeamGroups; i++) {
            StringBuilder indexToString = new StringBuilder(String.valueOf(i));
            while (indexToString.length() < 3) {
                indexToString.insert(0, '0');
            }
            String teamName = indexToString + SCOREBOARD_TEAM_NAME;
            Team registered;
            if ((registered = scoreboard.getTeam(teamName)) == null) {
                registered = scoreboard.registerNewTeam(teamName);
            }
            registered.setPrefix(ChatColor.BOLD.toString());
            registered.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            scoreboardTeamNamesStaff.set(i, registered);
        }

        // reload all online players
        for (Player player : server.getOnlinePlayers()) {
            if (player.hasPermission("hmc.bypass.skilllevel")) {
                updateSkillBypassedPlayer(player);
            } else {
                updateSkill(player, 0);
            }
        }
    }

    @Override
    public void onDisable() {
        // Unregister all objectives and teams
        scoreboardObjective.unregister();
        scoreboardTeamNames.forEach(Team::unregister);
        scoreboardTeamNamesStaff.forEach(Team::unregister);
    }
}
