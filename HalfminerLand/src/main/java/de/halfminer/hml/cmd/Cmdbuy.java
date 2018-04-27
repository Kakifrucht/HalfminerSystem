package de.halfminer.hml.cmd;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.Faction;
import de.halfminer.hml.land.Land;
import de.halfminer.hml.land.contract.AbstractContract;
import de.halfminer.hml.land.contract.BuyContract;
import de.halfminer.hml.land.contract.FreeBuyContract;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.StringArgumentSeparator;
import org.bukkit.Chunk;

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
        Chunk landChunk = landToBuy.getChunk();

        boolean buyAsServer = args.length > 0
                && args[0].equalsIgnoreCase("server")
                && player.hasPermission("hml.cmd.buy.server");

        // check config buy restrictions
        if (!buyAsServer) {

            List<String> worldRestrictions = hml.getConfig().getStringList("buyLimits.worldRestrictions");
            String worldName = landChunk.getWorld().getName();
            for (String minimumCoordinate : worldRestrictions) {

                StringArgumentSeparator separator = new StringArgumentSeparator(minimumCoordinate, ',');
                if (separator.getArgument(0).equals(worldName)) {

                    int minimumCoordinateInt = separator.getArgumentInt(1);

                    // check if world is disabled
                    if (minimumCoordinateInt < 0) {
                        MessageBuilder.create("cmdBuyNotBuyableWorld", hml).sendMessage(player);
                        return;
                    }

                    // check if minimum coordinate requirement is met
                    int chunkMinimumCoordinate = (int) Math.ceil((double) minimumCoordinateInt / 16d);
                    if (landChunk.getX() < chunkMinimumCoordinate
                            && landChunk.getX() > -chunkMinimumCoordinate
                            && landChunk.getZ() < chunkMinimumCoordinate
                            && landChunk.getZ() > -chunkMinimumCoordinate) {

                        MessageBuilder.create("cmdBuyNotBuyableCoordinate", hml)
                                .addPlaceholderReplace("%MINIMUMCOORDS%", String.valueOf(minimumCoordinateInt))
                                .sendMessage(player);
                        return;
                    }

                    break;
                }
            }
        }

        // TEMPORARY FACTION CHECK START - remove dependency from plugin.yml when removing
        Faction factionAt = Board.getInstance().getFactionAt(new FLocation(player.getLocation()));
        if (!factionAt.isWilderness() && !player.equals(factionAt.getFPlayerAdmin().getPlayer())) {
            MessageBuilder.create("&7Land &e>> &cDieses Land kann nur vom Gildenbesitzer gekauft werden", hml)
                    .setDirectString()
                    .sendMessage(player);
            return;
        }
        // TEMPORARY FACTION CHECK END

        // check status
        Land.BuyableStatus status = landToBuy.getBuyableStatus();

        if (status.equals(Land.BuyableStatus.ALREADY_OWNED)) {

            MessageBuilder.create("cmdBuyAlreadyOwned" + (landToBuy.isOwner(player) ? "Self" : ""), hml)
                    .addPlaceholderReplace("%PLAYER%", landToBuy.getOwnerName())
                    .sendMessage(player);
            return;
        }

        if (!buyAsServer) {

            if (status.equals(Land.BuyableStatus.LAND_NOT_BUYABLE)) {
                MessageBuilder.create("cmdBuyNotBuyable", hml).sendMessage(player);
                return;
            }

            if (status.equals(Land.BuyableStatus.OTHER_PLAYERS_ON_LAND)) {
                MessageBuilder.create("cmdBuyNotBuyableNotVacant", hml).sendMessage(player);
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

        int freeLandsMax = hml.getLandStorage().getInt(player.getUniqueId().toString() + ".freetotal");
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
                contract = new FreeBuyContract(player, landToBuy);
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
            MessageBuilder.create("notEnoughMoney", hml)
                    .addPlaceholderReplace("%COST%", String.valueOf(cost))
                    .sendMessage(player);
            return;
        }

        if (contract.canBeFulfilled()) {

            if (landToBuy.isAbandoned() && !landToBuy.isFreeLand()) {
                //TODO remove last cost from storage (new abstraction?)
            }

            // buy land
            contractManager.fulfillContract(contract);
            MessageBuilder.create("cmdBuySuccess" + (buyAsServer ? "AsServer" : ""), hml)
                    .addPlaceholderReplace("%COST%", String.valueOf(cost))
                    .sendMessage(player);

            if (buyAsServer) {
                landToBuy.setServerLand(true);
            }

            if (contract instanceof FreeBuyContract && freeLandsMax != Integer.MAX_VALUE) {
                MessageBuilder.create("cmdBuyFreeLandsLeft", hml)
                        .addPlaceholderReplace("%FREELANDSOWNED%", String.valueOf(freeLandsOwned + 1))
                        .addPlaceholderReplace("%FREELANDSMAX%", String.valueOf(freeLandsMax))
                        .sendMessage(player);
            }

        } else {

            board.showChunkParticles(player, landToBuy);
            contract.setCanBeFulfilled();

            MessageBuilder.create("cmdBuyConfirm", hml)
                    .addPlaceholderReplace("%COST%", String.valueOf(cost))
                    .sendMessage(player);
        }
    }
}
