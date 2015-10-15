package de.halfminer.hms.cmd;

import de.halfminer.hms.modules.ModStorage;
import de.halfminer.hms.util.Language;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings("unused")
public class Cmdstats extends BaseCommand {

    private final ModStorage storage = hms.getModStorage();

    public Cmdstats() {
        this.permission = "hms.stats";
    }

    @Override
    public void run(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length > 0) {
            String uid = storage.getString("uid." + args[0].toLowerCase());
            if (uid.length() > 0) showStats(sender, uid);
            else
                sender.sendMessage(Language.getMessagePlaceholderReplace("commandPlayerNotFound", true, "%PREFIX%", "Stats"));
        } else {
            if (sender instanceof Player) showStats(sender, ((Player) sender).getUniqueId().toString());
            if (sender instanceof Player) showStats(sender, ((Player) sender).getUniqueId().toString());
            else sender.sendMessage(Language.getMessage("notAPlayer"));
        }

    }

    private void showStats(final CommandSender sendTo, final String uid) {
        final String playerName = storage.getString(uid + ".lastName");
        final String skillGroup = storage.getString(uid + ".skillGroup");
        final int skillLevel = storage.getInt(uid + ".skillLevel");
        final int timeOnline = storage.getInt(uid + ".timeOnline") / 60;
        final int joins = storage.getInt(uid + ".joins");
        final int kills = storage.getInt(uid + ".kills");
        final int deaths = storage.getInt(uid + ".deaths");
        final int votes = storage.getInt(uid + ".votes");
        final int mobKills = storage.getInt(uid + ".mobKills");
        final int blocksPlaced = storage.getInt(uid + ".blocksPlaced");
        final int blocksBroken = storage.getInt(uid + ".blocksBroken");
        final String oldNames = storage.getString(uid + ".lastNames");
        //Will iterate quite often, so better async it
        hms.getServer().getScheduler().runTaskAsynchronously(hms, new Runnable() {
            @Override
            public void run() {
                String message = Language.getMessage("commandStatsTop") + "\n";
                message += Language.getMessagePlaceholderReplace("commandStatsShow", false, "%PLAYER%", playerName,
                        "%SKILLGROUP%", skillGroup, "%SKILLLEVEL%", String.valueOf(skillLevel),
                        "%ONLINETIME%", String.valueOf(timeOnline), "%JOINS%", String.valueOf(joins),
                        "%KILLS%", String.valueOf(kills), "%DEATHS%", String.valueOf(deaths),
                        "%VOTES%", String.valueOf(votes), "%MOBKILLS%", String.valueOf(mobKills),
                        "%BLOCKSPLACED%", String.valueOf(blocksPlaced), "%BLOCKSBROKEN%", String.valueOf(blocksBroken),
                        "%OLDNAMES%", oldNames) + "\n";
                if (oldNames.length() > 0)
                    message += Language.getMessagePlaceholderReplace("commandStatsOldnames", false,
                            "%OLDNAMES%", oldNames) + "\n";
                if (sendTo.getName().equals(playerName))
                    message += Language.getMessage("commandStatsShowotherStats") + "\n";
                message += Language.getMessage("commandStatsBottom");

                sendTo.sendMessage(message);
            }
        });

    }

}
