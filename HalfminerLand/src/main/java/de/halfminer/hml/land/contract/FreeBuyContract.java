package de.halfminer.hml.land.contract;

import de.halfminer.hml.land.Land;
import org.bukkit.entity.Player;

public class FreeBuyContract extends BuyContract {


    public FreeBuyContract(Player buyingPlayer, Land landToBuy) {
        super(buyingPlayer, landToBuy, 0);
    }

    @Override
    public void fulfill(Land land) {
        if (canBeFulfilled()) {
            super.fulfill(land);
            land.setFreeLand(true);
            removeLastCostFromStorage(player.getUniqueId());
        }
    }
}
