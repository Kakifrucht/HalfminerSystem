package de.halfminer.hmb.data;

import de.halfminer.hmb.HalfminerBattle;
import de.halfminer.hmb.arena.abs.AbstractKitArena;
import de.halfminer.hmb.arena.abs.Arena;
import de.halfminer.hmb.enums.BattleModeType;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Pair;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.ChatColor;
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
    private Map<Pair<BattleModeType, String>, ItemStack[]> kits = new HashMap<>();

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

    public Arena getArena(BattleModeType modeType, String name) {
        for (Arena arena : getArenasFromType(modeType)) {
            if (arena.getName().equalsIgnoreCase(name))
                return arena;
        }
        return null;
    }

    private Map<String, Arena> getArenaMap(BattleModeType type) {
        if (!arenas.containsKey(type)) {
            arenas.put(type, new HashMap<>());
        }
        return arenas.get(type);
    }

    public ItemStack[] getKit(BattleModeType modeType, String name) {
        return kits.get(new Pair<>(modeType, name));
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

                BattleModeType type = BattleModeType.getBattleMode(mode);
                if (type == null) {
                    hmb.getLogger().warning("Invalid BattleModeType while loading kit for mode kit " + mode);
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

        Map<BattleModeType, Map<String, Arena>> oldArenas = arenas;
        arenas = new HashMap<>();

        ConfigurationSection arenaSection = arenaConfig.getConfigurationSection("arenas");
        if (arenaSection != null) {
            for (String arenaName : arenaSection.getKeys(false)) {

                BattleModeType type = BattleModeType.getBattleMode(arenaSection.getString(arenaName + ".battleMode"));
                if (type == null) {
                    hmb.getLogger().warning("Invalid BattleModeType for arena " + arenaName);
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

    public boolean addArena(BattleModeType modeType, String name, Location... spawns) {

        if (getArena(modeType, name) != null) return false;

        Map<String, Arena> arenasMode = getArenaMap(modeType);
        Arena newArena = getArenaFromBattleMode(modeType, name);
        if (newArena != null) {
            newArena.setSpawns(Arrays.asList(spawns));
            arenasMode.put(name.toLowerCase(), newArena);
            saveData();
            return true;
        } else return false;
    }

    public boolean delArena(BattleModeType modeType, String name) {

        Map<String, Arena> arenasMode = getArenaMap(modeType);
        if (arenasMode != null) {
            Arena arena = arenasMode.get(name.toLowerCase());
            if (arena != null) {
                arenasMode.remove(name.toLowerCase());
                kits.remove(new Pair<>(modeType, arena.getName()));
                saveData();
                return true;
            }
        }
        return false;
    }

    public boolean setSpawn(BattleModeType modeType, String arenaName, Location location, int spawnToSet) {
        Arena setSpawn = getArena(modeType, arenaName);
        if (setSpawn != null) {
            setSpawn.setSpawn(location, spawnToSet);
            saveData();
            return true;
        }
        return false;
    }

    public boolean removeSpawn(BattleModeType modeType, String arenaName, int spawnToRemove) {
        Arena clearSpawns = getArena(modeType, arenaName);
        if (clearSpawns != null) {
            clearSpawns.removeSpawn(spawnToRemove);
            saveData();
            return true;
        }
        return false;
    }

    public boolean setKit(BattleModeType modeType, String arenaName, PlayerInventory setKitTo) {
        Arena toSetKit = getArena(modeType, arenaName);
        if (toSetKit instanceof AbstractKitArena) {
            ItemStack[] newKit = setKitTo.getContents();
            kits.put(new Pair<>(modeType, toSetKit.getName()), newKit);
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


    public void sendArenaSelection(Player selector, List<Arena> freeArenas, String command, String randomKey) {

        ComponentBuilder builder = new ComponentBuilder("");

        for (Arena freeArena : freeArenas) {
            String tooltipOnHover = MessageBuilder.create(hmb, "modeGlobalChooseArenaHover")
                    .addPlaceholderReplace("%ARENA%", freeArena.getName())
                    .returnMessage();
            builder.append(freeArena.getName())
                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command + freeArena.getName()))
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(tooltipOnHover).create()))
                    .color(net.md_5.bungee.api.ChatColor.GREEN).bold(true)
                    .append("  ").reset();
        }

        if (randomKey.length() > 0) {
            builder.append(MessageBuilder.returnMessage(hmb, "modeGlobalRandomArena"))
                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command + "random"))
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new ComponentBuilder(MessageBuilder.returnMessage(hmb, randomKey)).create()))
                    .color(net.md_5.bungee.api.ChatColor.GRAY);
        }

        selector.playSound(selector.getLocation(), Sound.BLOCK_NOTE_PLING, 1.0f, 1.9f);
        selector.spigot().sendMessage(builder.create());
    }

    private void saveData() {

        arenaConfig.set("arenas", null);
        arenaConfig.set("kits", null);

        for (Map<String, Arena> arenaPairs : arenas.values()) {
            for (Arena arena : arenaPairs.values()) {
                ConfigurationSection newSection = arenaConfig.createSection("arenas." + arena.getName());
                newSection.set("battleMode", arena.getBattleModeType().toString());
                newSection.set("spawns", arena.getSpawns());
            }
        }

        for (Map.Entry<Pair<BattleModeType, String>, ItemStack[]> entry : kits.entrySet()) {
            BattleModeType battleMode = entry.getKey().getLeft();
            String arenaName = entry.getKey().getRight();
            ItemStack[] stacks = entry.getValue();
            arenaConfig.set("kits." + battleMode.toString() + '.' + arenaName, stacks);
        }

        try {
            arenaConfig.save(arenaFile);
        } catch (IOException e) {
            e.printStackTrace();
            hmb.getLogger().severe("Arena config could not be saved");
        }
    }

    private Arena getArenaFromBattleMode(BattleModeType battleMode, String name) {

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
