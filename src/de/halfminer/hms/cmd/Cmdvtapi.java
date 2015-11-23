package de.halfminer.hms.cmd;

import de.halfminer.hms.exception.PlayerNotFoundException;
import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.StatsType;
import de.halfminer.hms.util.TitleSender;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

@SuppressWarnings("unused")
public class Cmdvtapi extends BaseCommand {

    @Override
    public void run(CommandSender sender, String label, String[] args) {

        if (!label.equalsIgnoreCase("vtapi") || args.length == 0 || !sender.isOp()) return;

        if (args.length > 0) {

            if (args[0].equalsIgnoreCase("title") && args.length > 2) {

                Player sendTo = hms.getServer().getPlayer(args[1]);
                if (sendTo != null) {
                    TitleSender.sendTitle(sendTo, Language.arrayToString(args, 2, false), 0, 50, 10);
                }

            } else if (sender instanceof Player) {

                Player player = (Player) sender;
                ConsoleCommandSender consoleInstance = hms.getServer().getConsoleSender();

                if (args[0].equalsIgnoreCase("takecase")) {

                    String playername = player.getName();

                    if (player.getItemInHand().getItemMeta().hasDisplayName()) {

                        ItemStack hand = player.getItemInHand();

                        hms.getServer().dispatchCommand(consoleInstance, "vt setstr temp casename_" + sender.getName() + " " + hand.getItemMeta().getDisplayName());
                        int amount = hand.getAmount();
                        if (amount > 1) hand.setAmount(hand.getAmount() - 1);
                        else player.getInventory().setItemInHand(null);
                        hms.getServer().dispatchCommand(consoleInstance, "vt run casino:caseopen " + playername);

                    } else {

                        hms.getServer().dispatchCommand(consoleInstance, "vt run casino:error " + playername);

                    }

                } else if (args[0].equalsIgnoreCase("takehead")) {

                    ItemStack item = player.getItemInHand();
                    if ((item != null) && (item.getType() == Material.SKULL_ITEM)) {

                        SkullMeta skull = (SkullMeta) item.getItemMeta();
                        if (!skull.hasOwner()) {
                            hms.getServer().dispatchCommand(consoleInstance, "vt run casino:error " + player.getName());
                            return;
                        }


                        String skullOwner = skull.getOwner();

                        int level;
                        try {
                            UUID uuid = storage.getUUID(skullOwner);
                            level = storage.getStatsInt(hms.getServer().getOfflinePlayer(uuid), StatsType.SKILL_LEVEL);
                        } catch (PlayerNotFoundException e) {
                            level = 1;
                        }

                        hms.getServer().dispatchCommand(consoleInstance, "vt setstr temp headname_" + player.getName() + " " + skullOwner);
                        hms.getServer().dispatchCommand(consoleInstance, "vt setint temp headlevel_" + player.getName() + " " + String.valueOf(level));

                        //Remove the skull
                        int amount = item.getAmount();
                        if (amount > 1) item.setAmount(amount - 1);
                        else player.getInventory().setItemInHand(null);
                        //proceed with next step
                        hms.getServer().dispatchCommand(consoleInstance, "vt run casino:roulette " + player.getName());

                    } else hms.getServer().dispatchCommand(consoleInstance, "vt run casino:error " + player.getName());
                }
            }
        }
    }
}
