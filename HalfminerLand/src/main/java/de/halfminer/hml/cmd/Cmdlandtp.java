package de.halfminer.hml.cmd;

import de.halfminer.hml.data.LandPlayer;
import de.halfminer.hml.data.LandStorage;
import de.halfminer.hml.data.PinnedTeleport;
import de.halfminer.hml.land.Land;
import de.halfminer.hms.handler.HanMenu;
import de.halfminer.hms.handler.menu.MenuClickHandler;
import de.halfminer.hms.handler.menu.MenuContainer;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Pair;
import de.halfminer.hms.util.Utils;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Cmdlandtp extends LandCommand {

    private static final int OWN_TELEPORT_DELAY_SECONDS = 30;
    private static final int INVENTORY_SLOTS_PER_LINE = 9;


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
            String teleportName = args[0].toLowerCase();
            Land teleportTo = board.getLandFromTeleport(teleportName);
            if (teleportTo != null) {

                if (teleportTo.isAbandoned()) {
                    boolean isStealingEnabled = hml.getConfig().getBoolean("teleport.allowStealingAbandoned", false);
                    MessageBuilder.create("cmdLandtpIsAbandoned" + (isStealingEnabled ? "Steal" : ""), hml)
                            .addPlaceholder("%NAME%", teleportName)
                            .sendMessage(player);
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
            List<Pair<ItemStack, String>> menuEntries = new ArrayList<>();

            List<PinnedTeleport> pinnedTeleports = landStorage.getPinnedTeleportList();
            for (PinnedTeleport pinnedTeleport : pinnedTeleports) {

                ItemStack itemStack;
                if (pinnedTeleport.hasMaterial()) {
                    itemStack = getPinnedItemStack(pinnedTeleport.getMaterial());
                } else {

                    OfflinePlayer teleportOwner = null;
                    HalfminerPlayer hTeleportOwner = pinnedTeleport.getOwner();
                    if (hTeleportOwner != null) {
                        teleportOwner = hTeleportOwner.getBase();
                    }

                    if (teleportOwner == null) {
                        // get default material from config, if land has no owner / is server land
                        String defaultMaterialServerLandString = hml.getConfig().getString("teleport.pinnedTeleportServerLandMaterial");
                        Material material = Material.matchMaterial(defaultMaterialServerLandString);
                        if (material == null) {
                            material = Material.DIRT;
                            hml.getLogger().warning("Invalid default material set in config under 'teleport.pinnedTeleportServerLandMaterial'");
                        }

                        itemStack = getPinnedItemStack(material);
                    } else {
                        itemStack = getSkullItem(teleportOwner);
                    }
                }

                MessageBuilder localeBuilder = getLocaleBuilder("cmdLandtpMenuItemFormatPinned",
                        pinnedTeleport.getOwnerName(), pinnedTeleport.getTeleport());

                Utils.applyLocaleToItemStack(itemStack, localeBuilder);
                menuEntries.add(new Pair<>(itemStack, pinnedTeleport.getTeleport()));
            }

            if (!pinnedTeleports.isEmpty()) {

                ItemStack stackBar = new ItemStack(Material.WHITE_STAINED_GLASS_PANE, 1);
                Utils.setDisplayName(stackBar, " ");
                Pair<ItemStack, String> pairAir = new Pair<>(new ItemStack(Material.AIR), "");
                Pair<ItemStack, String> pairBar = new Pair<>(stackBar, "");

                // fill up line with air if necessary
                while (menuEntries.size() % INVENTORY_SLOTS_PER_LINE != 0) {
                    menuEntries.add(pairAir);
                }

                // add spacer line between pins and playerskulls
                for (int i = 0; i < INVENTORY_SLOTS_PER_LINE; i++) {
                    menuEntries.add(pairBar);
                }
            }

            // add owned teleports to front
            String shownTeleportOfExecutingPlayer = landStorage.getLandPlayer(player).getShownTeleport();
            for (Land land : board.getLandsWithTeleport(player)) {
                boolean isShown = land.getTeleportName().equalsIgnoreCase(shownTeleportOfExecutingPlayer);
                addSkullToCollection(menuEntries, player, land.getTeleportName(), isShown);
            }

            // add shown teleports by online players
            for (Player onlinePlayer : server.getOnlinePlayers()) {

                LandPlayer lPlayer = landStorage.getLandPlayer(onlinePlayer);
                String teleport = lPlayer.getShownTeleport();

                if (teleport != null) {

                    Land teleportLand = board.getLandFromTeleport(teleport);
                    if (teleportLand != null && !onlinePlayer.equals(player) && teleportLand.isOwner(onlinePlayer)) {
                        addSkullToCollection(menuEntries, onlinePlayer, teleport, true);
                    } else if (!onlinePlayer.equals(player)) {
                        lPlayer.setShownTeleport(null);
                    }
                }
            }

            if (menuEntries.isEmpty()) {
                MessageBuilder.create("cmdLandtpMenuEmpty", hml).sendMessage(player);
                return;
            }

            // build menu
            String inventoryTitle = MessageBuilder.returnMessage("cmdLandtpMenuTitle", hml, false);
            ItemStack[] menuItems = new ItemStack[menuEntries.size()];
            for (int i = 0; i < menuEntries.size(); i++) {
                Pair<ItemStack, String> pair = menuEntries.get(i);

                if (pair != null) {
                    menuItems[i] = pair.getLeft();
                }
            }

            HanMenu menuHandler = hms.getMenuHandler();
            MenuClickHandler menuClickHandler = (e, rawSlot) -> {
                ItemStack clickedItem = e.getCurrentItem();
                if (clickedItem != null && !clickedItem.getType().equals(Material.AIR)) {

                    String teleportName = menuEntries.get(rawSlot).getRight();
                    if (!teleportName.isEmpty()) {
                        player.chat("/" + command + " " + teleportName);

                        menuHandler.closeMenu(player);
                    }
                }
            };

            MenuContainer menuContainer = new MenuContainer(hml, player, inventoryTitle, menuItems, menuClickHandler);
            menuHandler.openMenu(menuContainer);
        }
    }

    private void addSkullToCollection(Collection<Pair<ItemStack, String>> collection,
                                      OfflinePlayer owner, String teleportName, boolean isShown) {

        // don't add same teleport twice
        for (Pair<ItemStack, String> itemStackStringPair : collection) {
            if (itemStackStringPair != null) {
                if (itemStackStringPair.getRight().equals(teleportName)) {
                    return;
                }
            }
        }

        ItemStack skull = getSkullItem(owner);

        String localeKey = "cmdLandtpMenuItemFormat" + (isShown ? "" : "NotShown");
        Utils.applyLocaleToItemStack(skull, getLocaleBuilder(localeKey, owner.getName(), teleportName));

        collection.add(new Pair<>(skull, teleportName));
    }

    private ItemStack getSkullItem(OfflinePlayer owner) {

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
        if (owner != null) {
            SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
            skullMeta.setOwningPlayer(owner);
            skull.setItemMeta(skullMeta);
        }

        return skull;
    }

    private ItemStack getPinnedItemStack(Material material) {
        ItemStack itemStack = new ItemStack(material);

        // add durability enchant, hide from item description
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        itemStack.setItemMeta(itemMeta);
        itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);

        return itemStack;
    }

    private MessageBuilder getLocaleBuilder(String localeKey, String ownerName, String teleportName) {
        return MessageBuilder.create(localeKey, hml)
                .togglePrefix()
                .addPlaceholder("%PLAYER%", ownerName)
                .addPlaceholder("%TELEPORT%", teleportName)
                .addPlaceholder("%TELEPORTFRIENDLY%", Utils.makeStringFriendly(teleportName));
    }
}
