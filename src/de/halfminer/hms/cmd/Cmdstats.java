package de.halfminer.hms.cmd;

import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.StatsType;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

@SuppressWarnings("unused")
public class Cmdstats extends BaseCommand {

    public Cmdstats() {
        this.permission = "hms.stats";
    }

    @Override
    public void run(CommandSender sender, String label, String[] args) {

        if (args.length > 0) {
            String uid = storage.getString("uid." + args[0].toLowerCase());
            OfflinePlayer player = hms.getServer().getOfflinePlayer(UUID.fromString(uid));
            if (player != null) showStats(sender, player);
            else {
                sender.sendMessage(Language.getMessagePlaceholderReplace("playerDoesNotExist", true, "%PREFIX%", "Stats"));
            }
        } else {
            if (sender instanceof Player) showStats(sender, (Player) sender);
            else sender.sendMessage(Language.getMessage("notAPlayer"));
        }

    }

    private void showStats(final CommandSender sendTo, final OfflinePlayer player) {

        //collect vars
        final String playerName = storage.getStatsString(player, StatsType.LAST_NAME);
        final String skillGroup = storage.getStatsString(player, StatsType.SKILL_GROUP);
        final int skillLevel = storage.getStatsInt(player, StatsType.SKILL_LEVEL);
        final int timeOnline = storage.getStatsInt(player, StatsType.TIME_ONLINE) / 60;
        final int joins = storage.getStatsInt(player, StatsType.JOINS);
        final int kills = storage.getStatsInt(player, StatsType.KILLS);
        final int deaths = storage.getStatsInt(player, StatsType.DEATHS);
        final double kdratio = storage.getStatsDouble(player, StatsType.KD_RATIO);
        final int votes = storage.getStatsInt(player, StatsType.VOTES);
        final int mobKills = storage.getStatsInt(player, StatsType.MOB_KILLS);
        final int blocksPlaced = storage.getStatsInt(player, StatsType.BLOCKS_PLACED);
        final int blocksBroken = storage.getStatsInt(player, StatsType.BLOCKS_BROKEN);
        final String oldNames = storage.getStatsString(player, StatsType.LAST_NAMES);

        //build the message
        String message = Language.getMessage("commandStatsTop") + "\n";
        message += Language.getMessagePlaceholderReplace("commandStatsShow", false, "%PLAYER%", playerName,
                "%SKILLGROUP%", skillGroup, "%SKILLLEVEL%", String.valueOf(skillLevel),
                "%ONLINETIME%", String.valueOf(timeOnline), "%JOINS%", String.valueOf(joins),
                "%KILLS%", String.valueOf(kills), "%DEATHS%", String.valueOf(deaths), "%KDRATIO%", String.valueOf(kdratio),
                "%VOTES%", String.valueOf(votes), "%MOBKILLS%", String.valueOf(mobKills),
                "%BLOCKSPLACED%", String.valueOf(blocksPlaced), "%BLOCKSBROKEN%", String.valueOf(blocksBroken),
                "%OLDNAMES%", oldNames) + "\n";

        if (oldNames.length() > 0)
            message += Language.getMessagePlaceholderReplace("commandStatsOldnames", false,
                    "%OLDNAMES%", oldNames) + "\n";

        if (sendTo.equals(player))
            message += Language.getMessage("commandStatsShowotherStats") + "\n";

        message += Language.getMessage("commandStatsBottom");
        sendTo.sendMessage(message);

    }

}
