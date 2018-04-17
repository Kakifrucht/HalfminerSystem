package de.halfminer.hml.land.contract;

import de.halfminer.hml.land.Land;
import de.halfminer.hms.util.Utils;
import org.bukkit.entity.Player;

public class SellContract extends AbstractContract {

    private final double cost;


    public SellContract(Player sellingPlayer, Land landToSell) {
        super(sellingPlayer, landToSell);
        double sellRefundMultiplier = hml.getConfig().getDouble("priceFormula.sellRefundMultiplier", .8d);
        this.cost = landToSell.isFreeLand() ? 0 : Utils.roundDouble(getLastCostFromStorage() * sellRefundMultiplier);
    }

    @Override
    public void fulfill(Land land) {
        if (canBeFulfilled) {

            boolean isFreeLand = land.isFreeLand();
            land.setOwner(null);

            if (cost != 0d) {
                hms.getHooksHandler().addMoney(player, cost);
            }

            if (!isFreeLand) {
                removeLastCostFromStorage(player.getUniqueId());
                hml.getLogger().info(player.getName() + " received $" + cost + " for selling land at [" + land + "]");
            } else {
                hml.getLogger().info(player.getName() + " sold his free land at [" + land);
            }
        }
    }

    @Override
    public double getCost() {
        return cost;
    }
}
