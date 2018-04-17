package de.halfminer.hml.cmd;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.Faction;
import de.halfminer.hml.land.Land;
import de.halfminer.hml.land.contract.AbstractContract;
import de.halfminer.hml.land.contract.BuyContract;
import de.halfminer.hml.land.contract.FreeContract;
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

        // check config buy restrictions
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

            if (player.equals(landToBuy.getOwner().getBase())) {
                MessageBuilder.create("cmdBuyAlreadyOwnedSelf", hml).sendMessage(player);
            } else {
                MessageBuilder.create("cmdBuyAlreadyOwned", hml)
                        .addPlaceholderReplace("%PLAYER%", landToBuy.getOwner().getName())
                        .sendMessage(player);
            }

            return;
        }

        if (status.equals(Land.BuyableStatus.LAND_NOT_BUYABLE)) {
            MessageBuilder.create("cmdBuyNotBuyable", hml).sendMessage(player);
            return;
        }

        if (status.equals(Land.BuyableStatus.OTHER_PLAYERS_ON_LAND)) {
            MessageBuilder.create("cmdBuyNotBuyableNotVacant", hml).sendMessage(player);
            return;
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
        int freeLandsOwned = 0;
        if (contract == null) {

            int paidLandsOwned = 0;
            for (Land land : board.getLands(player.getUniqueId())) {
                if (land.isFreeLand()) {
                    freeLandsOwned++;
                } else {
                    paidLandsOwned++;
                }
            }

            if (freeLandsOwned < freeLandsMax) {
                contract = new FreeContract(player, landToBuy);
            } else {
                contract = new BuyContract(player, landToBuy, paidLandsOwned);
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

        if (contract.canBeFulfilled()
                && args.length > 0
                && args[0].equalsIgnoreCase("confirm")) {

            // buy land
            contractManager.fulfillContract(contract);
            MessageBuilder.create("cmdBuySuccess", hml)
                    .addPlaceholderReplace("%COST%", String.valueOf(cost))
                    .sendMessage(player);

            if (contract instanceof FreeContract) {
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
