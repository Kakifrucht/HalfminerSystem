package de.halfminer.hms.handler.type;

/**
 * Contains references to all handlers
 */
public enum HandlerType {

    BOSS_BAR    ("BossBar"),
    HOOKS       ("Hooks"),
    STORAGE     ("Storage"),
    TELEPORT    ("Teleport"),
    TITLES      ("Titles");

    private final String className;

    HandlerType(String name) {
        className = name;
    }

    public String getClassName() {
        return "Han" + className;
    }
}
