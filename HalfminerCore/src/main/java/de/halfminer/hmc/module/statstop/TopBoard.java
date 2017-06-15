package de.halfminer.hmc.module.statstop;

import de.halfminer.hms.handler.storage.DataType;
import de.halfminer.hms.util.HalfminerPlayer;
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
     * @param name clean and readable name for this board
     * @param maxEntriesPerBoard new value for how many entries are allowed on it
     * @param minimumValue new value for lowest amount on board
     * @return true if maxEntriesPerBoard value was increased or minimumValue value was reduced to indicate that it
     *         should be tried to refill the board
     */
    boolean updateConfig(String name, int maxEntriesPerBoard, int minimumValue);

    int getIndex(HalfminerPlayer player);

    void insertOrUpdatePlayer(HalfminerPlayer toUpdate);

    void insertOrUpdatePlayers(List<HalfminerPlayer> toUpdate);
}
