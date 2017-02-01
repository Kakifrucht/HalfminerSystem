package de.halfminer.hmb.enums;

/**
 * Contains a list of included gamemodes and their classnames, needed for reflection.
 */
public enum GameModeType {
    GLOBAL  ("GlobalMode", null),
    DUEL    ("DuelMode", "DuelArena");

    private final String modeClassName;
    private final String arenaClassName;

    GameModeType(String classNameMode, String classNameArena) {
        this.modeClassName = classNameMode;
        this.arenaClassName = classNameArena;
    }

    public String getModeClassName() {
        return modeClassName;
    }

    public String getArenaClassName() {
        return arenaClassName;
    }

    public static GameModeType getGameMode(String toResolve) {
        if (toResolve.length() < 3) return null;
        for (GameModeType type : values()) {
            if (type.getModeClassName().toLowerCase().startsWith(toResolve))
                return type;
        }
        return null;
    }
}
