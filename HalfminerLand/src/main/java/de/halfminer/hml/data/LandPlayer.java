package de.halfminer.hml.data;

import de.halfminer.hms.handler.HanStorage;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class LandPlayer {

    private static final String LAST_COST_LIST_PATH = "previousCostList";
    private static final String FREE_LANDS_PATH = "freetotal";

    private final ConfigurationSection dataSection;


    LandPlayer(HanStorage landStorage, HalfminerPlayer halfminerPlayer) {
        this.dataSection = landStorage.getConfigurationSection(halfminerPlayer.getUniqueId().toString());
    }

    public double getLastCostFromStorage() {

        List<String> previousCostList = getLastCostList();
        if (previousCostList.isEmpty()) {
            return 0d;
        }

        return Double.valueOf(previousCostList.get(previousCostList.size() - 1));
    }

    public void addCurrentCostToStorage(double cost) {

        List<String> previousCostList = getLastCostList();
        previousCostList.add(String.valueOf(cost));

        dataSection.set(LAST_COST_LIST_PATH, previousCostList);
    }

    public void removeLastCostFromStorage() {

        List<String> previousCostList = getLastCostList();
        if (!previousCostList.isEmpty()) {
            previousCostList.remove(previousCostList.size() - 1);
        }

        if (previousCostList.isEmpty()) {
            dataSection.set(LAST_COST_LIST_PATH, null);
        } else {
            dataSection.set(LAST_COST_LIST_PATH, previousCostList);
        }
    }

    public int getFreeLands() {
        return dataSection.getInt(FREE_LANDS_PATH, 0);
    }

    public void setFreeLands(int freeLands) {
        dataSection.set(FREE_LANDS_PATH, freeLands > 0 ? freeLands : null);
    }

    private List<String> getLastCostList() {

        List<String> previousCostList = dataSection.getStringList(LAST_COST_LIST_PATH);

        if (previousCostList == null) {
            previousCostList = new ArrayList<>();
        }

        return previousCostList;
    }
}
