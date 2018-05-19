package de.halfminer.hms.handler.storage;

/**
 * Contains references to all stats type, usable with {@link HalfminerPlayer}
 */
public enum DataType {

    BLOCKS_BROKEN("blocksbroken"),
    BLOCKS_PLACED("blocksplaced"),
    DEATHS("deaths"),
    GEMS("gems"),
    KD_RATIO("kdratio"),
    KILLS("kills"),
    LAST_NAME("lastname"),
    LAST_NAMES("lastnames"),
    LAST_REPAIR("lastrepair"),
    LAST_SEEN("lastseen"),
    MOB_KILLS("mobkills"),
    NEWTP_USED("newtp"),
    REVENUE("revenue"),
    SKILL_ELO("skillelo"),
    SKILL_ELO_LAST_CHANGE("skillelolastchange"),
    SKILL_LEVEL("skilllevel"),
    TIME_ONLINE("timeonline"),
    VOTES("votes");

    private final String name;

    DataType(String type) {
        name = type;
    }

    public String toString() {
        return name;
    }

    public static DataType getFromString(String string) {

        String lowercase = string.toLowerCase();
        for (DataType dataType : values()) {
            if (dataType.toString().equals(lowercase)) {
                return dataType;
            }
        }

        return null;
    }
}
