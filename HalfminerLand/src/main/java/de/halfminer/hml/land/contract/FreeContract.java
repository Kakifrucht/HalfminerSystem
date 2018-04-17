package de.halfminer.hml.land.contract;

import de.halfminer.hml.land.Land;
import org.bukkit.entity.Player;

public class FreeContract extends BuyContract {


    public FreeContract(Player buyingPlayer, Land landToBuy) {
        super(buyingPlayer, landToBuy, 0);
    }

    @Override
    public void fulfill(Land land) {
        super.fulfill(land);
        land.setFreeLand(true);
        removeLastCostFromStorage(player.getUniqueId());
    }

    @Override
    public boolean canBeFulfilled() {
        return true;
    }
}
