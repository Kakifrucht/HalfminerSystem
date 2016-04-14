package de.halfminer.hms.cmd;

import de.halfminer.hms.enums.DataType;
import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.exception.PlayerNotFoundException;
import de.halfminer.hms.handlers.HanTitles;
import de.halfminer.hms.util.Language;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

/**
 * - Small features for VariableTriggers
 * - Remove head in casino
 * - Remove case in casino
 * - Set VariableTriggers vars accordingly
 */
@SuppressWarnings("unused")
public class Cmdvtapi extends HalfminerCommand {

    @Override
    public void run(CommandSender sender, String label, String[] args) {

        if (args.length == 0 || !sender.isOp()) return;

        if (args.length > 0) {

            if (args[0].equalsIgnoreCase("title") && args.length > 2) {

                Player sendTo = server.getPlayer(args[1]);
                if (sendTo != null) {
                    ((HanTitles) hms.getHandler(HandlerType.TITLES)).sendTitle(sendTo,
                            Language.arrayToString(args, 2, false), 0, 50, 10);
                }

            } else if (sender instanceof Player) {

                Player player = (Player) sender;
                ConsoleCommandSender consoleInstance = server.getConsoleSender();

                if (args[0].equalsIgnoreCase("takecase")) {

                    String playername = player.getName();

                    if (player.getInventory().getItemInMainHand().getItemMeta().hasDisplayName()) {

                        ItemStack hand = player.getInventory().getItemInMainHand();

                        server.dispatchCommand(consoleInstance, "vt setstr temp casename_" + sender.getName() + " " + hand.getItemMeta().getDisplayName());
                        int amount = hand.getAmount();
                        if (amount > 1) hand.setAmount(hand.getAmount() - 1);
                        else player.getInventory().setItemInMainHand(null);
                        server.dispatchCommand(consoleInstance, "vt run casino:caseopen " + playername);

                    } else {

                        server.dispatchCommand(consoleInstance, "vt run casino:error " + playername);
                    }

                } else if (args[0].equalsIgnoreCase("takehead")) {

                    ItemStack item = player.getInventory().getItemInMainHand();
                    if ((item != null) && (item.getType() == Material.SKULL_ITEM)) {

                        SkullMeta skull = (SkullMeta) item.getItemMeta();
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

                        server.dispatchCommand(consoleInstance, "vt setstr temp headname_" + player.getName() + " " + skullOwner);
                        server.dispatchCommand(consoleInstance, "vt setint temp headlevel_" + player.getName() + " " + String.valueOf(level));

                        // Remove the skull
                        int amount = item.getAmount();
                        if (amount > 1) item.setAmount(amount - 1);
                        else player.getInventory().setItemInMainHand(null);
                        // proceed with next step
                        server.dispatchCommand(consoleInstance, "vt run casino:roulette " + player.getName());

                    } else server.dispatchCommand(consoleInstance, "vt run casino:error " + player.getName());
                }
            }
        }
    }
}
