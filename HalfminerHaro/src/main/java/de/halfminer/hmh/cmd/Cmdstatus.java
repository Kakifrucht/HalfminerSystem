package de.halfminer.hmh.cmd;

import de.halfminer.hmh.data.HaroPlayer;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.ChatColor;

import java.util.List;

public class Cmdstatus extends HaroCommand {

    public Cmdstatus() {
        super("status");
    }

    @Override
    protected void execute() {

        List<HaroPlayer> addedPlayers = haroStorage.getAddedPlayers(false);
        boolean isGameRunning = haroStorage.isGameRunning();

        String gameStatusMessageKey = "cmdStatusHeaderIs";
        if (isGameRunning) {
            if (haroStorage.isGameOver()) {
                gameStatusMessageKey += "Over";
            } else {
                gameStatusMessageKey += "Running";
            }
        } else {
            gameStatusMessageKey += "NotRunning";
        }

        MessageBuilder.create("cmdStatusHeader", hmh)
                .addPlaceholder("GAMEISRUNNING", MessageBuilder.returnMessage(gameStatusMessageKey, hmh, false))
                .sendMessage(sender);

        if (addedPlayers.isEmpty()) {
            MessageBuilder.create("cmdStatusNoPlayersAdded", hmh).togglePrefix().sendMessage(sender);
        } else {

            String playerSpacer = MessageBuilder.returnMessage("cmdStatusPlayerListSpacer", hmh, false);
            StringBuilder playerListBuilder = new StringBuilder();
            for (HaroPlayer addedPlayer : addedPlayers) {
                ChatColor color;
                if (addedPlayer.isOnline()) {
                    color = ChatColor.GREEN;
                } else if (addedPlayer.isEliminated()) {
                    color = ChatColor.RED;
                } else {
                    color  = ChatColor.YELLOW;
                }

                playerListBuilder
                        .append(color)
                        .append(addedPlayer.getName());

                if (isGameRunning && !addedPlayer.isEliminated()) {
                    String timeLeftString = MessageBuilder.create("cmdStatusPlayerListTime", hmh)
                            .togglePrefix()
                            .addPlaceholder("TIMELEFT", addedPlayer.getTimeLeftSeconds())
                            .returnMessage();
                    playerListBuilder.append(timeLeftString);
                }

                playerListBuilder.append(playerSpacer);
            }

            String playerListString = playerListBuilder.substring(0, playerListBuilder.length() - playerSpacer.length());
            MessageBuilder.create("cmdStatusPlayerList", hmh)
                    .togglePrefix()
                    .addPlaceholder("PLAYERLIST", playerListString)
                    .sendMessage(sender);

            MessageBuilder.create("cmdStatusLegend", hmh)
                    .togglePrefix()
                    .sendMessage(sender);
        }
    }
}
