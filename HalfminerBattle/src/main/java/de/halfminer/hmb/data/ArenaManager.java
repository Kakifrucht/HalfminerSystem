package de.halfminer.hmb.data;

import de.halfminer.hmb.HalfminerBattle;
import de.halfminer.hmb.arena.abs.Arena;
import de.halfminer.hmb.arena.abs.KitArena;
import de.halfminer.hmb.enums.BattleModeType;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
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

    private Map<BattleModeType, Map<String, Arena>> arenas = new HashMap<>();


    public List<Arena> getArenasFromType(BattleModeType type) {
        if (arenas.containsKey(type))
            return new LinkedList<>(arenas.get(type).values());
        else return Collections.emptyList();
    }

    public List<Arena> getFreeArenasFromType(BattleModeType type) {
        List<Arena> all = getArenasFromType(type);
        all.removeIf(arena -> !arena.isFree());
        return all;
    }

    private Map<String, Arena> getArenaMap(BattleModeType type) {
        if (!arenas.containsKey(type)) {
            arenas.put(type, new HashMap<>());
        }
        return arenas.get(type);
    }

    public Arena getArena(BattleModeType modeType, String name) {
        return getArenaMap(modeType).get(name.toLowerCase());
    }

    public void reloadConfig() throws IOException {

        if (arenaFile == null || !arenaFile.exists()) {
            arenaFile = new File(hmb.getDataFolder(), "arenas.yml");
            if (!arenaFile.exists() && !arenaFile.createNewFile()) {
                throw new IOException("Could not create arenas.yml as file already exists");
            }
        }

        arenaConfig = YamlConfiguration.loadConfiguration(arenaFile);
        int totalArenasLoaded = 0;
        int totalKitsLoaded = 0;

        Map<BattleModeType, Map<String, Arena>> oldArenas = arenas;
        arenas = new HashMap<>();

        for (String key : arenaConfig.getKeys(false)) {

            BattleModeType type = BattleModeType.getBattleMode(key);
            if (type == null) {
                hmb.getLogger().warning("Invalid BattleModeType: " + key);
                continue;
            }

            ConfigurationSection arenaSection = arenaConfig.getConfigurationSection(key);
            Map<String, Arena> oldArenaTypeMap = oldArenas.get(type);
            Map<String, Arena> newArenaTypeMap = getArenaMap(type);

            for (String arenaName : arenaSection.getKeys(false)) {

                String arenaNameLowerCase = arenaName.toLowerCase();
                // load spawns
                List<Location> spawnLocations = new ArrayList<>();
                arenaSection
                        .getList(arenaName + ".spawns")
                        .stream()
                        .filter(obj -> obj instanceof Location)
                        .forEach(obj -> spawnLocations.add((Location) obj));

                // load kit
                ItemStack[] kit = null;
                if (arenaSection.contains(arenaName + ".kit")) {
                    List<?> contents = arenaSection.getList(arenaName + ".kit");
                    kit = new ItemStack[contents.size()];
                    for (int i = 0; i < contents.size(); i++) {
                        if (contents.get(i) instanceof ItemStack)
                            kit[i] = (ItemStack) contents.get(i);
                    }
                    totalKitsLoaded++;
                }

                // check to see if arena already exists and just update kit/spawns, else add new one
                if (oldArenaTypeMap != null && oldArenaTypeMap.containsKey(arenaNameLowerCase)) {
                    Arena arenaToReload = oldArenaTypeMap.get(arenaNameLowerCase);
                    arenaToReload.setSpawns(spawnLocations);
                    if (kit != null && arenaToReload instanceof KitArena) {
                        ((KitArena) arenaToReload).setKit(kit);
                    }
                    newArenaTypeMap.put(arenaNameLowerCase, arenaToReload);
                    totalArenasLoaded++;
                } else {
                    if (addArena(type, arenaName, spawnLocations, kit)) {
                        totalArenasLoaded++;
                    }
                }
            }
        }

        MessageBuilder.create("modeGlobalArenaLoadLog", hmb)
                .addPlaceholderReplace("%ARENAS%", String.valueOf(totalArenasLoaded))
                .addPlaceholderReplace("%KITS%", String.valueOf(totalKitsLoaded))
                .logMessage(Level.INFO);
    }

    public boolean addArena(BattleModeType modeType, String name, List<Location> spawns) {
        return addArena(modeType, name, spawns, null);
    }

    private boolean addArena(BattleModeType modeType, String name, List<Location> spawns, ItemStack[] kit) {

        if (getArena(modeType, name) != null) return false;

        Map<String, Arena> arenasMode = getArenaMap(modeType);
        Arena newArena = getNewArenaFromBattleMode(modeType, name);
        if (newArena != null) {
            newArena.setSpawns(spawns);
            if (kit != null && newArena instanceof KitArena) {
                ((KitArena) newArena).setKit(kit);
            }
            arenasMode.put(name.toLowerCase(), newArena);
            return true;
        } else return false;
    }

    public boolean delArena(BattleModeType modeType, String name) {

        Map<String, Arena> arenasMode = getArenaMap(modeType);
        if (arenasMode != null) {
            Arena arena = arenasMode.get(name.toLowerCase());
            if (arena != null) {
                arenasMode.remove(name.toLowerCase());
                return true;
            }
        }
        return false;
    }

    public boolean setSpawn(BattleModeType modeType, String arenaName, Location location, int spawnToSet) {
        Arena setSpawn = getArena(modeType, arenaName);
        if (setSpawn != null) {
            setSpawn.setSpawn(location, spawnToSet);
            return true;
        }
        return false;
    }

    public boolean removeSpawn(BattleModeType modeType, String arenaName, int spawnToRemove) {
        Arena clearSpawns = getArena(modeType, arenaName);
        if (clearSpawns != null) {
            clearSpawns.removeSpawn(spawnToRemove);
            return true;
        }
        return false;
    }

    public boolean setKit(BattleModeType modeType, String arenaName, PlayerInventory setKitTo) {
        Arena toSetKit = getArena(modeType, arenaName);
        if (toSetKit instanceof KitArena) {
            ((KitArena) toSetKit).setKit(setKitTo.getContents());
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

    public String getStringFromBattleMode(BattleModeType modeType) {

        List<Arena> arenas = getArenasFromType(modeType);
        if (arenas.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (Arena arena : arenas) {
            sb.append(ChatColor.GRAY)
                    .append(arena.isFree() ? ChatColor.GREEN : ChatColor.RED)
                    .append(arena.getName())
                    .append(" &r ");
        }

        sb.setLength(sb.length() - 1);
        return sb.toString();
    }


    public void sendArenaSelection(Player selector, List<Arena> freeArenas, String command, String randomHoverKey) {

        ComponentBuilder builder = new ComponentBuilder("");

        for (Arena freeArena : freeArenas) {
            String tooltipOnHover = MessageBuilder.create("modeGlobalChooseArenaHover", hmb)
                    .togglePrefix()
                    .addPlaceholderReplace("%ARENA%", freeArena.getName())
                    .returnMessage();
            builder.append(freeArena.getName())
                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command + freeArena.getName()))
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(tooltipOnHover).create()))
                    .color(ChatColor.GREEN).bold(true)
                    .append("  ").reset();
        }

        if (randomHoverKey.length() > 0) {
            builder.append(MessageBuilder.returnMessage("modeGlobalRandomArena", hmb, false))
                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command + "random"))
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new ComponentBuilder(MessageBuilder.returnMessage(randomHoverKey, hmb, false)).create()))
                    .color(ChatColor.GRAY);
        }

        selector.playSound(selector.getLocation(), Sound.BLOCK_NOTE_PLING, 1.0f, 1.9f);
        selector.spigot().sendMessage(builder.create());
    }

    public void saveData() {

        for (BattleModeType type : BattleModeType.values()) {
            arenaConfig.set(Utils.makeStringFriendly(type.toString()), null);
        }

        for (BattleModeType type : arenas.keySet()) {
            ConfigurationSection newSection = arenaConfig.createSection(Utils.makeStringFriendly(type.toString()));

            for (Arena arena : arenas.get(type).values()) {
                ConfigurationSection newArenaSection = newSection.createSection(arena.getName());
                newArenaSection.set("spawns", arena.getSpawns());
                if (arena instanceof KitArena) {
                    newArenaSection.set("kit", ((KitArena) arena).getKit());
                }
            }
        }

        try {
            arenaConfig.save(arenaFile);
        } catch (IOException e) {
            e.printStackTrace();
            hmb.getLogger().severe("Arena config could not be saved");
        }
    }

    public void endAllGames() {
        for (Map<String, Arena> arenasPerType : arenas.values()) {
            arenasPerType.values().forEach(Arena::forceGameEnd);
        }
    }

    private Arena getNewArenaFromBattleMode(BattleModeType battleMode, String name) {

        try {
            Class<?> cl = Class.forName(HalfminerBattle.PACKAGE_PATH + ".arena." + battleMode.getArenaClassName());
            Constructor<?> cons = cl.getConstructor(String.class);
            return (Arena) cons.newInstance(name);
        } catch (Exception e) {
            hmb.getLogger().severe("Arena class for BattleModeType " + battleMode + " not found");
            e.printStackTrace();
            return null;
        }
    }
}
