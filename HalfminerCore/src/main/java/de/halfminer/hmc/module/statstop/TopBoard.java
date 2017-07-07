package de.halfminer.hmc.module.statstop;

import de.halfminer.hms.handler.storage.DataType;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.util.Pair;

import java.util.List;
import java.util.UUID;

/**
 * Toplist managed by {@link de.halfminer.hmc.module.ModStatsTop}, contains a sorted list with players stats.
 */
public interface TopBoard {

    String getName();

    DataType getDataType();

    List<Pair<UUID, Integer>> getList();

    /**
     * Update the board with new settings, will also trim the board.
     *
     * @param name               clean and readable name for this board
     * @param maxEntriesPerBoard new value for how many entries are allowed on it
     * @param minimumValue       new value for lowest amount on board
     * @param maximumValue       new value for highest amount on board
     * @return true if settings were changed in a way a refill of all players should be done, for example
     * due to increasing of maxEntriesOnBoard
     */
    boolean updateConfig(String name, int maxEntriesPerBoard, int minimumValue, int maximumValue);

    int getIndex(HalfminerPlayer player);

    void insertOrUpdatePlayer(HalfminerPlayer toUpdate);

    void insertOrUpdatePlayers(List<HalfminerPlayer> toUpdate);

    /**
     * Insert or update players on board.
     *
     * @param toUpdate  list of players to insert/update
     * @param clearFill will clear current board if true, increases performance when readding every player
     */
    void insertOrUpdatePlayers(List<HalfminerPlayer> toUpdate, boolean clearFill);
}
