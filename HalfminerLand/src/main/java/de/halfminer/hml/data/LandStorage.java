package de.halfminer.hml.data;

import de.halfminer.hml.LandClass;
import de.halfminer.hms.handler.HanStorage;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class LandStorage extends LandClass {

    private static final String ROOT_MAP_PATH = "map";

    private final HanStorage landStorage;


    public LandStorage(HanStorage landStorage) {
        super(false);
        this.landStorage = landStorage;
    }

    public LandPlayer getLandPlayer(Player player) {
        return getLandPlayer(hms.getStorageHandler().getPlayer(player));
    }

    public LandPlayer getLandPlayer(HalfminerPlayer player) {
        return new LandPlayer(landStorage, player);
    }

    public ConfigurationSection getMapSection() {
        return landStorage.getConfigurationSection(ROOT_MAP_PATH);
    }

    public void saveData() {
        landStorage.saveConfig();
    }
}
