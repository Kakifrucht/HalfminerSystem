package de.halfminer.hml.cmd;

import de.halfminer.hml.HalfminerLand;
import de.halfminer.hml.data.LandPlayer;
import de.halfminer.hml.data.LandStorage;
import de.halfminer.hml.data.PinnedTeleport;
import de.halfminer.hml.land.Land;
import de.halfminer.hms.handler.HanMenu;
import de.halfminer.hms.handler.menu.MenuClickHandler;
import de.halfminer.hms.handler.menu.MenuContainer;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.util.Message;
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

                teleportTo.updateAbandonmentStatus();

                if (teleportTo.isAbandoned()) {
                    boolean isStealingEnabled = hml.getConfig().getBoolean("teleport.allowStealingAbandoned", false);
                    Message.create("cmdLandtpIsAbandoned" + (isStealingEnabled ? "Steal" : ""), hml)
                            .addPlaceholder("%NAME%", teleportName)
                            .send(player);
                } else {

                    if (teleportTo.isOwner(player) && !player.hasPermission("hml.bypass.landtptimer")) {
                        Message.create("cmdLandtpOwnTimer", hml).send(player);
                        hms.getTeleportHandler().startTeleport(player, teleportTo.getTeleportLocation(), OWN_TELEPORT_DELAY_SECONDS);
                    } else {
                        hms.getTeleportHandler().startTeleport(player, teleportTo.getTeleportLocation());
                    }
                }

            } else {
                Message.create("teleportNotExist", hml).send(player);
            }

        } else {
            openMenu();
        }
    }

    private void openMenu() {
        LandStorage landStorage = hml.getLandStorage();
        List<TeleportEntry> menuEntries = new ArrayList<>();

        for (PinnedTeleport pinnedTeleport : landStorage.getPinnedTeleportList()) {

            TeleportEntry teleportEntry;
            if (pinnedTeleport.hasMaterial()) {
                teleportEntry = new TeleportEntry(pinnedTeleport.getMaterial(), pinnedTeleport.getTeleport(), pinnedTeleport.getOwnerName());
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

                    teleportEntry = new TeleportEntry(material, pinnedTeleport.getTeleport(), pinnedTeleport.getOwnerName());
                } else {
                    teleportEntry = new TeleportEntry(teleportOwner, pinnedTeleport.getTeleport(), true);
                }
            }

            menuEntries.add(teleportEntry);
        }

        if (!menuEntries.isEmpty()) {

            // fill up line with air if necessary
            while (menuEntries.size() % INVENTORY_SLOTS_PER_LINE != 0) {
                menuEntries.add(new TeleportEntry(true));
            }

            // add spacer line between pins and playerskulls
            for (int i = 0; i < INVENTORY_SLOTS_PER_LINE; i++) {
                menuEntries.add(new TeleportEntry(false));
            }
        }

        // add owned teleports to front
        String shownTeleport = landStorage.getLandPlayer(player).getShownTeleport();
        for (Land land : board.getLandsWithTeleport(player)) {
            boolean isShown = !land.getTeleportName().equalsIgnoreCase(shownTeleport);
            if (!containsTeleport(menuEntries, land.getTeleportName())) {
                menuEntries.add(new TeleportEntry(player, land.getTeleportName(), false, isShown));
            }
        }

        // add shown teleports of online players
        for (Player onlinePlayer : server.getOnlinePlayers()) {

            LandPlayer lPlayer = landStorage.getLandPlayer(onlinePlayer);
            String teleportName = lPlayer.getShownTeleport();

            if (teleportName != null) {

                Land teleportLand = board.getLandFromTeleport(teleportName);
                if (!onlinePlayer.equals(player)) {
                    lPlayer.setShownTeleport(null);
                } else if (teleportLand != null
                        && teleportLand.isOwner(onlinePlayer)
                        && !containsTeleport(menuEntries, teleportName)) {

                    menuEntries.add(new TeleportEntry(onlinePlayer, teleportName, false));
                }
            }
        }

        if (menuEntries.isEmpty()) {
            Message.create("cmdLandtpMenuEmpty", hml).send(player);
            return;
        }

        hms.getMenuHandler().openMenu(createMenuContainer(menuEntries));
    }

    private boolean containsTeleport(List<TeleportEntry> list, String teleport) {
        return list.stream().anyMatch(teleportEntry -> teleportEntry.getTeleportName().equalsIgnoreCase(teleport));
    }

    private MenuContainer createMenuContainer(List<TeleportEntry> menuEntries) {

        String inventoryTitle = Message.returnMessage("cmdLandtpMenuTitle", hml, false);
        ItemStack[] menuItems = new ItemStack[menuEntries.size()];
        for (int i = 0; i < menuEntries.size(); i++) {
            menuItems[i] = menuEntries.get(i).getMenuEntry();
        }

        HanMenu menuHandler = hms.getMenuHandler();
        MenuClickHandler menuClickHandler = (e, rawSlot) -> {
            ItemStack clickedItem = e.getCurrentItem();
            if (clickedItem != null && !clickedItem.getType().equals(Material.AIR)) {

                String teleportName = menuEntries.get(rawSlot).getTeleportName();
                if (!teleportName.isEmpty()) {
                    player.chat("/" + command + " " + teleportName);
                    menuHandler.closeMenu(player);
                }
            }
        };

        return new MenuContainer(hml, player, inventoryTitle, menuItems, menuClickHandler);
    }


    private static class TeleportEntry {

        private final Material material;
        private final OfflinePlayer owner;

        private final String teleportName;
        private final String teleportOwner;
        private final boolean pinned;
        private final boolean notShown;


        TeleportEntry(Material material, String teleportName, String teleportOwner) {
            this.material = material;
            this.owner = null;

            this.teleportName = teleportName;
            this.teleportOwner = teleportOwner;
            this.pinned = true;
            this.notShown = false;
        }

        TeleportEntry(OfflinePlayer owner, String teleportName, boolean pinned) {
            this(owner, teleportName, pinned, false);
        }

        TeleportEntry(OfflinePlayer owner, String teleportName, boolean pinned, boolean notShown) {
            this.material = null;
            this.owner = owner;

            this.teleportName = teleportName;
            this.teleportOwner = owner.getName();
            this.pinned = pinned;
            this.notShown = notShown;
        }

        TeleportEntry(boolean air) {
            this.material = air ? Material.AIR : Material.WHITE_STAINED_GLASS_PANE;
            this.owner = null;

            this.teleportName = "";
            this.teleportOwner = "";
            this.pinned = false;
            this.notShown = false;
        }

        private String getTeleportName() {
            return teleportName;
        }

        private ItemStack getMenuEntry() {

            /* expected itemstacks:
                - Material -> Assume we are pinned, apply Durability/Hideenchants
                - Spacer -> Use given Material, remove item name, don't apply locale
                - Playerskull -> Use player skull, set to current owner
             */
            ItemStack itemStack = null;
            if (material != null) {

                if (!teleportName.isEmpty()) { // pinned teleport with custom material
                    itemStack = new ItemStack(material);

                    // add durability enchant, hide from item description
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    itemStack.setItemMeta(itemMeta);
                    itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
                } else { // spacer
                    itemStack = new ItemStack(material, 1);
                    if (!material.equals(Material.AIR)) {
                        Utils.setDisplayName(itemStack, " ");
                    }

                    return itemStack;
                }

            } else if (owner != null) { // playerskull
                itemStack = new ItemStack(Material.PLAYER_HEAD, 1);

                // don't resolve skulls of offline players, as it hogs the main thread
                if (owner.isOnline()) {
                    SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
                    skullMeta.setOwningPlayer(owner);
                    itemStack.setItemMeta(skullMeta);
                }
            }

            String localeSuffix = (pinned ? "Pinned" : (notShown ? "NotShown" : ""));
            Message localeBuilder = getLocaleBuilder("cmdLandtpMenuItemFormat" + localeSuffix, teleportOwner, teleportName);
            Utils.applyLocaleToItemStack(itemStack, localeBuilder);

            return itemStack;
        }

        private Message getLocaleBuilder(String localeKey, String ownerName, String teleportName) {
            return Message.create(localeKey, HalfminerLand.getInstance())
                    .togglePrefix()
                    .addPlaceholder("%PLAYER%", ownerName)
                    .addPlaceholder("%TELEPORT%", teleportName)
                    .addPlaceholder("%TELEPORTFRIENDLY%", Utils.makeStringFriendly(teleportName));
        }
    }
}
