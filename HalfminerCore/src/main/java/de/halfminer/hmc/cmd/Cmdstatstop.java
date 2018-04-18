package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
import de.halfminer.hmc.module.ModStatsTop;
import de.halfminer.hmc.module.ModuleType;
import de.halfminer.hmc.module.statstop.TopBoard;
import de.halfminer.hms.handler.storage.PlayerNotFoundException;
import de.halfminer.hms.handler.storage.DataType;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Pair;
import org.bukkit.ChatColor;

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
 * - View position on every board at once
 *   - Specify player to compare ranks easily
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

            // since the task will be done asynchronously, first collect message builder and then send them synchronously
            messageQueue = new ArrayList<>();

            // player scoreboard lookup and comparison
            if (args[0].equalsIgnoreCase("player")) {

                HalfminerPlayer thisPlayer = isPlayer ? storage.getPlayer(player) : null;
                HalfminerPlayer toLookup;
                if (args.length > 1) {
                    // compare with other player
                    try {
                        toLookup = storage.getPlayer(args[1]);
                    } catch (PlayerNotFoundException e) {
                        e.sendNotFoundMessage(sender, "Statstop");
                        return;
                    }
                } else if (isPlayer) {
                    toLookup = thisPlayer;
                } else {
                    sendNotAPlayerMessage("Statstop");
                    return;
                }

                boolean doCompare = isPlayer && !toLookup.equals(thisPlayer);

                scheduler.runTaskAsynchronously(hmc, () -> {

                    String headerKey = doCompare ? "cmdStatstopPlayerHeaderCompare" : "cmdStatstopPlayerHeader";
                    messageQueue.add(MessageBuilder.create(headerKey, hmc, "Statstop")
                            .addPlaceholderReplace("%PLAYER%", toLookup.getName()));

                    for (TopBoard board : topBoardMap.values()) {

                        int index = board.getIndex(toLookup) + 1;
                        String indexStr;
                        if (index > 0) {
                            indexStr = MessageBuilder.create("cmdStatstopPlayerEntryRank", hmc)
                                    .addPlaceholderReplace("%RANK%", String.valueOf(index))
                                    .returnMessage();
                        } else {
                            indexStr = getUnrankedString();
                            index = Integer.MAX_VALUE; // to compare easily
                        }

                        int indexCompare = Integer.MIN_VALUE;
                        String indexCompareStr = "";
                        if (doCompare) {
                            indexCompare = board.getIndex(thisPlayer) + 1;
                        }

                        String messageKey = "cmdStatstopPlayerEntry";
                        // comparison only takes place if we are not unranked
                        if (doCompare && indexCompare > 0) {
                            messageKey += "Compare";

                            // green if we are ranked higher, else red
                            ChatColor color = indexCompare < index ? ChatColor.GREEN : ChatColor.RED;
                            indexCompareStr = MessageBuilder.create("cmdStatstopPlayerEntryRank", hmc)
                                    .addPlaceholderReplace("%RANK%", color.toString() + indexCompare)
                                    .returnMessage();
                        }

                        messageQueue.add(MessageBuilder.create(messageKey, hmc)
                                .addPlaceholderReplace("%BOARD%", board.getName())
                                .addPlaceholderReplace("%RANK%", indexStr)
                                .addPlaceholderReplace("%RANKCOMP%", indexCompareStr));
                    }

                    messageQueue.add(MessageBuilder.create("cmdStatstopPlayerFooter", hmc)
                            .addPlaceholderReplace("%PLAYER%", toLookup.getName()));

                    sendMessageQueue();
                });

                return;
            } // -- end player scoreboard lookup and comparison

            // we are trying to get the specified board
            DataType type = DataType.getFromString(args[0]);
            if (type == null || !topBoardMap.containsKey(type)) {
                MessageBuilder.create("cmdStatstopDoesntExist", hmc, "Statstop").sendMessage(sender);
                return;
            }

            TopBoard topBoard = topBoardMap.get(type);
            topBoardList = topBoard.getList();

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

                } else { // default view (top 5 + own position)

                    messageQueue.add(MessageBuilder.create("cmdStatstopHeader", hmc, "Statstop")
                            .addPlaceholderReplace("%BOARDNAME%", topBoard.getName()));

                    for (int i = 0; i < 5 && i < topBoardList.size(); i++) {
                        addEntryMessageToQueue(i);
                    }

                    if (isPlayer) {
                        HalfminerPlayer hPlayer = storage.getPlayer(player);
                        int indexOnBoard = topBoard.getIndex(hPlayer);
                        if (indexOnBoard > 3) {
                            for (int i = Math.max(5, indexOnBoard - 1); i <= indexOnBoard + 1 && i < topBoardList.size(); i++) {
                                addEntryMessageToQueue(i);
                            }
                        } else if (indexOnBoard < 0) {
                            // show last player and add unranked position if not on board
                            if (topBoardList.size() > 5) {
                                addEntryMessageToQueue(topBoardList.size() - 1);
                            }
                            messageQueue.add(MessageBuilder.create("cmdStatstopPosition", hmc)
                                    .addPlaceholderReplace("%RANK%", getUnrankedString())
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

                sendMessageQueue();
            });

        } else { // board list

            MessageBuilder.create("cmdStatstopListHeader", hmc).sendMessage(sender);
            for (TopBoard topBoard : topBoardMap.values()) {
                sendListMessage(topBoard.getName(), topBoard.getDataType().toString());
            }

            // send command for /statstop player
            sendListMessage(MessageBuilder.returnMessage("cmdStatstopListEntryPlayer", hmc), "player");
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

    private String getUnrankedString() {
        return MessageBuilder.returnMessage("cmdStatstopUnranked", hmc);
    }

    private void sendInvalidPageNumber() {
        MessageBuilder.create("cmdStatstopInvalidPage", hmc, "Statstop")
                .addPlaceholderReplace("%MAXPAGE%", String.valueOf(((topBoardList.size() - 1) / 10) + 1))
                .sendMessage(sender);
    }

    private void sendListMessage(String boardName, String boardString) {
        MessageBuilder.create("cmdStatstopListEntry", hmc)
                .addPlaceholderReplace("%NAME%", boardName)
                .addPlaceholderReplace("%TYPE%", boardString)
                .sendMessage(sender);
    }

    private void sendMessageQueue() {
        scheduler.runTask(hmc, () -> messageQueue.forEach(messageBuilder -> messageBuilder.sendMessage(sender)));
    }
}
