package de.halfminer.hml.land.contract;

import de.halfminer.hml.land.Land;
import de.halfminer.hms.util.Utils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class BuyContract extends AbstractContract {

    private final boolean isFreeBuy;
    private final double cost;


    public BuyContract(Player buyingPlayer, Land landToBuy) {
        super(buyingPlayer, landToBuy);

        isFreeBuy = true;
        cost = 0d;
    }

    public BuyContract(Player buyingPlayer, Land landToBuy, int amountOfLandOwned) {
        super(buyingPlayer, landToBuy);

        this.isFreeBuy = false;

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

    public boolean isFreeBuy() {
        return isFreeBuy;
    }

    @Override
    void fulfill(Land land) {

        if (!isFreeBuy) {
            hms.getHooksHandler().addMoney(player, -getCost());
            landStorage.getLandPlayer(player).addLandCost(cost);
        }

        land.setOwner(hms.getStorageHandler().getPlayer(player));
        land.setFreeLand(isFreeBuy);

        hml.getLogger().info(player.getName() + " paid $" + cost + " to buy the land at [" + land + "]");
    }

    @Override
    public double getCost() {
        return cost;
    }
}
