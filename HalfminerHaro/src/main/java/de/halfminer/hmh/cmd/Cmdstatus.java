package de.halfminer.hmh.cmd;

import de.halfminer.hmh.data.HaroPlayer;
import de.halfminer.hms.util.Message;
import org.bukkit.ChatColor;

import java.util.List;

/**
 * - Prints current game information (is the game running/over?).
 * - Shows all added players, their online/elimination status, and their remaining time, if not yet eliminated.
 */
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

        Message.create("cmdStatusHeader", hmh)
                .addPlaceholder("GAMEISRUNNING", Message.returnMessage(gameStatusMessageKey, hmh, false))
                .send(sender);

        if (addedPlayers.isEmpty()) {
            Message.create("cmdStatusNoPlayersAdded", hmh).togglePrefix().send(sender);
        } else {

            String playerSpacer = Message.returnMessage("cmdStatusPlayerListSpacer", hmh, false);
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
                    String timeLeftString = Message.create("cmdStatusPlayerListTime", hmh)
                            .togglePrefix()
                            .addPlaceholder("TIMELEFT", addedPlayer.getTimeLeftSeconds())
                            .returnMessage();
                    playerListBuilder.append(timeLeftString);
                }

                playerListBuilder.append(playerSpacer);
            }

            String playerListString = playerListBuilder.substring(0, playerListBuilder.length() - playerSpacer.length());
            Message.create("cmdStatusPlayerList", hmh)
                    .togglePrefix()
                    .addPlaceholder("PLAYERLIST", playerListString)
                    .send(sender);

            Message.create("cmdStatusLegend", hmh)
                    .togglePrefix()
                    .send(sender);
        }
    }
}
