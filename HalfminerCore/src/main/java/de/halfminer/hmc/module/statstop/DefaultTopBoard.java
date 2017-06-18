package de.halfminer.hmc.module.statstop;

import de.halfminer.hms.handler.storage.DataType;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Default implementation of {@link TopBoard}.
 */
public class DefaultTopBoard implements TopBoard {

    private final DataType dataType;
    private final List<Pair<UUID, Integer>> board;

    private boolean hasInitialLoad = false;

    // config
    private String name;
    private int maxEntriesPerBoard;
    private int minimumValue;
    private int maximumValue;


    public DefaultTopBoard(DataType dataType, String name, int maxEntriesPerBoard, int minimumValue, int maximumValue) {
        this.dataType = dataType;
        board = Collections.synchronizedList(new ArrayList<>(maxEntriesPerBoard));
        updateConfig(name, maxEntriesPerBoard, minimumValue, maximumValue);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public DataType getDataType() {
        return dataType;
    }

    @Override
    public List<Pair<UUID, Integer>> getList() {
        return board;
    }

    @Override
    public boolean updateConfig(String name, int maxEntriesPerBoard, int minimumValue, int maximumValue) {

        this.name = name;
        int maxEntriesPerBoardOld = this.maxEntriesPerBoard;
        int minimumValueOld = this.minimumValue;
        int maximumValueOld = this.maximumValue;
        this.maxEntriesPerBoard = maxEntriesPerBoard;
        this.minimumValue = minimumValue;
        this.maximumValue = maximumValue;

        synchronized (board) {
            doBoardTrim();
        }

        // if board was made bigger or min/max value was lowered/increased, return true to ask for reinsert of players
        return maxEntriesPerBoardOld < maxEntriesPerBoard
                || minimumValueOld > minimumValue
                || maximumValueOld < maximumValue;
    }

    @Override
    public int getIndex(HalfminerPlayer player) {

        // speed up initial load
        if (!hasInitialLoad) {
            return Integer.MIN_VALUE;
        }

        synchronized (board) {
            for (int i = 0; i < board.size(); i++) {
                if (board.get(i).getLeft().equals(player.getUniqueId())) {
                    return i;
                }
            }
        }

        return Integer.MIN_VALUE;
    }

    @Override
    public void insertOrUpdatePlayer(HalfminerPlayer toUpdate) {

        int currentIndex = getIndex(toUpdate);
        int value = toUpdate.getInt(dataType);
        if (value < minimumValue || value > maximumValue) {
            // remove if currently on board
            if (currentIndex >= 0) {
                board.remove(currentIndex);
            }

            return;
        }

        // insert if already on board, board not full or if lowest value is smaller than player value
        if (currentIndex >= 0 || board.size() < maxEntriesPerBoard || getLowestValue() < value) {

            Pair<UUID, Integer> newPair = new Pair<>(toUpdate.getUniqueId(), value);
            if (board.isEmpty()) {
                board.add(newPair);
                return;
            }

            // if already on board swap value up/down until barrier is hit
            if (currentIndex >= 0) {

                synchronized (board) {

                    // if next element is lower, move up, if previous element is higher move down, else update value
                    int swapDirection;
                    if (currentIndex > 0 && board.get(currentIndex - 1).getRight() < value) {
                        swapDirection = -1; // move "up"
                    } else if (currentIndex < board.size() - 1 && board.get(currentIndex + 1).getRight() > value) {
                        swapDirection = 1; // move "down"
                    } else {
                        board.set(currentIndex, newPair);
                        return;
                    }

                    for (int i = currentIndex + swapDirection; i >= 0 && i < board.size(); i += swapDirection) {
                        Pair<UUID, Integer> elemTemp = board.get(i);
                        if ((swapDirection > 0 && elemTemp.getRight() <= value)
                                || (swapDirection < 0 && elemTemp.getRight() >= value)) {
                            // barrier hit
                            break;
                        }
                        board.set(i, newPair);
                        board.set(i - swapDirection, elemTemp);
                    }
                }
            } else {
                synchronized (board) {
                    for (int i = board.size() - 1; i >= 0; i--) {
                        if (board.get(i).getRight() > value) {
                            board.add(i + 1, newPair);
                            doBoardTrim();
                            return;
                        }
                    }

                    board.add(0, newPair);
                    doBoardTrim();
                }
            }
        }
    }

    @Override
    public void insertOrUpdatePlayers(List<HalfminerPlayer> toUpdate) {
        insertOrUpdatePlayers(toUpdate, false);
    }

    @Override
    public void insertOrUpdatePlayers(List<HalfminerPlayer> toUpdate, boolean clearFill) {

        if (clearFill) {
            board.clear();
            hasInitialLoad = false;
        }

        toUpdate.forEach(this::insertOrUpdatePlayer);

        if (!hasInitialLoad) {
            hasInitialLoad = true;
        }
    }

    private int getLowestValue() {
        if (board.isEmpty()) {
            return Integer.MIN_VALUE;
        } else {
            return board.get(board.size() - 1).getRight();
        }
    }

    private void doBoardTrim() {
        if (!board.isEmpty()) {
            while (board.get(0).getRight() > maximumValue) {
                board.remove(0);
            }
            while (board.size() > maxEntriesPerBoard || (getLowestValue() < minimumValue)) {
                board.remove(board.size() - 1);
            }
        }
    }
}
