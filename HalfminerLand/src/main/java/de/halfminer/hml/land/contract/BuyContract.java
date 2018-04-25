package de.halfminer.hml.land.contract;

import de.halfminer.hml.land.Land;
import de.halfminer.hms.util.Utils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class BuyContract extends AbstractContract {

    private final double cost;


    public BuyContract(Player buyingPlayer, Land landToBuy, int amountOfLandOwned) {
        super(buyingPlayer, landToBuy);

        ConfigurationSection config = hml.getConfig().getConfigurationSection("priceFormula");

        if (amountOfLandOwned == 0) {
            cost = 0d;
        } else {
            int minPrice = config.getInt("minPrice", 2);
            double factor = config.getDouble("factor", 1.45d);
            double limit = config.getDouble("limit", 200000d);

            cost = Utils.roundDouble(Math.min(limit, minPrice * Math.pow(factor, amountOfLandOwned)));
        }
    }

    @Override
    void fulfill(Land land) {

        if (cost != 0d) {
            hms.getHooksHandler().addMoney(player, -getCost());
        }

        land.setOwner(hms.getStorageHandler().getPlayer(player));
        addCurrentCostToStorage(cost);

        hml.getLogger().info(player.getName() + " paid $" + cost + " to buy the land at [" + land + "]");
    }

    @Override
    public double getCost() {
        return cost;
    }
}
