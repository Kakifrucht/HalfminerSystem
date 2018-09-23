package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
import de.halfminer.hmc.module.ModSkillLevel;
import de.halfminer.hmc.module.ModuleType;
import de.halfminer.hms.handler.storage.DataType;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.handler.storage.PlayerNotFoundException;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * - View own / other players stats
 * - Allows to compare statistics easily
 */
@SuppressWarnings("unused")
public class Cmdstats extends HalfminerCommand {

    public Cmdstats() {
        this.permission = "hmc.stats";
    }

    @Override
    public void execute() {

        if (args.length > 0) {

            boolean compare = args.length > 1 && args[1].equalsIgnoreCase("compare") && sender instanceof Player;
            try {
                showStats(storage.getPlayer(args[0]), compare);
            } catch (PlayerNotFoundException e) {
                e.sendNotFoundMessage(sender, "Stats");
            }

        } else {

            if (isPlayer) {
                showStats(storage.getPlayer((Player) sender), false);
            } else {
                sendNotAPlayerMessage("Stats");
            }
        }
    }

    private void showStats(final HalfminerPlayer player, boolean compare) {

        HalfminerPlayer compareWith = null;
        if (compare && !sender.equals(player.getBase())) {
            compareWith = storage.getPlayer((Player) sender);
        }

        MessageBuilder.create("cmdStatsHeader", hmc).sendMessage(sender);
        MessageBuilder.create("cmdStatsShow", hmc)
                .addPlaceholderReplace("%PLAYER%", player.getName())
                .addPlaceholderReplace("%SKILLGROUP%",
                        ((ModSkillLevel) hmc.getModule(ModuleType.SKILL_LEVEL)).getSkillgroup(player.getBase()))
                .addPlaceholderReplace("%SKILLLEVEL%", getIntAndCompare(player, DataType.SKILL_LEVEL, compareWith))
                .addPlaceholderReplace("%ONLINETIME%", getIntAndCompare(player, DataType.TIME_ONLINE, compareWith))
                .addPlaceholderReplace("%KILLS%", getIntAndCompare(player, DataType.KILLS, compareWith))
                .addPlaceholderReplace("%DEATHS%", getIntAndCompare(player, DataType.DEATHS, compareWith))
                .addPlaceholderReplace("%KDRATIO%", getDoubleAndCompare(player, DataType.KD_RATIO, compareWith))
                .addPlaceholderReplace("%VOTES%", getIntAndCompare(player, DataType.VOTES, compareWith))
                .addPlaceholderReplace("%REVENUE%", getDoubleAndCompare(player, DataType.REVENUE, compareWith))
                .addPlaceholderReplace("%MOBKILLS%", getIntAndCompare(player, DataType.MOB_KILLS, compareWith))
                .addPlaceholderReplace("%BLOCKSPLACED%", getIntAndCompare(player, DataType.BLOCKS_PLACED, compareWith))
                .addPlaceholderReplace("%BLOCKSBROKEN%", getIntAndCompare(player, DataType.BLOCKS_BROKEN, compareWith))
                .sendMessage(sender);

        List<String> previousNames = player.getPreviousNames();
        if (!previousNames.isEmpty()) {

            String previousNamesToSend = player.getString(DataType.PREVIOUS_NAMES);

            int toDisplayMax = hmc.getConfig().getInt("cmd.stats.previousNamesMax", 4);
            if (previousNames.size() > toDisplayMax && previousNames.size() > 1) {

                // if configured limit has been reached, show the first seen, most recent and two random names in between
                StringBuilder sb = new StringBuilder(previousNames.get(0)).append(' ');
                previousNames.remove(0);

                String mostRecentName = previousNames.get(previousNames.size() - 1);
                previousNames.remove(mostRecentName);

                Collections.shuffle(previousNames);
                for (int i = 0; i < previousNames.size() && i < 2; i++) {
                    sb.append(previousNames.get(i)).append(' ');
                }

                sb.append(mostRecentName);
                previousNamesToSend = sb.toString();
            }

            MessageBuilder.create("cmdStatsPreviousNames", hmc)
                    .addPlaceholderReplace("%PREVIOUSNAMES%", previousNamesToSend)
                    .sendMessage(sender);
        }

        if (sender.equals(player.getBase())) {
            MessageBuilder.create("cmdStatsShowotherStats", hmc).sendMessage(sender);
        } else if (compare) {
            MessageBuilder.create("cmdStatsCompareLegend", hmc).sendMessage(sender);
        } else if (sender instanceof Player) {
            MessageBuilder.create("cmdStatsCompareInfo", hmc)
                    .addPlaceholderReplace("%PLAYER%", player.getName())
                    .sendMessage(sender);
        }

        MessageBuilder.create("lineSeparator").sendMessage(sender);
    }

    @SuppressWarnings("Duplicates")
    private String getIntAndCompare(HalfminerPlayer player, DataType type, HalfminerPlayer compareWith) {
        String returnString = "";
        int playerVar = player.getInt(type);

        if (compareWith != null) {
            int compareVar = compareWith.getInt(type);
            if (playerVar < compareVar) returnString = ChatColor.GREEN.toString();
            else if (playerVar > compareVar) returnString = ChatColor.RED.toString();
            else returnString = ChatColor.YELLOW.toString();
        }

        if (type == DataType.TIME_ONLINE) playerVar /= 60;
        return returnString + playerVar;
    }

    @SuppressWarnings("Duplicates")
    private String getDoubleAndCompare(HalfminerPlayer player, DataType type, HalfminerPlayer compareWith) {

        // code duplicated, as Java doesn't offer simple ways to abstract primitive types
        String returnString = "";
        double playerVar = player.getDouble(type);

        if (compareWith != null) {
            double compareVar = compareWith.getDouble(type);
            if (playerVar < compareVar) returnString = ChatColor.GREEN.toString();
            else if (playerVar > compareVar) returnString = ChatColor.RED.toString();
            else returnString = ChatColor.YELLOW.toString();
        }

        return returnString + playerVar;
    }
}
