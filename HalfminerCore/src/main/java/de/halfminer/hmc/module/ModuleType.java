package de.halfminer.hmc.module;

/**
 * Contains references to all modules
 */
public enum ModuleType {

    ANTI_KILLFARMING    ("AntiKillfarming"),
    ANTI_XRAY           ("AntiXray"),
    AUTO_MESSAGE        ("AutoMessage"),
    CHAT_MANAGER        ("ChatManager"),
    COMBAT_LOG          ("CombatLog"),
    CRAFTING            ("Crafting"),
    GLITCH_PROTECTION   ("GlitchProtection"),
    HEALTH_BAR          ("HealthBar"),
    MOTD                ("Motd"),
    PERFORMANCE         ("Performance"),
    PVP                 ("PvP"),
    RESPAWN             ("Respawn"),
    SELL                ("Sell"),
    SKILL_LEVEL         ("SkillLevel"),
    STATIC_LISTENERS    ("StaticListeners"),
    STATS               ("Stats"),
    STATS_TOP           ("StatsTop"),
    TITLES              ("Titles"),
    TPS                 ("Tps");

    private final String className;

    ModuleType(String name) {
        className = name;
    }

    public String getClassName() {
        return "Mod" + className;
    }
}
