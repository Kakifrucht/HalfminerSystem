package de.halfminer.hml.cmd;

import de.halfminer.hml.data.LandPlayer;
import de.halfminer.hml.data.LandStorage;
import de.halfminer.hml.land.Land;
import de.halfminer.hms.handler.HanMenu;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Pair;
import de.halfminer.hms.util.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Cmdlandtp extends LandCommand {

    private static final int OWN_TELEPORT_DELAY_SECONDS = 30;
    private static final int MENU_ITEMS_PER_ROW = 9;


    public Cmdlandtp() {
        super("landtp");
    }

    @Override
    public void execute() {

        if (!isPlayer) {
            sendNotAPlayerMessage();
            return;
        }

        if (args.length > 0) {
            Land teleportTo = board.getLandFromTeleport(args[0].toLowerCase());
            if (teleportTo != null) {

                if (teleportTo.isAbandoned()) {
                    MessageBuilder.create("cmdLandtpIsAbandoned", hml).sendMessage(player);
                } else {

                    if (teleportTo.isOwner(player) && !player.hasPermission("hml.bypass.landtptimer")) {
                        MessageBuilder.create("cmdLandtpOwnTimer", hml).sendMessage(player);
                        hms.getTeleportHandler().startTeleport(player, teleportTo.getTeleportLocation(), OWN_TELEPORT_DELAY_SECONDS);
                    } else {
                        hms.getTeleportHandler().startTeleport(player, teleportTo.getTeleportLocation());
                    }
                }

            } else {
                MessageBuilder.create("teleportNotExist", hml).sendMessage(player);
            }

        } else { // open land GUI

            LandStorage landStorage = hml.getLandStorage();
            List<Pair<ItemStack, String>> menuSkulls = new ArrayList<>();

            String shownTeleportOfExecutingPlayer = landStorage.getLandPlayer(player).getShownTeleport();
            for (Land land : board.getLandsWithTeleport(player)) {
                boolean isShown = land.getTeleportName().equalsIgnoreCase(shownTeleportOfExecutingPlayer);
                addItemStackToCollection(menuSkulls, player, land.getTeleportName(), isShown);
            }

            for (Player onlinePlayer : server.getOnlinePlayers()) {

                LandPlayer lPlayer = landStorage.getLandPlayer(onlinePlayer);
                String teleport = lPlayer.getShownTeleport();

                if (teleport != null) {

                    Land teleportLand = board.getLandFromTeleport(teleport);
                    if (teleportLand != null && !onlinePlayer.equals(player) && teleportLand.isOwner(onlinePlayer)) {
                        addItemStackToCollection(menuSkulls, onlinePlayer, teleport, true);
                    } else if (!onlinePlayer.equals(player)) {
                        lPlayer.setShownTeleport(null);
                    }
                }
            }

            if (menuSkulls.isEmpty()) {
                MessageBuilder.create("cmdLandtpMenuEmpty", hml).sendMessage(player);
                return;
            }

            int inventorySize = menuSkulls.size();
            inventorySize += MENU_ITEMS_PER_ROW - (inventorySize % MENU_ITEMS_PER_ROW);

            String inventoryTitle = MessageBuilder.returnMessage("cmdLandtpMenuTitle", hml, false);
            Inventory menuInventory = server.createInventory(player, inventorySize, inventoryTitle);
            for (int i = 0; i < menuSkulls.size(); i++) {
                menuInventory.setItem(i, menuSkulls.get(i).getLeft());
            }

            HanMenu menuHandler = hms.getMenuHandler();
            menuHandler.openMenu(hml, player, menuInventory, e -> {

                ItemStack clickedItem = e.getCurrentItem();
                if (clickedItem != null && clickedItem.getType().equals(Material.SKULL_ITEM)) {

                    String teleportName = menuSkulls.get(e.getRawSlot()).getRight();
                    player.chat("/" + command + " " + teleportName);

                    menuHandler.closeMenu(player);
                }
            });
        }
    }

    private void addItemStackToCollection(Collection<Pair<ItemStack, String>> collection,
                                          Player owner, String teleportName, boolean isShown) {

        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);

        String localeKey = "cmdLandtpMenuItemFormat" + (isShown ? "" : "NotShown");
        MessageBuilder displayAndLoreBuilder = MessageBuilder.create(localeKey, hml)
                .togglePrefix()
                .addPlaceholderReplace("%PLAYER%", owner.getName())
                .addPlaceholderReplace("%TELEPORT%", teleportName)
                .addPlaceholderReplace("%TELEPORTFRIENDLY%", Utils.makeStringFriendly(teleportName));

        Utils.applyLocaleToItemStack(displayAndLoreBuilder, skull);

        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        skullMeta.setOwningPlayer(owner);
        skull.setItemMeta(skullMeta);

        collection.add(new Pair<>(skull, teleportName));
    }
}
