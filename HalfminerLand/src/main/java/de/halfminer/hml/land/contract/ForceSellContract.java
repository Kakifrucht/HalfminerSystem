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
    void fulfill(Land land) {

        if (!land.isFreeLand()) {
            landStorage.getLandPlayer(owner).removeHighestCost();
        }

        land.setOwner(null);

        hml.getLogger().info(owner.getName() + "' land at ["
                + land + "] was force sold by " + player.getName()
                + ", $" + getCost() + " was not returned to the original owner");
    }
}
