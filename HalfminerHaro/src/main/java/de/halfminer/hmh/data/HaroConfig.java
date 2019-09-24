package de.halfminer.hmh.data;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/**
 * Reads this plugins config file.
 */
public class HaroConfig {

    private final FileConfiguration configuration;


    HaroConfig(FileConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * @return list with commands that were set to be executed when the game starts
     */
    public List<String> getGameInitCommands() {
        return configuration.getStringList("init.gameCommands");
    }

    /**
     * @return list with commands to be executed when the player gets initialized
     */
    List<String> getPlayerInitCommands() {
        return configuration.getStringList("init.playerCommands");
    }

    /**
     * @return double that represents the maximum distance a player can have from a specified spawn point
     */
    double getMaxSpawnDistance() {
        return configuration.getDouble("init.maxDistanceFromSpawn", 0d);
    }

    /**
     * @return how much play time players should have when the game starts
     */
    public int getTimeAtStart() {
        return configuration.getInt("time.start", 3600);
    }

    /**
     * @return how much play time should be added to a player per day
     */
    public int getTimePerDay() {
        return configuration.getInt("time.day", 3600);
    }

    /**
     * @return the maximum time a player can accumulate
     */
    public int getMaxTime() {
        return configuration.getInt("time.max", 10800);
    }

    /**
     * @return at how many seconds left should the player get a chat warning that the time is running out
     */
    public int getTimeLeftNotify() {
        return configuration.getInt("time.notifyTimeLeft", 30);
    }
}
