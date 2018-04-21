package de.halfminer.hml.cmd;

import de.halfminer.hml.land.Land;
import de.halfminer.hml.land.contract.AbstractContract;
import de.halfminer.hml.land.contract.ForceSellContract;
import de.halfminer.hml.land.contract.SellContract;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
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

        boolean hasForceSellPermission = player.hasPermission("hml.cmd.sell.force");
        boolean isForceSell = hasForceSellPermission
                && args.length > 0
                && args[0].equalsIgnoreCase("force");

        Land landToSell = board.getLandAt(player);
        Land.SellableStatus sellableStatus = landToSell.getSellableStatus(player);

        if (sellableStatus.equals(Land.SellableStatus.NO_OWNER)) {
            MessageBuilder.create("landNotOwned", hml).sendMessage(player);
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
                MessageBuilder.create(hasForceSellPermission ? "cmdSellForceUsage" : "landNotOwned", hml).sendMessage(player);
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
        }

        boolean isServerLand = landToSell.isServerLand();
        if (contract == null) {
            contract = isForceSell ? new ForceSellContract(player, landToSell) : new SellContract(player, landToSell);
            contractManager.setContract(contract);

            if (isServerLand) {
                contract.setCanBeFulfilled();
            }
        }

        if ((contract.canBeFulfilled()
                && args.length > 0
                && args[0].equalsIgnoreCase("confirm"))
                || landToSell.isServerLand()) {

            // notify player if land was force sold (only if online)
            HalfminerPlayer landOwner = landToSell.getOwner();
            if (isForceSell && !landToSell.isServerLand() && landOwner.getBase().isOnline()) {
                MessageBuilder.create("cmdSellForceNotify", hml)
                        .addPlaceholderReplace("%FORCINGPLAYER%", player.getName())
                        .addPlaceholderReplace("%LAND%", landToSell.toString())
                        .sendMessage(landOwner.getBase().getPlayer());
            }

            // sell land
            contractManager.fulfillContract(contract);

            String localeKey = isServerLand ? "cmdSellServerSuccess" : (isForceSell ? "cmdSellForceSuccess" : "cmdSellSuccess");
            MessageBuilder.create(localeKey, hml)
                    .addPlaceholderReplace("%COST%", String.valueOf(contract.getCost()))
                    .addPlaceholderReplace("%LANDOWNER%", landOwner.getName())
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
