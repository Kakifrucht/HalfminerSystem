package de.halfminer.hml.data;

import de.halfminer.hms.handler.HanStorage;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class LandPlayer {

    private static final String LAST_COST_LIST_PATH = "previousCostList";
    private static final String FREE_LANDS_PATH = "freetotal";
    private static final String SHOWN_TELEPORT_PATH = "shownTeleport";

    private final ConfigurationSection dataSection;


    LandPlayer(HanStorage landStorage, HalfminerPlayer halfminerPlayer) {
        this.dataSection = landStorage.getConfigurationSection(halfminerPlayer.getUniqueId().toString());
    }

    public double getHighestCost() {

        List<String> previousCostList = getCostListFromStorage();

        double highestCost = 0d;
        for (String costString : previousCostList) {
            double cost = Double.valueOf(costString);
            if (cost > highestCost) {
                highestCost = cost;
            }
        }

        return highestCost;
    }

    public void addLandCost(double cost) {

        List<String> previousCostList = getCostListFromStorage();
        previousCostList.add(String.valueOf(cost));

        dataSection.set(LAST_COST_LIST_PATH, previousCostList);
    }

    public void removeHighestCost() {

        List<String> previousCostList = getCostListFromStorage();
        if (!previousCostList.isEmpty()) {
            previousCostList.remove(String.valueOf(getHighestCost()));
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

    public String getShownTeleport() {
        return dataSection.getString(SHOWN_TELEPORT_PATH);
    }

    public void setShownTeleport(String teleport) {
        dataSection.set(SHOWN_TELEPORT_PATH, teleport);
    }

    private List<String> getCostListFromStorage() {

        List<String> previousCostList = dataSection.getStringList(LAST_COST_LIST_PATH);

        if (previousCostList == null) {
            previousCostList = new ArrayList<>();
        }

        return previousCostList;
    }
}
