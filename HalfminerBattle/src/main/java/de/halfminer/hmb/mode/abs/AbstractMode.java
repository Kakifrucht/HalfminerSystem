package de.halfminer.hmb.mode.abs;

import de.halfminer.hmb.BattleClass;
import de.halfminer.hms.manageable.Reloadable;

/**
 * Abstract game mode
 */
public abstract class AbstractMode extends BattleClass implements BattleMode, Reloadable {

    protected final BattleModeType type;

    protected AbstractMode(BattleModeType type) {
        this.type = type;
    }

    @Override
    public BattleModeType getType() {
        return type;
    }
}
