package de.halfminer.hml.cmd;

import de.halfminer.hml.land.Land;
import de.halfminer.hms.handler.menu.MenuContainer;
import de.halfminer.hms.handler.menu.MenuCreator;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.handler.storage.PlayerNotFoundException;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Cmdlist extends LandCommand implements MenuCreator {


    public Cmdlist() {
        super("list");
    }

    @Override
    protected void execute() {

        boolean isLookup = args.length >= 1 && sender.hasPermission("hml.cmd.list.others");

        if (!isLookup && !isPlayer) {
            sendNotAPlayerMessage();
            return;
        }

        Set<Land> ownedLands;
        String name;
        if (isLookup) {

            if (args[0].equals("-s")) {
                ownedLands = board.getLandsOfServer();
                name = hml.getConfig().getString("serverName", "");
            } else {
                HalfminerPlayer hPlayer;
                try {
                    hPlayer = hms.getStorageHandler().getPlayer(args[0]);
                } catch (PlayerNotFoundException e) {
                    e.sendNotFoundMessage(sender, "Land");
                    return;
                }

                ownedLands = board.getLands(hPlayer);
                name = hPlayer.getName();
            }

        } else {
            ownedLands = board.getLands(player);
            name = player.getName();
        }

        if (ownedLands.isEmpty()) {
            MessageBuilder.create("noLandOwned" + (isLookup ? "Player" : ""), hml)
                    .addPlaceholderReplace("%PLAYER%", name)
                    .sendMessage(sender);
            return;
        }

        // move lands with teleport to front
        List<Land> sortedLandList = ownedLands
                .stream()
                .filter(Land::hasTeleportLocation)
                .collect(Collectors.toList());

        ownedLands.removeAll(sortedLandList);
        sortedLandList.addAll(ownedLands);

        if (isPlayer) {
            ItemStack[] menuItems = new ItemStack[sortedLandList.size()];

            int currentMenuIndex = 0;
            for (Land ownedLand : sortedLandList) {

                Material material;
                switch (ownedLand.getWorld().getEnvironment()) {
                    case NETHER:
                        material = Material.NETHERRACK;
                        break;
                    case THE_END:
                        material = Material.ENDER_STONE;
                        break;
                    default:
                        material = Material.GRASS;
                }

                ItemStack landItem = new ItemStack(material);

                String localeKey = "cmdListMenuLandEntry" + (ownedLand.hasTeleportLocation() ? "Teleport" : "");
                String teleportString = ownedLand.hasTeleportLocation() ? ownedLand.getTeleportName() : "";
                MessageBuilder localeBuilder = MessageBuilder.create(localeKey, hml)
                        .togglePrefix()
                        .addPlaceholderReplace("%WORLD%", ownedLand.getWorld().getName())
                        .addPlaceholderReplace("%X%", String.valueOf(ownedLand.getXLandCorner()))
                        .addPlaceholderReplace("%Z%", String.valueOf(ownedLand.getZLandCorner()))
                        .addPlaceholderReplace("%TELEPORT%", teleportString);

                Utils.applyLocaleToItemStack(landItem, localeBuilder);
                menuItems[currentMenuIndex++] = landItem;
            }

            String menuTitle = MessageBuilder.create("cmdListMenuTitle", hml)
                    .togglePrefix()
                    .addPlaceholderReplace("%PLAYER%", name)
                    .returnMessage();

            MenuContainer menuContainer = new MenuContainer(this, player, menuTitle, menuItems);
            hms.getMenuHandler().openMenu(menuContainer);

        } else /* sender is not player, show as text */ {

            StringBuilder landListStringBuilder = new StringBuilder();
            for (Land land : sortedLandList) {

                MessageBuilder toAppendBuilder = MessageBuilder
                        .create("cmdListAsTextFormat" + (land.hasTeleportLocation() ? "Teleport" : ""), hml)
                        .addPlaceholderReplace("%WORLD%", land.getWorld().getName())
                        .addPlaceholderReplace("%X%", String.valueOf(land.getXLandCorner()))
                        .addPlaceholderReplace("%Z%", String.valueOf(land.getZLandCorner()));

                if (land.hasTeleportLocation()) {
                    toAppendBuilder.addPlaceholderReplace("%TELEPORT%", land.getTeleportName());
                }

                landListStringBuilder
                        .append(toAppendBuilder.togglePrefix().returnMessage())
                        .append(" ");
            }

            MessageBuilder.create("cmdListAsText", hml)
                    .addPlaceholderReplace("%PLAYER%", name)
                    .addPlaceholderReplace("%LANDAMOUNT%", String.valueOf(sortedLandList.size()))
                    .addPlaceholderReplace("%LANDLIST%", landListStringBuilder.toString().trim())
                    .sendMessage(sender);
        }
    }
}
