package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
import de.halfminer.hms.handler.types.DataType;
import de.halfminer.hms.exceptions.PlayerNotFoundException;
import de.halfminer.hms.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

/**
 * - Small features for script integration
 *   - Show titles
 *   - Check if player has room in inv
 *   - Remove head in casino
 *   - Remove case in casino
 *   - Set script vars accordingly
 */
@SuppressWarnings("unused")
public class Cmdhmcapi extends HalfminerCommand {

    @Override
    public void execute() {

        if (args.length == 0 || !sender.isOp()) return;

        if (args[0].equalsIgnoreCase("title") && args.length > 2) {

            Player sendTo = server.getPlayer(args[1]);
            if (sendTo != null) {
                hms.getTitlesHandler().sendTitle(sendTo,
                        Utils.arrayToString(args, 2, false), 0, 50, 10);
            }

        } else if (args[0].equalsIgnoreCase("hasroom") && args.length > 2) {

            Player player = server.getPlayer(args[1]);
            if (player != null) {

                int freeSlotsRequired;
                try {
                    freeSlotsRequired = Integer.decode(args[2]);
                } catch (NumberFormatException e) {
                    freeSlotsRequired = 1;
                }

                setHasRoomBoolean(player.getName(), Utils.hasRoom(player, freeSlotsRequired));

            } else setHasRoomBoolean(args[1], false);

        } else if (isPlayer) {

            ConsoleCommandSender consoleInstance = server.getConsoleSender();

            if (args[0].equalsIgnoreCase("takecase")) {

                ItemStack hand = player.getInventory().getItemInMainHand();
                if (stackNameContains(hand, "kiste")
                        && reduceHandAmountOrRemove(Material.DRAGON_EGG, 1)) {

                    server.dispatchCommand(consoleInstance, "vt setstr temp casename_"
                            + sender.getName() + " " + hand.getItemMeta().getDisplayName());
                    server.dispatchCommand(consoleInstance, "vt run casino:caseopen " + player.getName());

                } else server.dispatchCommand(consoleInstance, "vt run casino:error " + player.getName());

            } else if (args[0].equalsIgnoreCase("takehead")) {

                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand != null && hand.hasItemMeta()
                        && reduceHandAmountOrRemove(Material.SKULL_ITEM, 1)) {

                    SkullMeta skull = (SkullMeta) hand.getItemMeta();

                    if (!skull.hasOwner()) {
                        server.dispatchCommand(consoleInstance, "vt run casino:error " + player.getName());
                        return;
                    }

                    String skullOwner = skull.getOwner();

                    int level;
                    try {
                        level = storage.getPlayer(skullOwner).getInt(DataType.SKILL_LEVEL);
                    } catch (PlayerNotFoundException e) {
                        level = 1;
                    }

                    server.dispatchCommand(consoleInstance,
                            "vt setstr temp headname_" + player.getName() + " " + skullOwner);
                    server.dispatchCommand(consoleInstance,
                            "vt setint temp headlevel_" + player.getName() + " " + String.valueOf(level));
                    server.dispatchCommand(consoleInstance, "vt run casino:roulette " + player.getName());

                } else server.dispatchCommand(consoleInstance, "vt run casino:error " + player.getName());
            } else if (args[0].equalsIgnoreCase("tradeup")) {

                ItemStack hand = player.getInventory().getItemInMainHand();
                if (stackNameContains(hand, "Votekiste")
                        && reduceHandAmountOrRemove(Material.DRAGON_EGG, 4)) {
                    server.dispatchCommand(consoleInstance, "vt run casino:tradeupcases " + player.getName());
                } else {
                    server.dispatchCommand(consoleInstance, "vt run casino:tradeupcaseserror " + player.getName());
                }
            }
        }
    }

    private boolean reduceHandAmountOrRemove(Material toMatch, int reduceBy) {

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || !hand.getType().equals(toMatch))
            return false;

        int amount = hand.getAmount();
        int deducted = amount - reduceBy;
        if (deducted < 0) {
            return false;
        } else if (deducted > 0) {
            hand.setAmount(deducted);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
        return true;
    }

    private boolean stackNameContains(ItemStack toCompare, String toContain) {
        return toCompare != null
                && toCompare.hasItemMeta()
                && toCompare.getItemMeta().hasDisplayName()
                && ChatColor.stripColor(toCompare.getItemMeta().getDisplayName()).contains(toContain);
    }

    private void setHasRoomBoolean(String playerName, boolean value) {
        hmc.getServer().dispatchCommand(hmc.getServer().getConsoleSender(), "vt setbool temp hasroom_"
                        + playerName + " " + String.valueOf(value));
    }
}
