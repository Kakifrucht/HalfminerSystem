package de.halfminer.hmb.enums;

/**
 * Contains a list of included battle modes and their classnames, needed for reflection
 */
public enum BattleModeType {
    GLOBAL  ("GlobalMode", null),
    DUEL    ("DuelMode", "DuelArena"),
    FFA     ("FFAMode", "FFAArena");

    private final String modeClassName;
    private final String arenaClassName;

    BattleModeType(String classNameMode, String classNameArena) {
        this.modeClassName = classNameMode;
        this.arenaClassName = classNameArena;
    }

    public String getModeClassName() {
        return modeClassName;
    }

    public String getArenaClassName() {
        return arenaClassName;
    }

    public static BattleModeType getBattleMode(String toResolve) {
        if (toResolve.length() < 3) return null;
        for (BattleModeType type : values()) {
            if (type.getModeClassName().toLowerCase().startsWith(toResolve.toLowerCase()))
                return type;
        }
        return null;
    }
}
