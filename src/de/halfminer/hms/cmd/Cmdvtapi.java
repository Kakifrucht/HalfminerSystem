package de.halfminer.hms.cmd;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

@SuppressWarnings("unused")
public class Cmdvtapi extends BaseCommand {

    public Cmdvtapi() {
        this.permission = "hms.default";
    }

    @Override
    public void run(CommandSender sender, Command cmd, String label, String[] args) {

        if (!label.equalsIgnoreCase("vtapi") || args.length == 0 || !sender.isOp()) return;

        if (sender instanceof Player) {

            Player player = (Player) sender;
            ConsoleCommandSender consoleInstance = hms.getServer().getConsoleSender();
            switch (args[0].toLowerCase()) {
                case "takecase":
                    String playername = player.getName();

                    //Check if the held Item has an Display Name (cases always do)
                    if (player.getItemInHand().getItemMeta().hasDisplayName()) {

                        ItemStack hand = player.getItemInHand();

                        //Take the Item by subtracting one, or replace with air
                        hms.getServer().dispatchCommand(consoleInstance, "vt setstr temp casename_" + sender.getName() + " " + hand.getItemMeta().getDisplayName());
                        int amount = hand.getAmount();
                        if (amount > 1) hand.setAmount(amount - 1);
                        else player.getInventory().setItemInHand(new ItemStack(Material.AIR));
                        //Back to VT
                        hms.getServer().dispatchCommand(consoleInstance, "vt run casino:caseopen " + playername);

                    } else {
                        //If no display name, exit
                        hms.getLogger().info("Das von " + playername + " gehaltene Item ist ungültig");
                        hms.getServer().dispatchCommand(consoleInstance, "vt run casino:error " + playername);
                    }
                    break;
                case "takehead":
                    ItemStack item = player.getItemInHand();
                    if ((item != null) && (item.getType() == Material.SKULL_ITEM)) {

                        SkullMeta skull = (SkullMeta) item.getItemMeta();
                        if (!skull.hasOwner()) {
                            //Not a valid skull
                            hms.getServer().dispatchCommand(consoleInstance, "vt run casino:error " + player.getName());
                            return;
                        }
                        //Set the skull
                        hms.getServer().dispatchCommand(consoleInstance, "vt setstr temp headname_" + player.getName() + " " + skull.getOwner());

                        //Remove the skull
                        int amount = item.getAmount();
                        if (amount > 1) item.setAmount(amount - 1);
                        else player.getInventory().setItemInHand(new ItemStack(Material.AIR));
                        //proceed with next step
                        hms.getServer().dispatchCommand(consoleInstance, "vt run casino:roulette " + player.getName());

                    } else hms.getServer().dispatchCommand(consoleInstance, "vt run casino:error " + player.getName());
                    break;
            }

        } else sender.sendMessage("This command can only be executed as player");
    }
}
