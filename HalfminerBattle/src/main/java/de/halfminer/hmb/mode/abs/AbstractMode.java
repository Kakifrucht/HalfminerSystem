package de.halfminer.hmb.mode.abs;

import de.halfminer.hmb.HalfminerBattle;
import de.halfminer.hmb.data.ArenaManager;
import de.halfminer.hmb.data.PlayerManager;
import de.halfminer.hmb.enums.BattleModeType;

/**
 * Abstract game mode containing shortcuts to commonly used objects
 */
public abstract class AbstractMode implements BattleMode {

    protected static final HalfminerBattle hmb = HalfminerBattle.getInstance();
    protected static final PlayerManager pm = hmb.getPlayerManager();
    protected static final ArenaManager am = hmb.getArenaManager();

    protected final BattleModeType type;

    public AbstractMode(BattleModeType type) {
        this.type = type;
    }

    @Override
    public BattleModeType getType() {
        return type;
    }
}
