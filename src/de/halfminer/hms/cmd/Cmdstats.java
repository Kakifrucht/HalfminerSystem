package de.halfminer.hms.cmd;

import de.halfminer.hms.modules.ModStorage;
import de.halfminer.hms.util.Language;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

@SuppressWarnings("unused")
public class Cmdstats extends BaseCommand {

    private final ModStorage storage = hms.getModStorage();

    public Cmdstats() {
        this.permission = "hms.stats";
    }

    @Override
    public void run(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length > 0) {
            OfflinePlayer player = null;
            String uid = storage.getString("uid." + args[0].toLowerCase());
            if (uid.length() > 0) player = hms.getServer().getOfflinePlayer(UUID.fromString(uid));
            if (player != null) showStats(sender, player);
            else sender.sendMessage(Language.getMessagePlaceholderReplace("commandPlayerNotFound", true, "%PREFIX%", "Stats"));
        } else {
            if (sender instanceof Player) showStats(sender, (Player) sender);
            else sender.sendMessage(Language.getMessage("notAPlayer"));
        }

    }

    private void showStats(final CommandSender sendTo, final OfflinePlayer toShow) {
        final String skillGroup = storage.getPlayerString(toShow, "skillGroup");
        final int skillLevel = storage.getPlayerInt(toShow, "skillLevel");
        final int timeOnline = storage.getPlayerInt(toShow, "timeOnline") / 60;
        final int joins = storage.getPlayerInt(toShow, "joins");
        final int kills = storage.getPlayerInt(toShow, "kills");
        final int deaths = storage.getPlayerInt(toShow, "deaths");
        final int votes = storage.getPlayerInt(toShow, "votes");
        final int mobKills = storage.getPlayerInt(toShow, "mobKills");
        final int blocksPlaced = storage.getPlayerInt(toShow, "blocksPlaced");
        final int blocksBroken = storage.getPlayerInt(toShow, "blocksBroken");
        final String oldNames = storage.getPlayerString(toShow, "lastNames");
        //Will iterate quite often, so better async it
        hms.getServer().getScheduler().runTaskAsynchronously(hms, new Runnable() {
            @Override
            public void run() {
                String message = Language.getMessage("commandStatsTop") + "\n";
                message += Language.getMessagePlaceholderReplace("commandStatsShow", false, "%PLAYER%", toShow.getName(),
                        "%SKILLGROUP%", skillGroup, "%SKILLLEVEL%", String.valueOf(skillLevel),
                        "%ONLINETIME%", String.valueOf(timeOnline), "%JOINS%", String.valueOf(joins),
                        "%KILLS%", String.valueOf(kills), "%DEATHS%", String.valueOf(deaths),
                        "%VOTES%", String.valueOf(votes), "%MOBKILLS%", String.valueOf(mobKills),
                        "%BLOCKSPLACED%", String.valueOf(blocksPlaced), "%BLOCKSBROKEN%", String.valueOf(blocksBroken),
                        "%OLDNAMES%", oldNames) + "\n";
                if (oldNames.length() > 0) message += Language.getMessagePlaceholderReplace("commandStatsOldnames", false,
                        "%OLDNAMES%", oldNames) + "\n";
                if (sendTo.getName().equals(toShow.getName())) message += Language.getMessage("commandStatsShowotherStats") + "\n";
                message += Language.getMessage("commandStatsBottom");

                sendTo.sendMessage(message);
            }
        });

    }

}
