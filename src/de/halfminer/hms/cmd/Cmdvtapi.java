package de.halfminer.hms.cmd;

import de.halfminer.hms.exception.PlayerNotFoundException;
import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.StatsType;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

@SuppressWarnings("unused")
public class Cmdvtapi extends BaseCommand {

    public Cmdvtapi() {
        this.permission = "hms.default";
    }

    @Override
    @SuppressWarnings("EmptyCatchBlock")
    public void run(CommandSender sender, String label, String[] args) {

        if (!label.equalsIgnoreCase("vtapi") || args.length == 0 || !sender.isOp()) return;

        if (args.length > 0) {

            if (args[0].equalsIgnoreCase("vote")) {

                if (args.length < 2) return;

                OfflinePlayer hasVoted;
                try {
                    hasVoted = hms.getServer().getOfflinePlayer(storage.getUUID(args[1]));
                } catch (PlayerNotFoundException e) {
                    return;
                }

                storage.set("vote." + hasVoted.getUniqueId().toString(), Long.MAX_VALUE);
                storage.incrementStatsInt(hasVoted, StatsType.VOTES, 1);
                hms.getServer().broadcast(Language.getMessagePlaceholderReplace("commandVtapiVoted", true, "%PREFIX%",
                        "Vote", "%PLAYER%", hasVoted.getName()), "hms.default");

                if (hasVoted instanceof Player) {
                    Player playerHasVoted = (Player) hasVoted;
                    playerHasVoted.playSound(playerHasVoted.getLocation(), Sound.NOTE_PLING, 1.0f, 2.0f);
                    String address = playerHasVoted.getAddress().getAddress().toString().replace('.', 'i').substring(1);
                    storage.incrementInt("vote.ip" + address, 1);
                }
            } else if (sender instanceof Player) {

                Player player = (Player) sender;
                ConsoleCommandSender consoleInstance = hms.getServer().getConsoleSender();

                if (args[0].equalsIgnoreCase("takecase")) {

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
                } else if (args[0].equalsIgnoreCase("takehead")) {

                    ItemStack item = player.getItemInHand();
                    if ((item != null) && (item.getType() == Material.SKULL_ITEM)) {

                        SkullMeta skull = (SkullMeta) item.getItemMeta();
                        if (!skull.hasOwner()) {
                            //Not a valid skull
                            hms.getServer().dispatchCommand(consoleInstance, "vt run casino:error " + player.getName());
                            return;
                        }
                        //Get the owner
                        String skullOwner = skull.getOwner();

                        int level = 1;
                        try {
                            UUID uid = storage.getUUID(skullOwner);
                            level = storage.getStatsInt(hms.getServer().getOfflinePlayer(uid), StatsType.SKILL_LEVEL);
                        } catch (PlayerNotFoundException e) {
                        }

                        hms.getServer().dispatchCommand(consoleInstance, "vt setstr temp headname_" + player.getName() + " " + skullOwner);
                        hms.getServer().dispatchCommand(consoleInstance, "vt setint temp headlevel_" + player.getName() + " " + String.valueOf(level));

                        //Remove the skull
                        int amount = item.getAmount();
                        if (amount > 1) item.setAmount(amount - 1);
                        else player.getInventory().setItemInHand(new ItemStack(Material.AIR));
                        //proceed with next step
                        hms.getServer().dispatchCommand(consoleInstance, "vt run casino:roulette " + player.getName());

                    } else hms.getServer().dispatchCommand(consoleInstance, "vt run casino:error " + player.getName());
                }
            }
        }
    }
}
