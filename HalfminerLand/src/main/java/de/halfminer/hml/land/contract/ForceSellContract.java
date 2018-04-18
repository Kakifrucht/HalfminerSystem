package de.halfminer.hml.land.contract;

import de.halfminer.hml.land.Land;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import org.bukkit.entity.Player;

public class ForceSellContract extends SellContract {

    private final HalfminerPlayer owner;


    public ForceSellContract(Player forcingPlayer, Land landToSell) {
        super(forcingPlayer, landToSell);
        this.owner = landToSell.getOwner();
    }

    @Override
    public void fulfill(Land land) {
        if (canBeFulfilled) {

            boolean isFreeLand = land.isFreeLand();
            land.setOwner(null);

            if (!isFreeLand) {
                removeLastCostFromStorage(owner.getUniqueId());
            }

            hml.getLogger().info(owner.getName() + "' land at ["
                    + land + "] was force sold by " + player.getName()
                    + ", $" + getCost() + " was not returned to the original owner");
        }
    }
}
