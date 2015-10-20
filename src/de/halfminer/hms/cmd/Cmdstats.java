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
            else {
                sender.sendMessage(Language.getMessagePlaceholderReplace("playerDoesNotExist", true, "%PREFIX%", "Stats"));
            }
        } else {
            if (sender instanceof Player) showStats(sender, ((Player) sender).getUniqueId().toString());
            else sender.sendMessage(Language.getMessage("notAPlayer"));
        }

    }

    private void showStats(final CommandSender sendTo, final String uid) {

        //collect vars
        final String playerName = storage.getString(uid + ".lastname");
        final String skillGroup = storage.getString(uid + ".skillgroup");
        final int skillLevel = storage.getInt(uid + ".skilllevel");
        final int timeOnline = storage.getInt(uid + ".timeonline") / 60;
        final int joins = storage.getInt(uid + ".joins");
        final int kills = storage.getInt(uid + ".kills");
        final int deaths = storage.getInt(uid + ".deaths");
        final double kdratio = storage.getDouble(uid + ".kdratio");
        final int votes = storage.getInt(uid + ".votes");
        final int mobKills = storage.getInt(uid + ".mobkills");
        final int blocksPlaced = storage.getInt(uid + ".blocksplaced");
        final int blocksBroken = storage.getInt(uid + ".blocksbroken");
        final String oldNames = storage.getString(uid + ".lastnames");

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

        if (sendTo.getName().equals(playerName))
            message += Language.getMessage("commandStatsShowotherStats") + "\n";

        message += Language.getMessage("commandStatsBottom");
        sendTo.sendMessage(message);

    }

}
