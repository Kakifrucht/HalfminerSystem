package de.halfminer.hms.enums;

public enum DataType {

    BLOCKS_BROKEN   ("blocksbroken"),
    BLOCKS_PLACED   ("blocksplaced"),
    DEATHS          ("deaths"),
    KD_RATIO        ("kdratio"),
    KILLS           ("kills"),
    LASTKILL        ("lastkill"),
    LAST_NAME       ("lastname"),
    LAST_NAMES      ("lastnames"),
    MOB_KILLS       ("mobkills"),
    NEUTP_USED      ("neutp"),
    REVENUE         ("revenue"),
    SKILL_ELO       ("skillelo"),
    SKILL_GROUP     ("skillgroup"),
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
