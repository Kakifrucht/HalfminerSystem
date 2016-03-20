package de.halfminer.hms.enums;

public enum HandlerType {

    BOSS_BAR ("BossBar"),
    TITLES ("Titles");

    private final String className;

    HandlerType(String name) {
        className = name;
    }

    public String getClassName() {
        return "Han" + className;
    }
}
