package de.halfminer.hml.data;

import de.halfminer.hms.handler.HanStorage;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Class handling the storage for a given player. Constructor takes a {@link HanStorage} instance for reading/writing,
 * and a {@link HalfminerPlayer} that will be associated with this instance.
 */
public class LandPlayer {

    private static final String LAST_COST_LIST_PATH = "previousCostList";
    private static final String FREE_LANDS_PATH = "freetotal";
    private static final String SHOWN_TELEPORT_PATH = "shownTeleport";
    private static final String FLY_TIME_LEFT_PATH = "flyTimeLeft";
    private static final String TITLE_PATH = "title";

    private final ConfigurationSection dataSection;


    LandPlayer(HanStorage landStorage, HalfminerPlayer halfminerPlayer) {
        this.dataSection = landStorage.getConfigurationSection(halfminerPlayer.getUniqueId().toString());
    }

    public double getHighestCost() {

        List<String> previousCostList = getCostListFromStorage();

        double highestCost = 0d;
        for (String costString : previousCostList) {
            double cost = Double.parseDouble(costString);
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

    public int getFlyTimeLeft() {
        return dataSection.getInt(FLY_TIME_LEFT_PATH, 0);
    }

    public void setFlyTimeLeft(int flyTimeLeft) {
        dataSection.set(FLY_TIME_LEFT_PATH, flyTimeLeft > 0 ? flyTimeLeft : null);
    }

    public boolean hasTitle() {
        return dataSection.contains(TITLE_PATH);
    }

    public String getTitle() {
        return dataSection.getString(TITLE_PATH);
    }

    public void setTitle(String title) {
        dataSection.set(TITLE_PATH, title);
    }

    private List<String> getCostListFromStorage() {

        List<String> previousCostList = dataSection.getStringList(LAST_COST_LIST_PATH);

        if (previousCostList == null) {
            previousCostList = new ArrayList<>();
        }

        return previousCostList;
    }
}
