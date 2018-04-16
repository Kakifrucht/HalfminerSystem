package de.halfminer.hml.cmd;

import de.halfminer.hml.WorldGuardHelper;
import de.halfminer.hml.land.Land;
import de.halfminer.hms.util.MessageBuilder;

public class Cmdhml extends LandCommand {


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
