package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
import de.halfminer.hmc.module.ModStatsTop;
import de.halfminer.hmc.module.ModuleDisabledException;
import de.halfminer.hmc.module.ModuleType;
import de.halfminer.hmc.module.statstop.TopBoard;
import de.halfminer.hms.handler.storage.PlayerNotFoundException;
import de.halfminer.hms.handler.storage.DataType;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.util.Message;
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

    private List<Pair<UUID, Integer>> topBoardList;

    private List<Message> messageQueue;


    public Cmdstatstop() {
        this.permission = "hmc.statstop";
    }

    @Override
    protected void execute() throws ModuleDisabledException {

        final ModStatsTop statsTop = (ModStatsTop) hmc.getModule(ModuleType.STATS_TOP);
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
                    messageQueue.add(Message.create(headerKey, hmc, "Statstop")
                            .addPlaceholder("%PLAYER%", toLookup.getName()));

                    for (TopBoard board : topBoardMap.values()) {

                        int index = board.getIndex(toLookup) + 1;
                        String indexStr;
                        if (index > 0) {
                            indexStr = Message.create("cmdStatstopPlayerEntryRank", hmc)
                                    .addPlaceholder("%RANK%", index)
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
                            indexCompareStr = Message.create("cmdStatstopPlayerEntryRank", hmc)
                                    .addPlaceholder("%RANK%", color.toString() + indexCompare)
                                    .returnMessage();
                        }

                        messageQueue.add(Message.create(messageKey, hmc)
                                .addPlaceholder("%BOARD%", board.getName())
                                .addPlaceholder("%RANK%", indexStr)
                                .addPlaceholder("%RANKCOMP%", indexCompareStr));
                    }

                    messageQueue.add(Message.create("cmdStatstopPlayerFooter", hmc)
                            .addPlaceholder("%PLAYER%", toLookup.getName()));

                    sendMessageQueue();
                });

                return;
            } // -- end player scoreboard lookup and comparison

            // we are trying to find the specified board
            DataType matchingType = null;
            String boardString = args[0].toLowerCase();
            for (DataType type : topBoardMap.keySet()) {
                if (type.toString().startsWith(boardString)) {
                    matchingType = type;
                    break;
                }
            }

            DataType type = matchingType;
            if (type == null || !topBoardMap.containsKey(type)) {
                Message.create("cmdStatstopDoesntExist", hmc, "Statstop").send(sender);
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

                    messageQueue.add(Message.create("cmdStatstopHeaderPaged", hmc, "Statstop")
                            .addPlaceholder("%BOARDNAME%", topBoard.getName())
                            .addPlaceholder("%PAGE%", page));

                    for (int i = startingAt; i < topBoardList.size() && i < startingAt + 10; i++) {
                        addEntryMessageToQueue(i);
                    }

                    if (startingAt + 10 < topBoardList.size()) {
                        messageQueue.add(Message.create("cmdStatstopPageNext", hmc)
                                .addPlaceholder("%TYPE%", type.toString())
                                .addPlaceholder("%PAGE%", page + 1));
                    }

                } else { // default view (top 5 + own position)

                    messageQueue.add(Message.create("cmdStatstopHeader", hmc, "Statstop")
                            .addPlaceholder("%BOARDNAME%", topBoard.getName()));

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
                            messageQueue.add(Message.create("cmdStatstopPosition", hmc)
                                    .addPlaceholder("%RANK%", getUnrankedString())
                                    .addPlaceholder("%SELFPREFIX%", Message.returnMessage("cmdStatstopSelfPrefix", hmc))
                                    .addPlaceholder("%PLAYER%", player.getName())
                                    .addPlaceholder("%VALUE%", hPlayer.getInt(type)));
                        }
                    }

                    if (topBoardList.size() > 5) {
                        messageQueue.add(Message.create("cmdStatstopPageInfo", hmc)
                                .addPlaceholder("%TYPE%", type.toString()));
                    }
                }

                sendMessageQueue();
            });

        } else { // board list

            Message.create("cmdStatstopListHeader", hmc).send(sender);
            for (TopBoard topBoard : topBoardMap.values()) {
                sendListMessage(topBoard.getName(), topBoard.getDataType().toString());
            }

            // send command for /statstop player
            sendListMessage(Message.returnMessage("cmdStatstopListEntryPlayer", hmc), "player");
            Message.create("lineSeparator").send(sender);
        }
    }

    private void addEntryMessageToQueue(int position) {
        Pair<UUID, Integer> uuidIntegerPair = topBoardList.get(position);
        try {
            HalfminerPlayer currentPlayer = storage.getPlayer(uuidIntegerPair.getLeft());
            // check if player at position is himself, if that is the case get the prefix from locale file
            String selfPrefix = "";
            if (isPlayer && currentPlayer.getUniqueId().equals(player.getUniqueId())) {
                selfPrefix = Message.returnMessage("cmdStatstopSelfPrefix", hmc);
            }

            messageQueue.add(Message.create("cmdStatstopPosition", hmc)
                    .addPlaceholder("%RANK%", position + 1)
                    .addPlaceholder("%SELFPREFIX%", selfPrefix)
                    .addPlaceholder("%PLAYER%", currentPlayer.getName())
                    .addPlaceholder("%VALUE%", uuidIntegerPair.getRight()));
        } catch (PlayerNotFoundException ignored) {}
    }

    private String getUnrankedString() {
        return Message.returnMessage("cmdStatstopUnranked", hmc);
    }

    private void sendInvalidPageNumber() {
        Message.create("cmdStatstopInvalidPage", hmc, "Statstop")
                .addPlaceholder("%MAXPAGE%", ((topBoardList.size() - 1) / 10) + 1)
                .send(sender);
    }

    private void sendListMessage(String boardName, String boardString) {
        Message.create("cmdStatstopListEntry", hmc)
                .addPlaceholder("%NAME%", boardName)
                .addPlaceholder("%TYPE%", boardString)
                .send(sender);
    }

    private void sendMessageQueue() {
        scheduler.runTask(hmc, () -> messageQueue.forEach(messageBuilder -> messageBuilder.send(sender)));
    }
}
