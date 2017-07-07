package de.halfminer.hmb.mode.abs;

/**
 * Contains a list of included battle modes and their classnames, needed for reflection
 */
public enum BattleModeType {
    GLOBAL("GlobalMode", null, "global"),
    DUEL("DuelMode", "DuelArena", "duel"),
    FFA("FFAMode", "FFAArena", "ffa");

    private final String modeClassName;
    private final String arenaClassName;
    private final String configNode;

    BattleModeType(String classNameMode, String classNameArena, String configNode) {
        this.modeClassName = classNameMode;
        this.arenaClassName = classNameArena;
        this.configNode = configNode;
    }

    public String getModeClassName() {
        return modeClassName;
    }

    public String getArenaClassName() {
        return arenaClassName;
    }

    public String getConfigNode() {
        return configNode;
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
