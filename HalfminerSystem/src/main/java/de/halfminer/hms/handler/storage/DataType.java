package de.halfminer.hms.handler.storage;

/**
 * Contains references to all stats type, usable with {@link de.halfminer.hms.util.HalfminerPlayer}
 */
public enum DataType {

    BLOCKS_BROKEN   ("blocksbroken"),
    BLOCKS_PLACED   ("blocksplaced"),
    DEATHS          ("deaths"),
    KD_RATIO        ("kdratio"),
    KILLS           ("kills"),
    LAST_PVP        ("lastpvp"),
    LAST_NAME       ("lastname"),
    LAST_NAMES      ("lastnames"),
    LAST_REPAIR     ("lastrepair"),
    MOB_KILLS       ("mobkills"),
    NEWTP_USED      ("newtp"),
    REVENUE         ("revenue"),
    SKILL_ELO       ("skillelo"),
    SKILL_LEVEL     ("skilllevel"),
    TIME_ONLINE     ("timeonline"),
    VOTES           ("votes");

    private final String name;

    DataType(String type) {
        name = type;
    }

    public String toString() {
        return name;
    }
}