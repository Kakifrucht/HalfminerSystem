package de.halfminer.hms.cmd;

import de.halfminer.hms.exception.PlayerNotFoundException;
import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.StatsType;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings("unused")
public class Cmdstats extends BaseCommand {

    public Cmdstats() {
        this.permission = "hms.stats";
    }

    @Override
    public void run(CommandSender sender, String label, String[] args) {

        if (args.length > 0) {

            boolean compare = false;
            if (args.length > 1 && args[1].equalsIgnoreCase("compare") && sender instanceof Player) compare = true;
            try {
                showStats(sender, hms.getServer().getOfflinePlayer(storage.getUUID(args[0])), compare);
            } catch (PlayerNotFoundException e) {
                sender.sendMessage(Language.getMessagePlaceholders("playerDoesNotExist", true, "%PREFIX%", "Stats"));
            }

        } else {

            if (sender instanceof Player) showStats(sender, (Player) sender, false);
            else sender.sendMessage(Language.getMessage("notAPlayer"));

        }

    }

    private void showStats(final CommandSender sendTo, final OfflinePlayer player, boolean compare) {

        Player compareWith = null;
        if (compare && !sendTo.equals(player)) compareWith = (Player) sendTo;

        final String oldNames = storage.getStatsString(player, StatsType.LAST_NAMES);

        //build the message
        String message = Language.getMessage("commandStatsTop") + "\n";
        message += Language.getMessagePlaceholders("commandStatsShow", false,
                "%PLAYER%", player.getName(),
                "%SKILLGROUP%", storage.getStatsString(player, StatsType.SKILL_GROUP),
                "%SKILLLEVEL%", getIntAndCompare(player, StatsType.SKILL_LEVEL, compareWith),
                "%ONLINETIME%", getIntAndCompare(player, StatsType.TIME_ONLINE, compareWith),
                "%JOINS%", getIntAndCompare(player, StatsType.JOINS, compareWith),
                "%KILLS%", getIntAndCompare(player, StatsType.KILLS, compareWith),
                "%DEATHS%", getIntAndCompare(player, StatsType.DEATHS, compareWith),
                "%KDRATIO%", getDoubleAndCompare(player, StatsType.KD_RATIO, compareWith),
                "%VOTES%", getIntAndCompare(player, StatsType.VOTES, compareWith),
                "%MOBKILLS%", getIntAndCompare(player, StatsType.MOB_KILLS, compareWith),
                "%BLOCKSPLACED%", getIntAndCompare(player, StatsType.BLOCKS_PLACED, compareWith),
                "%BLOCKSBROKEN%", getIntAndCompare(player, StatsType.BLOCKS_BROKEN, compareWith),
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
                    "%PLAYER%", player.getName()) + "\n";
        }

        message += Language.getMessage("commandStatsBottom");
        sendTo.sendMessage(message);

    }

    private String getIntAndCompare(OfflinePlayer player, StatsType type, Player compareWith) {
        String returnString = "";
        int playerVar = storage.getStatsInt(player, type);

        if (compareWith != null) {
            int compareVar = storage.getStatsInt(compareWith, type);
            if (playerVar < compareVar) returnString = ChatColor.GREEN.toString();
            else if (playerVar > compareVar) returnString = ChatColor.RED.toString();
            else returnString = ChatColor.YELLOW.toString();
        }

        if (type == StatsType.TIME_ONLINE) playerVar /= 60;
        return returnString + String.valueOf(playerVar);
    }

    private String getDoubleAndCompare(OfflinePlayer player, StatsType type, Player compareWith) {
        String returnString = "";
        double playerVar = storage.getStatsDouble(player, type);

        if (compareWith != null) {
            double compareVar = storage.getStatsDouble(compareWith, type);
            if (playerVar < compareVar) returnString = ChatColor.GREEN.toString();
            else if (playerVar > compareVar) returnString = ChatColor.RED.toString();
            else returnString = ChatColor.YELLOW.toString();
        }

        return returnString + String.valueOf(playerVar);
    }

}
