package de.halfminer.hms.cmd;

import de.halfminer.hms.enums.DataType;
import de.halfminer.hms.exception.PlayerNotFoundException;
import de.halfminer.hms.util.HalfminerPlayer;
import de.halfminer.hms.util.Language;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * - View own / other players stats
 * - Allows to compare statistics easily
 */
@SuppressWarnings("unused")
public class Cmdstats extends HalfminerCommand {

    public Cmdstats() {
        this.permission = "hms.stats";
    }

    @Override
    public void run(CommandSender sender, String label, String[] args) {

        if (args.length > 0) {

            boolean compare = false;
            if (args.length > 1 && args[1].equalsIgnoreCase("compare") && sender instanceof Player) compare = true;
            try {
                showStats(sender, storage.getPlayer(args[0]), compare);
            } catch (PlayerNotFoundException e) {
                e.sendNotFoundMessage(sender, "Stats");
            }

        } else {

            if (sender instanceof Player) showStats(sender, storage.getPlayer((Player) sender), false);
            else sender.sendMessage(Language.getMessage("notAPlayer"));

        }

    }

    private void showStats(final CommandSender sendTo, final HalfminerPlayer player, boolean compare) {

        HalfminerPlayer compareWith = null;
        if (compare && !sendTo.equals(player)) compareWith = storage.getPlayer((Player) sendTo);

        final String oldNames = player.getString(DataType.LAST_NAMES);

        // build the message
        String message = Language.getMessage("commandStatsTop") + "\n";
        message += Language.getMessagePlaceholders("commandStatsShow", false,
                "%PLAYER%", player.getString(DataType.LAST_NAME),
                "%SKILLGROUP%", player.getString(DataType.SKILL_GROUP),
                "%SKILLLEVEL%", getIntAndCompare(player, DataType.SKILL_LEVEL, compareWith),
                "%ONLINETIME%", getIntAndCompare(player, DataType.TIME_ONLINE, compareWith),
                "%KILLS%", getIntAndCompare(player, DataType.KILLS, compareWith),
                "%DEATHS%", getIntAndCompare(player, DataType.DEATHS, compareWith),
                "%KDRATIO%", getDoubleAndCompare(player, DataType.KD_RATIO, compareWith),
                "%VOTES%", getIntAndCompare(player, DataType.VOTES, compareWith),
                "%REVENUE%", getDoubleAndCompare(player, DataType.REVENUE, compareWith),
                "%MOBKILLS%", getIntAndCompare(player, DataType.MOB_KILLS, compareWith),
                "%BLOCKSPLACED%", getIntAndCompare(player, DataType.BLOCKS_PLACED, compareWith),
                "%BLOCKSBROKEN%", getIntAndCompare(player, DataType.BLOCKS_BROKEN, compareWith),
                "%OLDNAMES%", oldNames) + "\n";

        if (oldNames.length() > 0)
            message += Language.getMessagePlaceholders("commandStatsOldnames", false,
                    "%OLDNAMES%", oldNames) + "\n";

        if (sendTo.equals(player))
            message += Language.getMessage("commandStatsShowotherStats") + "\n";
        else if (compare) {
            message += Language.getMessage("commandStatsCompareLegend") + "\n";
        } else if (sendTo instanceof Player) {
            message += Language.getMessagePlaceholders("commandStatsCompareInfo", false,
                    "%PLAYER%", player.getString(DataType.LAST_NAME)) + "\n";
        }

        message += Language.getMessage("lineSeparator");
        sendTo.sendMessage(message);

    }

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
        return returnString + String.valueOf(playerVar);
    }

    private String getDoubleAndCompare(HalfminerPlayer player, DataType type, HalfminerPlayer compareWith) {
        String returnString = "";
        double playerVar = player.getDouble(type);

        if (compareWith != null) {
            double compareVar = compareWith.getDouble(type);
            if (playerVar < compareVar) returnString = ChatColor.GREEN.toString();
            else if (playerVar > compareVar) returnString = ChatColor.RED.toString();
            else returnString = ChatColor.YELLOW.toString();
        }

        return returnString + String.valueOf(playerVar);
    }

}
