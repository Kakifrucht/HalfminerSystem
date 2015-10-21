package de.halfminer.hmduel.module;

import de.halfminer.hmduel.HalfminerDuel;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loading arenas and kits from config and creating necessary objects
 */
public class ArenaManager {

    private final HalfminerDuel hmd = HalfminerDuel.getInstance();

    private List<Arena> arenas = new ArrayList<>();
    private Map<String, ItemStack[]> kits = new HashMap<>();

    public ArenaManager() {
        loadArenasFromConfig();
    }

    /**
     * Load arenas and kits from config, without damaging already loaded arenas
     */
    public void loadArenasFromConfig() {

        kits = loadKits(); //Simply override old ones, as they are not crucial

        if (arenas.size() > 0) { //Arenas are reloaded, make sure that existing ones are updated and not replaced
            List<Arena> newArenas = loadArenas();
            for (Arena newArena : newArenas) { //Instead of creating new arenas, simply refresh already existing ones
                boolean arenaExists = false;
                for (Arena currentArena : arenas) {
                    if (newArena.getName().equalsIgnoreCase(currentArena.getName())) {
                        currentArena.setLocation(newArena.getLocations()[0], true);
                        currentArena.setLocation(newArena.getLocations()[1], false);
                        currentArena.setKit(getKit(currentArena.getName()));
                        arenaExists = true;
                        break;
                    }
                }
                if (!arenaExists) arenas.add(newArena);
            }
        } else {
            arenas = loadArenas(); //arenas not yet loaded
        }

    }

    /**
     * Determine if arenas exist, used to disable the plugin, if this is not the case
     *
     * @return true if there is no arena (and the plugin should be disabled), else false
     */
    public boolean noArenaExists() {
        return this.arenas.size() == 0;
    }

    /**
     * Returns the already generated list of arenas, used for the ArenaQueue
     *
     * @return list of arenas
     */
    List<Arena> getArenas() {
        return arenas;
    }

    /**
     * Load the arena section from the config
     *
     * @return list containing all arenas read in the config
     */
    private List<Arena> loadArenas() {

        List<Arena> toReturn = new ArrayList<>();

        ConfigurationSection section = hmd.getConfig().getConfigurationSection("arenas");
        if (section != null) {
            for (String s : section.getKeys(false)) {

                Location[] locations = new Location[]
                        {stringToLocation(section.getString(s + ".spawn1")), stringToLocation(section.getString(s + ".spawn2"))};

                toReturn.add(new Arena(s, locations, getKit(s)));
            }
        }

        return toReturn;
    }

    /**
     * Load the kit section from the config
     *
     * @return HashMap, where String is a
     */
    private Map<String, ItemStack[]> loadKits() {

        Map<String, ItemStack[]> returnMap = new HashMap<>();

        ConfigurationSection kitSection = hmd.getConfig().getConfigurationSection("kits");
        if (kitSection != null) {
            for (String s : kitSection.getKeys(false)) {

                ItemStack[] kit = new ItemStack[40];
                List<?> list = kitSection.getList(s);
                for (int i = 0; i < list.size(); i++) {
                    kit[i] = (ItemStack) list.get(i);
                }

                returnMap.put(s, kit);
            }
        }

        return returnMap;

    }

    /**
     * Stores all arenas currently in the memory / arenas list to the config.yml
     */
    private void saveArenas() {
        hmd.getConfig().set("arenas", null);
        if (arenas.size() != 0) {
            for (Arena arena : arenas) {
                Location[] locations = arena.getLocations();
                ConfigurationSection newSection = hmd.getConfig().createSection("arenas." + arena.getName());
                newSection.set("spawn1", locationToString(locations[0]));
                newSection.set("spawn2", locationToString(locations[1]));
            }
        }
        hmd.saveConfig();
    }

    /**
     * Stores all kits currently in the memory / kits list to the config.yml
     */
    private void saveKits() {
        hmd.getConfig().set("kits", null);
        if (kits.keySet().size() != 0) {
            for (Map.Entry<String, ItemStack[]> items : kits.entrySet()) {
                hmd.getConfig().set("kits." + items.getKey(), items.getValue());
            }
        }
        hmd.saveConfig();
    }

    /**
     * Adds an arena to the list, without requiring any sorts of reloads.
     * A location will have to be added
     *
     * @param name name of the arena to add
     * @param loc  default spawn location of the arena
     * @return true if creation was successful, false if not, because the arena exists already
     */
    public boolean addArena(String name, Location loc) {

        for (Arena arena : arenas) { //check if arena with given name exists already
            if (arena.getName().equalsIgnoreCase(name)) return false;
        }

        arenas.add(new Arena(name, new Location[]{loc, loc}, getKit(name)));
        saveArenas();
        return true;
    }

    /**
     * Remove an arena from the list. If a game is running in it, it will still finish
     *
     * @param name name of the arena to remove
     * @return true if arena was removed, false if arena does not exist
     */
    public boolean delArena(String name) {

        for (int i = 0; i < arenas.size(); i++) {
            if (arenas.get(i).getName().equalsIgnoreCase(name)) {
                String arenaName = arenas.get(i).getName();
                arenas.remove(i);
                kits.remove(arenaName);
                saveArenas();
                saveKits();
                return true;
            }
        }
        return false;

    }

    /**
     * Set the spawns of an arena
     *
     * @param arenaName Name of the arena that is being set
     * @param location  Location of the spawn
     * @param setArenaA true if spawnA is being set, false if spawnB
     * @return true if spawn was set, false if creation was unsuccessful
     */
    public boolean setSpawn(String arenaName, Location location, boolean setArenaA) {

        for (Arena arena : arenas) {
            if (arena.getName().equalsIgnoreCase(arenaName)) {
                arena.setLocation(location, setArenaA);
                saveArenas();
                return true;
            }
        }
        return false;
    }

    /**
     * Set the kit of given arena
     *
     * @param arenaName    Name to set the inventory from
     * @param armorContent Armor area of the kit
     * @param content      Content area of the kit
     * @return true if kit could be set, false if arena doesnt exist
     */
    public boolean setKit(String arenaName, ItemStack[] armorContent, ItemStack[] content) {

        for (Arena arena : arenas) {
            if (arena.getName().equalsIgnoreCase(arenaName)) {
                ItemStack[] newKit = ArrayUtils.addAll(armorContent, content);
                kits.put(arena.getName(), newKit);
                arena.setKit(newKit);
                saveKits();
                return true;
            }
        }
        return false;
    }

    /**
     * Return the kit of a given Arena, if it was not set, return empty inventory
     *
     * @param arenaName name of the arena the kit should be returned
     * @return ItemStack[40] where index 0-3 are the armor contents, and 4-39 are the contents
     */
    private ItemStack[] getKit(String arenaName) {
        return kits.containsKey(arenaName) ? kits.get(arenaName) : new ItemStack[40];
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
        return new Location(hmd.getServer().getWorld(toLocationSplit[0]), x, y, z, yaw, pitch);
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
