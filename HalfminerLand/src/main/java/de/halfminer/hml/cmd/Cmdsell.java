package de.halfminer.hml.cmd;

import de.halfminer.hml.land.Land;
import de.halfminer.hml.land.contract.AbstractContract;
import de.halfminer.hml.land.contract.SellContract;
import de.halfminer.hms.util.MessageBuilder;

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

        Land landToSell = board.getLandAt(player);
        Land.SellableStatus sellableStatus = landToSell.getSellableStatus(player.getUniqueId());

        if (sellableStatus.equals(Land.SellableStatus.NOT_OWNED)) {
            MessageBuilder.create("landNotOwned", hml).sendMessage(player);
            return;
        }

        if (sellableStatus.equals(Land.SellableStatus.OTHER_PLAYERS_ON_LAND)) {
            MessageBuilder.create("cmdSellOthersOnLand", hml).sendMessage(player);
            return;
        }

        if (sellableStatus.equals(Land.SellableStatus.HAS_TELEPORT)) {
            MessageBuilder.create("cmdSellHasTeleport", hml)
                    .addPlaceholderReplace("%TELEPORT%", landToSell.getTeleportName())
                    .sendMessage(player);
            return;
        }

        SellContract contract = null;
        if (contractManager.hasContract(player)) {
            AbstractContract absContract = contractManager.getContract(player, landToSell);
            if (absContract instanceof SellContract) {
                contract = (SellContract) absContract;
            }
        }

        if (contract == null) {
            contract = new SellContract(player, landToSell);
            contractManager.setContract(contract);
        }

        if (contract.canBeFulfilled()
                && args.length > 0
                && args[0].equalsIgnoreCase("confirm")) {

            // sell land
            contractManager.fulfillContract(contract);
            MessageBuilder.create("cmdSellSuccess", hml)
                    .addPlaceholderReplace("%COST%", String.valueOf(contract.getCost()))
                    .sendMessage(player);

        } else {
            board.showChunkParticles(player, landToSell);
            contract.setCanBeFulfilled();

            MessageBuilder.create("cmdSellConfirm", hml)
                    .addPlaceholderReplace("%COST%", String.valueOf(contract.getCost()))
                    .sendMessage(player);
        }
    }
}
