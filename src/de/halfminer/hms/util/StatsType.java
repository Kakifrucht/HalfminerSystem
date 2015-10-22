package de.halfminer.hms.util;

public enum StatsType {

    KILLS           ("kills"),
    DEATHS          ("deaths"),
    KD_RATIO        ("kdratio"),
    TIME_ONLINE     ("timeonline"),
    BLOCKS_PLACED   ("blocksplaced"),
    BLOCKS_BROKEN   ("blocksbroken"),
    JOINS           ("joins"),
    VOTES           ("votes"),
    SKILL_LEVEL     ("skilllevel"),
    SKILL_ELO       ("skillelo"),
    SKILL_GROUP     ("skillgroup"),
    MOB_KILLS       ("mobkills"),
    LASTKILL        ("lastkill"),
    LAST_NAME       ("lastname"),
    LAST_NAMES      ("lastnames"),
    NEUTP_USED      ("neutp");

    private final String name;

    StatsType(String type) {
        name = type;
    }

    public String toString() {
        return name;
    }

}
