package de.halfminer.hml.cmd;

import de.halfminer.hml.WorldGuardHelper;
import de.halfminer.hml.land.Land;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.handler.storage.PlayerNotFoundException;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.configuration.ConfigurationSection;

public class Cmdhml extends LandCommand {

    private static final String STORAGE_FREE_LANDS = ".freetotal";


    public Cmdhml() {
        super("hml");
    }

    @Override
    protected void execute() {

        if (args.length < 1) {
            showUsage();
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload":

                hml.reload();
                MessageBuilder.create("pluginReloaded", "Land")
                        .addPlaceholderReplace("%PLUGINNAME%", hml.getName())
                        .sendMessage(sender);
                break;
            case "save":

                hml.saveLandStorage();
                MessageBuilder.create("cmdHmlSave", hml).sendMessage(sender);
                break;
            case "status":

                MessageBuilder.create("cmdHmlStatus", hml)
                        .addPlaceholderReplace("%TOTALLANDS%", String.valueOf(board.getOwnedLandSet().size()))
                        .sendMessage(sender);
                break;
            case "forcewgrefresh":

                MessageBuilder.create("cmdHmlRefreshStarted", hml).sendMessage(sender);

                WorldGuardHelper wgh = hml.getWorldGuardHelper();
                for (Land land : board.getOwnedLandSet()) {
                    wgh.updateRegionOfLand(land, true);
                }

                MessageBuilder.create("cmdHmlRefreshDone", hml).sendMessage(sender);
                break;
            case "free":

                if (args.length < 2) {
                    showUsage();
                    return;
                }

                HalfminerPlayer toEdit;
                try {
                    toEdit = hms.getStorageHandler().getPlayer(args[1]);
                } catch (PlayerNotFoundException e) {
                    sendPlayerNotFoundMessage();
                    return;
                }

                int setTo = -1;
                if (args.length > 2) {
                    try {
                        setTo = Integer.parseInt(args[2]);
                    } catch (NumberFormatException ignored) {}
                }

                ConfigurationSection landStorageRoot = hml.getLandStorage().getRootSection();
                String key = toEdit.getUniqueId().toString() + STORAGE_FREE_LANDS;
                int currentFree = landStorageRoot.getInt(key, 0);

                if (setTo >= 0) {
                    landStorageRoot.set(key, setTo > 0 ? setTo : null);
                    hml.getLogger().info("Set free lands for " + toEdit.getName() + " from "
                            + currentFree + " to " + setTo);
                }

                MessageBuilder.create(setTo >= 0 ? "cmdHmlFreeSet" : "cmdHmlFreeShow", hml)
                        .addPlaceholderReplace("%PLAYER%", toEdit.getName())
                        .addPlaceholderReplace("%CURRENTFREE%", String.valueOf(currentFree))
                        .addPlaceholderReplace("%SETFREE%", String.valueOf(setTo))
                        .sendMessage(sender);

                break;
            default:
                showUsage();
        }
    }

    private void showUsage() {
        MessageBuilder.create("cmdHmlUsage", hml)
                .addPlaceholderReplace("%VERSION%", hml.getDescription().getVersion())
                .sendMessage(sender);
    }
}
