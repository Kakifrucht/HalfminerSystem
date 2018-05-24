package de.halfminer.hml.cmd;

import de.halfminer.hml.data.LandPlayer;
import de.halfminer.hml.data.LandStorage;
import de.halfminer.hml.land.Land;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Collections;
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
            List<ItemStack> menuSkulls = new ArrayList<>();

            for (Player onlinePlayer : server.getOnlinePlayers()) {

                LandPlayer lPlayer = landStorage.getLandPlayer(onlinePlayer);
                String teleport = lPlayer.getShownTeleport();

                if (teleport != null) {

                    Land teleportLand = board.getLandFromTeleport(teleport);
                    if (teleportLand != null && teleportLand.isOwner(onlinePlayer)) {

                        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
                        Utils.setDisplayName(skull, player.getName());
                        Utils.setItemLore(skull, Collections.singletonList("§a/" + command + " " + teleport));

                        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
                        skullMeta.setOwningPlayer(onlinePlayer);
                        skull.setItemMeta(skullMeta);

                        menuSkulls.add(skull);
                    } else {
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
                menuInventory.setItem(i, menuSkulls.get(i));
            }

            hms.getMenuHandler().openMenu(hml, player, menuInventory, e -> {

                ItemStack clickedItem = e.getCurrentItem();
                if (clickedItem != null && clickedItem.getType().equals(Material.SKULL_ITEM)) {

                    SkullMeta skullMeta = (SkullMeta) clickedItem.getItemMeta();
                    LandPlayer clickedPlayer = landStorage.getLandPlayer(skullMeta.getOwningPlayer());
                    Land teleportLand = board.getLandFromTeleport(clickedPlayer.getShownTeleport());

                    if (teleportLand != null) {
                        player.chat("/" + command + " " + clickedPlayer.getShownTeleport());
                    } else {
                        MessageBuilder.create("cmdLandtpMenuNoLongerAvailable", hml).sendMessage(player);
                    }

                    scheduler.runTask(hml, player::closeInventory);
                }
            });
        }
    }
}
