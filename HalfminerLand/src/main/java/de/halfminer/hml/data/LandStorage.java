package de.halfminer.hml.data;

import de.halfminer.hml.LandClass;
import de.halfminer.hml.land.Land;
import de.halfminer.hms.handler.HanStorage;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.util.StringArgumentSeparator;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Storage backed by a {@link HanStorage} instance for map/land and player data storage.
 * Returns {@link LandPlayer} instances, a list of pinned teleports via {@link #getPinnedTeleportList()} and
 * the entire map section as {@link ConfigurationSection} via {@link #getMapSection()}.
 */
public class LandStorage extends LandClass {

    private static final String ROOT_MAP_PATH = "map";
    private static final String PINNED_TELEPORT_PATH = "pinnedTeleports";

    private final HanStorage landStorage;


    public LandStorage(HanStorage landStorage) {
        super(false);
        this.landStorage = landStorage;
    }

    public LandPlayer getLandPlayer(OfflinePlayer player) {
        return getLandPlayer(hms.getStorageHandler().getPlayer(player));
    }

    public LandPlayer getLandPlayer(HalfminerPlayer player) {
        return new LandPlayer(landStorage, player);
    }

    public ConfigurationSection getMapSection() {
        return landStorage.getConfigurationSection(ROOT_MAP_PATH);
    }

    public List<PinnedTeleport> getPinnedTeleportList() {

        List<String> pinnedTeleportStringList = landStorage.getRootSection().getStringList(PINNED_TELEPORT_PATH);
        List<PinnedTeleport> pinnedTeleportList = new ArrayList<>();

        List<String> teleportDeletedList = new ArrayList<>();
        for (String pinnedTeleportStr : pinnedTeleportStringList) {

            StringArgumentSeparator argumentSeparator = new StringArgumentSeparator(pinnedTeleportStr, ',');
            String teleport = argumentSeparator.getArgument(0);
            Land land = hml.getBoard().getLandFromTeleport(teleport);

            if (land != null) {

                Material material = null;
                if (argumentSeparator.meetsLength(2)) {
                    try {
                        material = Material.valueOf(argumentSeparator.getArgument(1));
                    } catch (IllegalArgumentException ignored) {}
                }

                pinnedTeleportList.add(new PinnedTeleport(land, material));
            } else {
                teleportDeletedList.add(pinnedTeleportStr);
            }
        }

        if (!teleportDeletedList.isEmpty()) {
            pinnedTeleportStringList.removeAll(teleportDeletedList);
            landStorage.getRootSection().set(PINNED_TELEPORT_PATH, pinnedTeleportStringList);
        }

        return pinnedTeleportList;
    }

    public void setPinnedTeleportList(List<PinnedTeleport> pinnedTeleportList) {

        List<String> teleportAsString = pinnedTeleportList
                .stream()
                .map(PinnedTeleport::toString)
                .collect(Collectors.toList());

        landStorage.set(PINNED_TELEPORT_PATH, teleportAsString);
    }

    public void saveData() {
        landStorage.saveConfig();
    }
}
