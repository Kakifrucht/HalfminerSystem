package de.halfminer.hms.enums;

public enum ModuleType {

    ANTI_KILLFARMING ("AntiKillfarming"),
    AUTO_MESSAGE ("AutoMessage"),
    GLITCH_PROTECTION ("CombatLog"),
    COMBAT_LOG ("GlitchProtection"),
    MOTD ("Motd"),
    PERFORMANCE ("Performance"),
    PVP ("PvP"),
    RESPAWN ("Respawn"),
    SIGN_EDIT ("SignEdit"),
    SKILL_LEVEL ("SkillLevel"),
    STATIC_LISTENERS ("StaticListeners"),
    STATS ("Stats"),
    TITLES ("Titles"),
    TPS ("Tps");

    private final String className;

    ModuleType(String name) {
        className = name;
    }

    public String getClassName() {
        return "Mod" + className;
    }
}
