package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
import de.halfminer.hmc.module.ModStatsTop;
import de.halfminer.hmc.module.ModuleType;
import de.halfminer.hmc.module.statstop.TopBoard;
import de.halfminer.hms.exceptions.PlayerNotFoundException;
import de.halfminer.hms.handler.storage.DataType;
import de.halfminer.hms.util.HalfminerPlayer;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * - View the leaderboards configured via ModStatsTop
 * - Shows all available leaderboards in a list
 * - By default, shows top 5 of selected board and the players position, plus the next and previous one to the player
 *   - If executing player is not on board, shows last player and his own stat
 *   - Specfify page number to scroll through the board
 */
@SuppressWarnings("unused")
public class Cmdstatstop extends HalfminerCommand {

    private final ModStatsTop statsTop = (ModStatsTop) hmc.getModule(ModuleType.STATS_TOP);
    private List<Pair<UUID, Integer>> topBoardList;

    private List<MessageBuilder> messageQueue;


    public Cmdstatstop() {
        this.permission = "hmc.statstop";
    }

    @Override
    protected void execute() {

        Map<DataType, TopBoard> topBoardMap = statsTop.getBoards();
        if (args.length > 0) {

            DataType type = DataType.getFromString(args[0]);
            if (type == null || !topBoardMap.containsKey(type)) {
                MessageBuilder.create("cmdStatstopDoesntExist", hmc, "Statstop").sendMessage(sender);
                return;
            }

            TopBoard topBoard = topBoardMap.get(type);
            topBoardList = topBoard.getList();
            // since the task will be done asynchronously, first collect message builder and then send them synchronously
            messageQueue = new ArrayList<>();

            scheduler.runTaskAsynchronously(hmc, () -> {
                // paged view
                if (args.length > 1) {

                    int page;
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        sendInvalidPageNumber();
                        return;
                    }

                    int startingAt = (page - 1) * 10;
                    if (page < 1 || startingAt >= topBoardList.size()) {
                        sendInvalidPageNumber();
                        return;
                    }

                    messageQueue.add(MessageBuilder.create("cmdStatstopHeaderPaged", hmc, "Statstop")
                            .addPlaceholderReplace("%BOARDNAME%", topBoard.getName())
                            .addPlaceholderReplace("%PAGE%", String.valueOf(page)));

                    for (int i = startingAt; i < topBoardList.size() && i < startingAt + 10; i++) {
                        addEntryMessageToQueue(i);
                    }

                    if (startingAt + 10 < topBoardList.size()) {
                        messageQueue.add(MessageBuilder.create("cmdStatstopPageNext", hmc)
                                .addPlaceholderReplace("%TYPE%", type.toString())
                                .addPlaceholderReplace("%PAGE%", String.valueOf(page + 1)));
                    }

                } else {

                    messageQueue.add(MessageBuilder.create("cmdStatstopHeader", hmc, "Statstop")
                            .addPlaceholderReplace("%BOARDNAME%", topBoard.getName()));

                    for (int i = 0; i < 5 && i < topBoardList.size(); i++) {
                        addEntryMessageToQueue(i);
                    }

                    if (isPlayer) {
                        HalfminerPlayer hPlayer = storage.getPlayer(player);
                        int indexOnBoard = topBoard.getIndex(hPlayer);
                        if (indexOnBoard > 3) {
                            int startAt = indexOnBoard == 4 ? 5 : indexOnBoard - 1;
                            for (int i = startAt; i <= indexOnBoard + 1 && i < topBoardList.size(); i++) {
                                addEntryMessageToQueue(i);
                            }
                        } else if (indexOnBoard < 0) {
                            // show last player and add unranked position if not on board
                            if (topBoardList.size() > 5) {
                                addEntryMessageToQueue(topBoardList.size() - 1);
                            }
                            messageQueue.add(MessageBuilder.create("cmdStatstopPosition", hmc)
                                    .addPlaceholderReplace("%RANK%", MessageBuilder.returnMessage("cmdStatstopSelfUnranked", hmc))
                                    .addPlaceholderReplace("%SELFPREFIX%", MessageBuilder.returnMessage("cmdStatstopSelfPrefix", hmc))
                                    .addPlaceholderReplace("%PLAYER%", player.getName())
                                    .addPlaceholderReplace("%VALUE%", String.valueOf(hPlayer.getInt(type))));
                        }
                    }

                    if (topBoardList.size() > 5) {
                        messageQueue.add(MessageBuilder.create("cmdStatstopPageInfo", hmc)
                                .addPlaceholderReplace("%TYPE%", type.toString()));
                    }
                }

                // send collected messages synchronously
                scheduler.runTask(hmc, () -> messageQueue.forEach(messageBuilder -> messageBuilder.sendMessage(sender)));
            });

        } else {
            MessageBuilder.create("cmdStatstopListHeader", hmc).sendMessage(sender);
            for (TopBoard topBoard : topBoardMap.values()) {
                MessageBuilder.create("cmdStatstopListEntry", hmc)
                        .addPlaceholderReplace("%NAME%", topBoard.getName())
                        .addPlaceholderReplace("%TYPE%", topBoard.getDataType().toString())
                        .sendMessage(sender);
            }
            MessageBuilder.create("lineSeparator").sendMessage(sender);
        }
    }

    private void addEntryMessageToQueue(int position) {
        Pair<UUID, Integer> uuidIntegerPair = topBoardList.get(position);
        try {
            HalfminerPlayer currentPlayer = storage.getPlayer(uuidIntegerPair.getLeft());
            // check if player at position is himself, if that is the case get the prefix from locale file
            String selfPrefix = "";
            if (isPlayer && currentPlayer.getUniqueId().equals(player.getUniqueId())) {
                selfPrefix = MessageBuilder.returnMessage("cmdStatstopSelfPrefix", hmc);
            }

            messageQueue.add(MessageBuilder.create("cmdStatstopPosition", hmc)
                    .addPlaceholderReplace("%RANK%", String.valueOf(position + 1))
                    .addPlaceholderReplace("%SELFPREFIX%", selfPrefix)
                    .addPlaceholderReplace("%PLAYER%", currentPlayer.getName())
                    .addPlaceholderReplace("%VALUE%", String.valueOf(uuidIntegerPair.getRight())));
        } catch (PlayerNotFoundException ignored) {}
    }

    private void sendInvalidPageNumber() {
        MessageBuilder.create("cmdStatstopInvalidPage", hmc, "Statstop")
                .addPlaceholderReplace("%MAXPAGE%", String.valueOf(((topBoardList.size() - 1) / 10) + 1))
                .sendMessage(sender);
    }
}
