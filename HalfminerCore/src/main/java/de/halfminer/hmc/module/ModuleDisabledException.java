package de.halfminer.hmc.module;

public class ModuleDisabledException extends Exception {
    private ModuleType type;

    public ModuleDisabledException(ModuleType type) {

        this.type = type;
    }

    public ModuleType getType() {
        return type;
    }
}
