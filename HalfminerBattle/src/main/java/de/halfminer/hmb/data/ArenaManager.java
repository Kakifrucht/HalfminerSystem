package de.halfminer.hmb.data;

import de.halfminer.hmb.HalfminerBattle;
import de.halfminer.hmb.arena.abs.AbstractKitArena;
import de.halfminer.hmb.arena.abs.Arena;
import de.halfminer.hmb.enums.GameModeType;
import de.halfminer.hms.util.Pair;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;

/**
 * Loading arenas and kits from config and creating necessary objects
 */
public class ArenaManager {

    private static final HalfminerBattle hmb = HalfminerBattle.getInstance();
    private File arenaFile;
    private FileConfiguration arenaConfig;

    private Map<GameModeType, Map<String, Arena>> arenas = new HashMap<>();
    private Map<Pair<GameModeType, String>, ItemStack[][]> kits = new HashMap<>();

    public ArenaManager() throws IOException {
        reloadConfig();
    }

    public Arena getArena(GameModeType gameMode, String name) {
        for (Arena arena : getArenasFromType(gameMode)) {
            if (arena.getName().equalsIgnoreCase(name))
                return arena;
        }
        return null;
    }

    public List<Arena> getArenasFromType(GameModeType type) {
        if (arenas.containsKey(type))
            return new LinkedList<>(arenas.get(type).values());
        else return Collections.emptyList();
    }

    public List<Arena> getFreeArenasFromType(GameModeType type) {
        List<Arena> all = getArenasFromType(type);
        all.removeIf(arena -> !arena.isFree());
        return all;
    }

    public ItemStack[][] getKit(GameModeType gameMode, String name) {
        return kits.get(new Pair<>(gameMode, name));
    }

    public void reloadConfig() throws IOException {

        if (arenaFile == null) {
            arenaFile = new File(hmb.getDataFolder(), "arenas.yml");
            if (arenaFile.exists() || arenaFile.createNewFile()) {
                arenaConfig = YamlConfiguration.loadConfiguration(arenaFile);
            } else throw new IOException("Could not create arenas.yml");
        }

        // kits must be (re)loaded first, as maps depend on them already being loaded
        kits = new HashMap<>();

        ConfigurationSection kitSection = arenaConfig.getConfigurationSection("kits");
        if (kitSection != null) {
            for (String name : kitSection.getKeys(false)) {

                ItemStack[][] kit = new ItemStack[3][];
                GameModeType type = GameModeType.getGameMode(kitSection.getString(name + ".gameMode"));
                if (type == null) {
                    hmb.getLogger().warning("Invalid gamemode type for kit " + name);
                    continue;
                }
                List<?> list = kitSection.getList(name + ".stacks");
                for (int i = 0; i < list.size(); i++) {
                    kit[i] = (ItemStack[]) list.get(i);
                }

                kits.put(new Pair<>(type, name), kit);
            }
        }

        Map<GameModeType, Map<String, Arena>> oldArenas = arenas;
        arenas = new HashMap<>();

        ConfigurationSection arenaSection = arenaConfig.getConfigurationSection("arenas");
        if (arenaSection != null) {
            for (String name : arenaSection.getKeys(false)) {

                GameModeType type = GameModeType.getGameMode(arenaSection.getString(name + ".gameMode"));
                if (type == null) {
                    hmb.getLogger().warning("Invalid gamemode type for arena " + name);
                    continue;
                }

                Map<String, Arena> arenaMap = arenas.get(type);
                if (!arenas.containsKey(type)) {
                    arenaMap = new HashMap<>();
                    arenas.put(type, arenaMap);
                }

                List<String> spawns = arenaSection.getStringList(name + ".spawns");
                List<Location> spawnLocations = new ArrayList<>();
                for (String spawn : spawns) {
                    spawnLocations.add(stringToLocation(spawn));
                }

                if (oldArenas.get(type).containsKey(name.toLowerCase())) {
                    Arena alreadyExistingToReload = oldArenas.get(type).get(name);
                    alreadyExistingToReload.setSpawns(spawnLocations);
                    alreadyExistingToReload.reload();
                    arenaMap.put(name.toLowerCase(), alreadyExistingToReload);
                } else {
                    addArena(type, name, (Location[]) spawnLocations.toArray());
                }
            }
        }
    }

    public boolean addArena(GameModeType gameMode, String name, Location... spawns) {

        if (getArena(gameMode, name) != null) return false;

        Map<String, Arena> arenasMode = arenas.get(gameMode);
        Arena newArena = getArenaFromGamemode(gameMode, name);
        if (newArena != null) {
            newArena.setSpawns(Arrays.asList(spawns));
            arenasMode.put(name.toLowerCase(), newArena);
            saveData();
            return true;
        } else return false;
    }

    public boolean delArena(GameModeType gameMode, String name) {

        Map<String, Arena> arenasMode = arenas.get(gameMode);
        if (arenasMode != null) {
            boolean removed = arenasMode.values().removeIf(a -> a.getName().equalsIgnoreCase(name));
            if (removed) {
                kits.keySet().removeIf(pair -> pair.getRight().equalsIgnoreCase(name));
                saveData();
            }
            return removed;
        }
        return false;
    }

    public boolean setSpawn(GameModeType gameMode, String arenaName, Location location, int spawn) {
        Arena setSpawn = getArena(gameMode, arenaName);
        if (setSpawn != null) {
            setSpawn.setSpawn(location, spawn);
            return true;
        }
        return false;
    }

    public boolean setKit(GameModeType gameMode, String arenaName, PlayerInventory setKitTo) {
        Arena setKit = getArena(gameMode, arenaName);
        if (setKit instanceof AbstractKitArena) {
            ItemStack[][] newKit = new ItemStack[3][];
            newKit[0] = setKitTo.getArmorContents();
            newKit[1] = setKitTo.getContents();
            newKit[2] = setKitTo.getExtraContents();
            kits.put(new Pair<>(gameMode, arenaName), newKit);
            setKit.reload();
            return true;
        }
        return false;
    }

    public boolean isArenaSpawn(Location loc) {

        for (Map<String, Arena> map : arenas.values()) {
            for (Arena arena : map.values()) {
                if (arena.isCloseToSpawn(loc))
                    return true;
            }
        }

        return false;
    }

    private void saveData() {
        arenaConfig.set("arenas", null);
        arenaConfig.set("kits", null);
        for (Map<String, Arena> arenaPairs : arenas.values()) {
            for (Arena arena : arenaPairs.values()) {
                ConfigurationSection newSection = hmb.getConfig().createSection("arenas." + arena.getName());
                newSection.set("gameMode", arena.getGameMode());
                newSection.set("spawns", arena.getSpawns());
            }
        }
        for (Map.Entry<Pair<GameModeType, String>, ItemStack[][]> kit : kits.entrySet()) {
            List<ItemStack> concatenatedStacks = new ArrayList<>(50);
            for (ItemStack[] itemStackArray : kit.getValue()) {
                Collections.addAll(concatenatedStacks, itemStackArray);
            }
            arenaConfig.set("kits." + kit.getKey().getRight() + ".gameMode", kit.getKey().getLeft());
            arenaConfig.set("kits." + kit.getKey().getRight() + ".stacks", concatenatedStacks);
        }

        try {
            arenaConfig.save(arenaFile);
        } catch (IOException e) {
            e.printStackTrace();
            hmb.getLogger().severe("Arena config could not be saved");
        }
    }

    private Arena getArenaFromGamemode(GameModeType gameMode, String name) {

        try {
            Class<?> cl = Class.forName(HalfminerBattle.PACKAGE_PATH + ".arena." + gameMode.getArenaClassName());
            Constructor<?> cons = cl.getConstructor(GameModeType.class, String.class);
            return (Arena) cons.newInstance(gameMode, name);
        } catch (Exception e) {
            hmb.getLogger().severe("Arena class for GameModeType " + gameMode + " not found");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Converts a string to a location
     *
     * @param toLocation String in correct format
     * @return Location that the string points to
     */
    private Location stringToLocation(String toLocation) {
        String[] toLocationSplit = toLocation.split(",");
        double x = Double.parseDouble(toLocationSplit[1]);
        double y = Double.parseDouble(toLocationSplit[2]);
        double z = Double.parseDouble(toLocationSplit[3]);
        float yaw = Float.parseFloat(toLocationSplit[4]);
        float pitch = Float.parseFloat(toLocationSplit[5]);
        return new Location(hmb.getServer().getWorld(toLocationSplit[0]), x, y, z, yaw, pitch);
    }

    /**
     * Converts a given location to a string
     *
     * @param loc Location that is supposed to be converted
     * @return String that can be converted back into a Location
     */
    private String locationToString(Location loc) {
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch();
    }
}
