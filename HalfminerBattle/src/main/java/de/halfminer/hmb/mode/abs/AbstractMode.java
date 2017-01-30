package de.halfminer.hmb.mode.abs;

import de.halfminer.hmb.HalfminerBattle;
import de.halfminer.hmb.data.ArenaManager;
import de.halfminer.hmb.data.PlayerManager;

/**
 * Abstract game mode containing shortcuts to commonly used objects
 */
public abstract class AbstractMode implements GameMode {

    protected static final HalfminerBattle hmb = HalfminerBattle.getInstance();
    protected static final PlayerManager pm = hmb.getPlayerManager();
    protected static final ArenaManager am = hmb.getArenaManager();
}
