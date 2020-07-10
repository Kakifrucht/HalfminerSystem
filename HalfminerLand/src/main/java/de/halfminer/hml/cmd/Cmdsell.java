package de.halfminer.hml.cmd;

import de.halfminer.hml.land.Land;
import de.halfminer.hml.land.contract.AbstractContract;
import de.halfminer.hml.land.contract.ForceSellContract;
import de.halfminer.hml.land.contract.SellContract;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.util.Message;

public class Cmdsell extends LandCommand {


    public Cmdsell() {
        super("sell");
    }

    @Override
    public void execute() {

        if (!isPlayer) {
            sendNotAPlayerMessage();
            return;
        }

        boolean hasForceSellPermission = player.hasPermission("hml.cmd.sell.force");
        boolean isForceSell = hasForceSellPermission
                && args.length > 0
                && args[0].equalsIgnoreCase("force");

        Land landToSell = board.getLandAt(player);
        Land.SellableStatus sellableStatus = landToSell.getSellableStatus(player);

        if (sellableStatus.equals(Land.SellableStatus.NO_OWNER)) {
            Message.create("landNotOwned", hml).send(player);
            return;
        }

        SellContract contract = null;
        if (contractManager.hasContract(player)) {
            AbstractContract absContract = contractManager.getContract(player, landToSell);
            if (absContract instanceof SellContract) {
                contract = (SellContract) absContract;
            }

            if (contract instanceof ForceSellContract) {
                isForceSell = true;
            }
        }

        // skip various checks if we force sell
        if (!isForceSell) {

            if (sellableStatus.equals(Land.SellableStatus.NOT_OWNED)) {
                Message.create(hasForceSellPermission ? "cmdSellForceUsage" : "landNotOwned", hml).send(player);
                return;
            }

            if (sellableStatus.equals(Land.SellableStatus.OTHER_PLAYERS_ON_LAND)) {
                Message.create("cmdSellOthersOnLand", hml).send(player);
                return;
            }

            if (sellableStatus.equals(Land.SellableStatus.HAS_TELEPORT)) {
                Message.create("cmdSellHasTeleport", hml)
                        .addPlaceholder("%TELEPORT%", landToSell.getTeleportName())
                        .send(player);
                return;
            }
        }

        boolean isServerLand = landToSell.isServerLand();
        if (contract == null) {
            contract = isForceSell ? new ForceSellContract(player, landToSell) : new SellContract(player, landToSell);
            contractManager.setContract(contract);

            if (isServerLand) {
                contract.setCanBeFulfilled();
            }
        }

        boolean isFreeLand = landToSell.isFreeLand();
        if (contract.canBeFulfilled()) {

            // notify player if land was force sold (only if online)
            HalfminerPlayer landOwner = landToSell.getOwner();
            if (isForceSell && !landToSell.isServerLand() && landOwner.getBase().isOnline()) {
                Message.create("cmdSellForceNotify", hml)
                        .addPlaceholder("%FORCINGPLAYER%", player.getName())
                        .addPlaceholder("%LAND%", landToSell.toString())
                        .send(landOwner.getBase().getPlayer());
            }

            // sell land
            contractManager.fulfillContract(contract);

            String localeKey = "cmdSellSuccess";
            if (isServerLand) {
                localeKey += "Server";
            } else if (isForceSell) {
                localeKey += "Force";
            } else if (isFreeLand) {
                localeKey += "Free";
            }

            Message.create(localeKey, hml)
                    .addPlaceholder("%COST%", contract.getCost())
                    .addPlaceholder("%LANDOWNER%", landOwner.getName())
                    .send(player);

        } else {
            board.showChunkParticles(player, landToSell);
            contract.setCanBeFulfilled();

            Message.create("cmdSellConfirm" + (isFreeLand ? "Free" : ""), hml)
                    .addPlaceholder("%COST%", contract.getCost())
                    .send(player);
        }
    }
}
