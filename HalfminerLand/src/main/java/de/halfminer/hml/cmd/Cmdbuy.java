package de.halfminer.hml.cmd;

import de.halfminer.hml.land.Land;
import de.halfminer.hml.land.contract.AbstractContract;
import de.halfminer.hml.land.contract.BuyContract;
import de.halfminer.hms.util.Message;
import de.halfminer.hms.util.StringArgumentSeparator;

import java.util.List;

public class Cmdbuy extends LandCommand {


    public Cmdbuy() {
        super("buy");
    }

    @Override
    public void execute() {

        if (!isPlayer) {
            sendNotAPlayerMessage();
            return;
        }

        Land landToBuy = board.getLandAt(player);

        boolean buyAsServer = args.length > 0
                && args[0].equalsIgnoreCase("server")
                && player.hasPermission("hml.cmd.buy.server");

        // check config buy restrictions
        if (!buyAsServer) {

            List<String> worldRestrictions = hml.getConfig().getStringList("buyLimits.worldRestrictions");
            String worldName = landToBuy.getWorld().getName();
            for (String minimumCoordinate : worldRestrictions) {

                StringArgumentSeparator separator = new StringArgumentSeparator(minimumCoordinate, ',');
                if (separator.getArgument(0).equals(worldName)) {

                    int minimumCoordinateInt = separator.getArgumentInt(1);

                    // check if world is disabled
                    if (minimumCoordinateInt < 0 && !player.hasPermission("hml.bypass.buydisabledworld")) {
                        Message.create("cmdBuyNotBuyableWorld", hml).send(player);
                        return;
                    }

                    if (!player.hasPermission("hml.bypass.minimumcoordinates")) {

                        // check if minimum coordinate requirement is met
                        if (landToBuy.getXLandCorner() < minimumCoordinateInt
                                && landToBuy.getXLandCorner() > -minimumCoordinateInt
                                && landToBuy.getZLandCorner() < minimumCoordinateInt
                                && landToBuy.getZLandCorner() > -minimumCoordinateInt) {

                            Message.create("cmdBuyNotBuyableCoordinate", hml)
                                    .addPlaceholder("%MINIMUMCOORDS%", minimumCoordinateInt)
                                    .send(player);
                            return;
                        }
                    }

                    break;
                }
            }
        }

        // check status
        Land.BuyableStatus status = landToBuy.getBuyableStatus();

        if (status.equals(Land.BuyableStatus.ALREADY_OWNED)) {

            Message.create("cmdBuyAlreadyOwned" + (landToBuy.isOwner(player) ? "Self" : ""), hml)
                    .addPlaceholder("%PLAYER%", landToBuy.getOwnerName())
                    .send(player);
            return;
        }

        if (!buyAsServer) {

            if (status.equals(Land.BuyableStatus.LAND_NOT_BUYABLE)) {
                Message.create("cmdBuyNotBuyable", hml).send(player);
                return;
            }

            if (status.equals(Land.BuyableStatus.OTHER_PLAYERS_ON_LAND)) {
                Message.create("cmdBuyNotBuyableNotVacant", hml).send(player);
                return;
            }
        }

        // get/create contract
        BuyContract contract = null;
        if (contractManager.hasContract(player)) {
            AbstractContract absContract = contractManager.getContract(player, landToBuy);
            if (absContract instanceof BuyContract) {
                contract = (BuyContract) absContract;
            }
        }

        int freeLandsMax = hml.getLandStorage().getLandPlayer(player).getFreeLands();
        if (buyAsServer || player.hasPermission("hml.cmd.buy.free")) {
            freeLandsMax = Integer.MAX_VALUE;
        }

        int freeLandsOwned = 0;
        int paidLandsOwned = 0;
        for (Land land : board.getLands(player)) {
            if (land.isFreeLand()) {
                freeLandsOwned++;
            } else {
                paidLandsOwned++;
            }
        }

        if (contract == null) {

            if (freeLandsOwned < freeLandsMax) {
                contract = new BuyContract(player, landToBuy);
            } else {
                contract = new BuyContract(player, landToBuy, paidLandsOwned);
            }

            if (buyAsServer) {
                contract.setCanBeFulfilled();
            }

            contractManager.setContract(contract);
        }

        // check money
        double money = hms.getHooksHandler().getMoney(player);
        double cost = contract.getCost();
        if (cost > money) {
            Message.create("notEnoughMoney", hml)
                    .addPlaceholder("%COST%", cost)
                    .send(player);
            return;
        }

        if (contract.canBeFulfilled()) {

            // if we are taking the land from another player, remove the last paid amount from their history
            if (landToBuy.isAbandoned() && !landToBuy.isFreeLand()) {
                hml.getLandStorage().getLandPlayer(landToBuy.getOwner()).removeHighestCost();
            }

            if (landToBuy.hasTeleportLocation()) {
                landToBuy.removeTeleport();
            }

            // buy land
            contractManager.fulfillContract(contract);
            String messageKey = "cmdBuySuccess" + (buyAsServer ? "AsServer" : (contract.isFreeBuy() ? "Free" : ""));
            Message.create(messageKey, hml)
                    .addPlaceholder("%COST%", cost)
                    .addPlaceholder("%FREELANDSOWNED%", freeLandsOwned + 1)
                    .addPlaceholder("%FREELANDSMAX%", freeLandsMax == Integer.MAX_VALUE ? "-" : freeLandsMax)
                    .addPlaceholder("%DAYSUNTILABANDONED%", hml.getConfig().getInt("landAbandonedAfterDays", 21))
                    .send(player);

            if (buyAsServer) {
                landToBuy.setServerLand(true);
            }

        } else {

            board.showChunkParticles(player, landToBuy);
            contract.setCanBeFulfilled();

            Message.create("cmdBuyConfirm" + (contract.isFreeBuy() ? "Free" : ""), hml)
                    .addPlaceholder("%COST%", cost)
                    .send(player);
        }
    }
}
