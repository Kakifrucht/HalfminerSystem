package de.halfminer.hml.cmd;

import de.halfminer.hml.land.Land;
import de.halfminer.hms.handler.menu.MenuClickHandler;
import de.halfminer.hms.handler.menu.MenuContainer;
import de.halfminer.hms.handler.menu.MenuCreator;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.handler.storage.PlayerNotFoundException;
import de.halfminer.hms.util.Message;
import de.halfminer.hms.util.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
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
            Message.create("noLandOwned" + (isLookup ? "Player" : ""), hml)
                    .addPlaceholder("%PLAYER%", name)
                    .send(sender);
            return;
        }

        // move lands with teleport to front
        List<Land> sortedLandList = ownedLands
                .stream()
                .filter(Land::hasTeleportLocation)
                .collect(Collectors.toList());

        ownedLands.removeAll(sortedLandList);
        sortedLandList.addAll(ownedLands);

        boolean isAbandoned = false;
        for (Land land : sortedLandList) {
            if (land.isAbandoned()) {
                isAbandoned = true;
                break;
            }
        }

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
                        material = Material.END_STONE;
                        break;
                    default:
                        material = Material.GRASS;
                }

                ItemStack landItem = new ItemStack(material);

                String localeKey = "cmdListMenuLandEntry" + (ownedLand.hasTeleportLocation() ? "Teleport" : "");
                String teleportString = ownedLand.hasTeleportLocation() ? ownedLand.getTeleportName() : "";
                Message localeBuilder = Message.create(localeKey, hml)
                        .togglePrefix()
                        .addPlaceholder("%WORLD%", ownedLand.getWorld().getName())
                        .addPlaceholder("%X%", ownedLand.getXLandCorner())
                        .addPlaceholder("%Z%", ownedLand.getZLandCorner())
                        .addPlaceholder("%TELEPORT%", teleportString);

                Utils.applyLocaleToItemStack(landItem, localeBuilder);
                menuItems[currentMenuIndex++] = landItem;
            }

            String menuTitle = Message.create("cmdListMenuTitle" + (isAbandoned ? "Abandoned" : ""), hml)
                    .togglePrefix()
                    .addPlaceholder("%PLAYER%", name)
                    .returnMessage();

            // teleport to land on inventory click, only with permission
            MenuClickHandler menuClickHandler = (e, rawSlot) -> {

                ItemStack item = e.getCurrentItem();
                if (item != null
                        && !item.getType().equals(Material.AIR)
                        && e.getWhoClicked().hasPermission("hml.cmd.list.teleport")) {

                    Land teleportTo = sortedLandList.get(rawSlot);
                    Block block = teleportTo.getWorld().getHighestBlockAt(teleportTo.getXLandCorner(), teleportTo.getZLandCorner());

                    // look into the land while standing on the corner block
                    Location location = block.getLocation();
                    location.setYaw(-45f);

                    hms.getTeleportHandler()
                            .startTeleport(player, location, () -> board.showChunkParticles(player, teleportTo), null);

                    hms.getMenuHandler().closeMenu(player);
                }
            };

            MenuContainer menuContainer = new MenuContainer(this, player, menuTitle, menuItems, menuClickHandler);
            hms.getMenuHandler().openMenu(menuContainer);

        } else /* sender is not player, show as text */ {

            StringBuilder landListStringBuilder = new StringBuilder();
            for (Land land : sortedLandList) {

                Message toAppendBuilder = Message
                        .create("cmdListAsTextFormat" + (land.hasTeleportLocation() ? "Teleport" : ""), hml)
                        .addPlaceholder("%WORLD%", land.getWorld().getName())
                        .addPlaceholder("%X%", land.getXLandCorner())
                        .addPlaceholder("%Z%", land.getZLandCorner());

                if (land.hasTeleportLocation()) {
                    toAppendBuilder.addPlaceholder("%TELEPORT%", land.getTeleportName());
                }

                landListStringBuilder
                        .append(toAppendBuilder.togglePrefix().returnMessage())
                        .append(" ");
            }

            Message.create("cmdListAsText", hml)
                    .addPlaceholder("%PLAYER%", name)
                    .addPlaceholder("%LANDAMOUNT%", sortedLandList.size())
                    .addPlaceholder("%LANDLIST%", landListStringBuilder.toString().trim())
                    .send(sender);
        }
    }
}
