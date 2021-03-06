package de.halfminer.hmc.module;

import de.halfminer.hmc.module.statstop.DefaultTopBoard;
import de.halfminer.hmc.module.statstop.TopBoard;
import de.halfminer.hms.handler.storage.DataType;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.util.Message;
import de.halfminer.hms.util.StringArgumentSeparator;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * - Manages ordered player stat scoreboards
 *   - Define which stats to track via config (integer based)
 *   - Define max board size
 *   - Define min and max amount to be on on board
 * - Optimized for performance, does updates asynchronously
 */
@SuppressWarnings("unused")
public class ModStatsTop extends HalfminerModule implements Listener {

    private Map<DataType, TopBoard> boards;

    private BukkitTask refreshTask;


    @EventHandler
    public void onDisconnectRefresh(PlayerQuitEvent e) {
        HalfminerPlayer toUpdate = storage.getPlayer(e.getPlayer());
        scheduler.runTaskAsynchronously(hmc, () -> boards.values().forEach(board -> board.insertOrUpdatePlayer(toUpdate)));
    }

    public Map<DataType, TopBoard> getBoards() {
        return boards;
    }

    @Override
    public void loadConfig() {

        int maxEntriesPerBoard = Math.max(hmc.getConfig().getInt("statsTop.maxPerBoard", 10), 1);

        Map<DataType, TopBoard> boardsOld = boards;
        boards = new HashMap<>();

        Set<TopBoard> insertAllPlayersTo = new HashSet<>();
        for (String line : hmc.getConfig().getStringList("statsTop.trackedStats")) {
            StringArgumentSeparator argumentSeparator = new StringArgumentSeparator(line, ',');

            if (!argumentSeparator.meetsLength(3)) {
                Message.create("modStatsTopInvalidLine", hmc)
                        .addPlaceholder("%LINE%", line)
                        .log(Level.WARNING);
                continue;
            }

            String name = argumentSeparator.getArgument(0);
            DataType type = DataType.getFromString(argumentSeparator.getArgument(1));
            int minimumValue = argumentSeparator.getArgumentIntMinimum(2, 1);
            int maximumValue = Integer.MAX_VALUE;

            if (type == null) {
                Message.create("modStatsTopInvalidType", hmc)
                        .addPlaceholder("%TYPE%", argumentSeparator.getArgument(1))
                        .log(Level.WARNING);
                continue;
            }

            // don't allow the same type twice
            if (boards.containsKey(type)) {
                Message.create("modStatsTopTypeAlreadyUsed", hmc)
                        .addPlaceholder("%TYPE%", argumentSeparator.getArgument(1))
                        .log(Level.WARNING);
                continue;
            }

            if (argumentSeparator.meetsLength(4)) {
                maximumValue = argumentSeparator.getArgumentIntMinimum(3, minimumValue);
            }

            TopBoard board;
            if (boardsOld != null && boardsOld.containsKey(type)) {
                board = boardsOld.get(type);

                boolean needsInsert = board.updateConfig(name, maxEntriesPerBoard, minimumValue, maximumValue);
                if (needsInsert) {
                    insertAllPlayersTo.add(board);
                }
            } else {
                board = new DefaultTopBoard(type, name, maxEntriesPerBoard, minimumValue, maximumValue);
                insertAllPlayersTo.add(board);
            }
            boards.put(type, board);
        }

        if (!insertAllPlayersTo.isEmpty()) {
            List<HalfminerPlayer> allPlayers = storage.getAllPlayers();
            insertAllPlayersTo.forEach(board -> board.insertOrUpdatePlayers(allPlayers, true));
        }

        if (refreshTask != null) {
            refreshTask.cancel();
        }

        long intervalSeconds = hmc.getConfig().getLong("statsTop.refreshIntervalSeconds", 180L);
        refreshTask = scheduler.runTaskTimer(hmc, () -> {

            List<HalfminerPlayer> players = server.getOnlinePlayers()
                    .stream()
                    .map(storage::getPlayer)
                    .collect(Collectors.toList());

            // do actual update asynchronously
            new Thread(() -> boards.values().forEach(board -> board.insertOrUpdatePlayers(players))).start();

        }, intervalSeconds * 20, intervalSeconds * 20);
    }
}
