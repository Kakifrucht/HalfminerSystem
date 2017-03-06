package de.halfminer.hmb;

import de.halfminer.hmb.data.ArenaManager;
import de.halfminer.hmb.data.PlayerManager;
import de.halfminer.hms.HalfminerClass;

/**
 * Arenas, gamemodes and others for HalfminerBattle
 */
@SuppressWarnings("SameParameterValue")
public class BattleClass extends HalfminerClass {

    protected final static HalfminerBattle hmb = HalfminerBattle.getInstance();
    protected static final PlayerManager pm = hmb.getPlayerManager();
    protected static final ArenaManager am = hmb.getArenaManager();

    protected BattleClass() {
        super(hmb);
    }

    protected BattleClass(boolean register) {
        super(hmb, register);
    }
}
