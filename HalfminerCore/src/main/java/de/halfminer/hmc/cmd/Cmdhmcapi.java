package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
import de.halfminer.hms.enums.DataType;
import de.halfminer.hms.exception.PlayerNotFoundException;
import de.halfminer.hms.util.Utils;
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

                if (hand != null && hand.getType().equals(Material.DRAGON_EGG) && hand.getItemMeta().hasDisplayName()) {

                    server.dispatchCommand(consoleInstance, "vt setstr temp casename_"
                            + sender.getName() + " " + hand.getItemMeta().getDisplayName());
                    int amount = hand.getAmount();
                    if (amount > 1) hand.setAmount(hand.getAmount() - 1);
                    else player.getInventory().setItemInMainHand(null);
                    server.dispatchCommand(consoleInstance, "vt run casino:caseopen " + player.getName());

                } else server.dispatchCommand(consoleInstance, "vt run casino:error " + player.getName());

            } else if (args[0].equalsIgnoreCase("takehead")) {

                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand != null && hand.getType().equals(Material.SKULL_ITEM)) {

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

                    // Remove the skull
                    int amount = hand.getAmount();
                    if (amount > 1) hand.setAmount(amount - 1);
                    else player.getInventory().setItemInMainHand(null);
                    // proceed with next step
                    server.dispatchCommand(consoleInstance, "vt run casino:roulette " + player.getName());

                } else server.dispatchCommand(consoleInstance, "vt run casino:error " + player.getName());
            }
        }
    }

    private void setHasRoomBoolean(String playerName, boolean value) {
        hmc.getServer().dispatchCommand(hmc.getServer().getConsoleSender(), "vt setbool temp hasroom_"
                        + playerName + " " + String.valueOf(value));
    }
}
