package de.halfminer.hmb.data;

import de.halfminer.hmb.HalfminerBattle;
import de.halfminer.hmb.arena.abs.AbstractKitArena;
import de.halfminer.hmb.arena.abs.Arena;
import de.halfminer.hmb.enums.GameModeType;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Pair;
import org.bukkit.ChatColor;
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
import java.util.logging.Level;

/**
 * Loading arenas and kits from config and maintaining necessary data
 */
public class ArenaManager {

    private static final HalfminerBattle hmb = HalfminerBattle.getInstance();
    private File arenaFile;
    private FileConfiguration arenaConfig;

    private Map<GameModeType, Map<String, Arena>> arenas = new HashMap<>();
    private Map<Pair<GameModeType, String>, ItemStack[]> kits = new HashMap<>();

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

    public Arena getArena(GameModeType gameMode, String name) {
        for (Arena arena : getArenasFromType(gameMode)) {
            if (arena.getName().equalsIgnoreCase(name))
                return arena;
        }
        return null;
    }

    private Map<String, Arena> getArenaMap(GameModeType type) {
        if (!arenas.containsKey(type)) {
            arenas.put(type, new HashMap<>());
        }
        return arenas.get(type);
    }

    public ItemStack[] getKit(GameModeType gameMode, String name) {
        return kits.get(new Pair<>(gameMode, name));
    }

    public void reloadConfig() throws IOException {

        if (arenaFile == null || !arenaFile.exists()) {
            arenaFile = new File(hmb.getDataFolder(), "arenas.yml");
            if (!arenaFile.exists() && !arenaFile.createNewFile()) {
                throw new IOException("Could not create arenas.yml");
            }
        }

        arenaConfig = YamlConfiguration.loadConfiguration(arenaFile);

        // kits must be (re)loaded first, as maps depend on them already being loaded
        kits = new HashMap<>();

        ConfigurationSection kitSection = arenaConfig.getConfigurationSection("kits");
        if (kitSection != null) {
            for (String mode : kitSection.getKeys(false)) {

                GameModeType type = GameModeType.getGameMode(mode);
                if (type == null) {
                    hmb.getLogger().warning("Invalid gamemode type while loading kit for mode kit " + mode);
                    continue;
                }

                ConfigurationSection kitSectionForMode = kitSection.getConfigurationSection(mode);
                for (String arenaName : kitSectionForMode.getKeys(false)) {

                    List<?> contents = kitSectionForMode.getList(arenaName);
                    ItemStack[] kit = new ItemStack[contents.size()];
                    for (int i = 0; i < contents.size(); i++) {
                        if (contents.get(i) instanceof ItemStack)
                            kit[i] = (ItemStack) contents.get(i);
                    }

                    kits.put(new Pair<>(type, arenaName), kit);
                }
            }
        }

        Map<GameModeType, Map<String, Arena>> oldArenas = arenas;
        arenas = new HashMap<>();

        ConfigurationSection arenaSection = arenaConfig.getConfigurationSection("arenas");
        if (arenaSection != null) {
            for (String arenaName : arenaSection.getKeys(false)) {

                GameModeType type = GameModeType.getGameMode(arenaSection.getString(arenaName + ".gameMode"));
                if (type == null) {
                    hmb.getLogger().warning("Invalid gamemode type for arena " + arenaName);
                    continue;
                }

                Map<String, Arena> arenaMap = getArenaMap(type);

                List<Location> spawnLocations = new ArrayList<>();
                arenaSection
                        .getList(arenaName + ".spawns")
                        .stream()
                        .filter(obj -> obj instanceof Location)
                        .forEach(obj -> spawnLocations.add((Location) obj));

                // check to see if arena already exists and just update kit/spawns, else add new one
                Map<String, Arena> oldArenasMap = oldArenas.get(type);
                if (oldArenasMap != null && oldArenasMap.containsKey(arenaName.toLowerCase())) {
                    Arena alreadyExistingToReload = oldArenasMap.get(arenaName.toLowerCase());
                    alreadyExistingToReload.setSpawns(spawnLocations);
                    alreadyExistingToReload.reload();
                    arenaMap.put(arenaName.toLowerCase(), alreadyExistingToReload);
                } else {
                    addArena(type, arenaName, spawnLocations.toArray(new Location[spawnLocations.size()]));
                }
            }
        }

        // log load amounts
        int totalArenas = 0;
        for (Map<String, Arena> map : arenas.values()) totalArenas += map.size();
        MessageBuilder.create(hmb, "modeGlobalArenaLoadLog")
                .addPlaceholderReplace("%ARENAS%", String.valueOf(totalArenas))
                .addPlaceholderReplace("%KITS%", String.valueOf(kits.size()))
                .logMessage(Level.INFO);
    }

    public boolean addArena(GameModeType gameMode, String name, Location... spawns) {

        if (getArena(gameMode, name) != null) return false;

        Map<String, Arena> arenasMode = getArenaMap(gameMode);
        Arena newArena = getArenaFromGamemode(gameMode, name);
        if (newArena != null) {
            newArena.setSpawns(Arrays.asList(spawns));
            arenasMode.put(name.toLowerCase(), newArena);
            saveData();
            return true;
        } else return false;
    }

    public boolean delArena(GameModeType gameMode, String name) {

        Map<String, Arena> arenasMode = getArenaMap(gameMode);
        if (arenasMode != null) {
            Arena arena = arenasMode.get(name.toLowerCase());
            if (arena != null) {
                arenasMode.remove(name.toLowerCase());
                saveData();
                return true;
            }
        }
        return false;
    }

    public boolean setSpawn(GameModeType gameMode, String arenaName, Location location, int spawn) {
        Arena setSpawn = getArena(gameMode, arenaName);
        if (setSpawn != null) {
            setSpawn.setSpawn(location, spawn);
            saveData();
            return true;
        }
        return false;
    }

    public boolean clearSpawns(GameModeType gameMode, String arenaName) {
        Arena clearSpawns = getArena(gameMode, arenaName);
        if (clearSpawns != null) {
            clearSpawns.setSpawns(Collections.emptyList());
            saveData();
            return true;
        }
        return false;
    }

    public boolean setKit(GameModeType gameMode, String arenaName, PlayerInventory setKitTo) {
        Arena toSetKit = getArena(gameMode, arenaName);
        if (toSetKit instanceof AbstractKitArena) {
            ItemStack[] newKit = setKitTo.getContents();
            kits.put(new Pair<>(gameMode, toSetKit.getName()), newKit);
            toSetKit.reload();
            saveData();
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

    public String getStringFromArenaList(List<Arena> arenas) {

        if (arenas.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        int number = 1;
        for (Arena arena : arenas) {
            sb.append(ChatColor.GRAY)
                    .append(number)
                    .append(": ")
                    .append(arena.isFree() ? ChatColor.GREEN : ChatColor.RED)
                    .append(arena.getName())
                    .append(' ');
            number++;
        }

        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    private void saveData() {

        arenaConfig.set("arenas", null);
        arenaConfig.set("kits", null);

        for (Map<String, Arena> arenaPairs : arenas.values()) {
            for (Arena arena : arenaPairs.values()) {
                ConfigurationSection newSection = arenaConfig.createSection("arenas." + arena.getName());
                newSection.set("gameMode", arena.getGameMode().toString());
                newSection.set("spawns", arena.getSpawns());
            }
        }

        for (Map.Entry<Pair<GameModeType, String>, ItemStack[]> entry : kits.entrySet()) {
            GameModeType gameMode = entry.getKey().getLeft();
            String arenaName = entry.getKey().getRight();
            ItemStack[] stacks = entry.getValue();
            arenaConfig.set("kits." + gameMode.toString() + '.' + arenaName, stacks);
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
            Constructor<?> cons = cl.getConstructor(String.class);
            return (Arena) cons.newInstance(name);
        } catch (Exception e) {
            hmb.getLogger().severe("Arena class for GameModeType " + gameMode + " not found");
            e.printStackTrace();
            return null;
        }
    }
}
