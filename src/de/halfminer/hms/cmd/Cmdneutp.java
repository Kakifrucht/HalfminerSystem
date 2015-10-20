package de.halfminer.hms.cmd;

import de.halfminer.hms.util.Language;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

@SuppressWarnings("unused")
public class Cmdneutp extends BaseCommand {

    private Random rnd = new Random();
    private World world = hms.getServer().getWorlds().get(0);
    private Block block;
    private int boundMin;
    private int boundMax;

    public Cmdneutp() {
        this.permission = "hms.neutp";
    }

    @Override
    public void run(CommandSender sender, Command cmd, String label, String[] args) {

        if (sender instanceof Player) {

            final Player player = (Player) sender;

            if (!storage.getPlayerBoolean(player, "neutp")) {

                storage.setPlayer(player, "neutp", true);

                boundMin = hms.getConfig().getInt("command.neutp.boundMin", 1000);
                boundMax = hms.getConfig().getInt("command.neutp.boundMax", 10000) - boundMin;

                player.sendMessage(Language.getMessagePlaceholderReplace("commandNeutpCountdown", true, "%PREFIX%", "Neutp"));
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 160, 127));
                player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 160, 127));

                hms.getServer().getScheduler().scheduleSyncDelayedTask(hms, new Runnable() {
                    @Override
                    public void run() {

                        int maxIteration = 0;
                        do {
                            maxIteration++;
                            int x = rnd.nextInt(boundMax) + boundMin;
                            int z = rnd.nextInt(boundMax) + boundMin;
                            if (rnd.nextBoolean()) x = -x;
                            if (rnd.nextBoolean()) z = -z;
                            int y = world.getHighestBlockAt(x, z).getLocation().getBlockY();
                            block = world.getBlockAt(x, y - 1, z);
                        } while (maxIteration < 10 && block.getType() == Material.WATER || block.getType() == Material.LAVA
                                || block.getType() == Material.STATIONARY_WATER || block.getType() == Material.STATIONARY_LAVA);

                        Location loc = block.getLocation();
                        loc.setY(block.getLocation().getBlockY() + 1);
                        player.teleport(loc);

                        hms.getServer().dispatchCommand(player, "sethome neutp");
                        for (int i = 0; i < 100; i++) player.sendMessage("");
                        player.sendMessage(Language.getMessagePlaceholderReplace("commandNeutpTpDone", true, "%PREFIX%", "Neutp",
                                "%PLAYER%", player.getName()));

                        hms.getLogger().info(Language.getMessagePlaceholderReplace("commandNeutpLog", false, "%PLAYER%", player.getName(),
                                "%LOCATION%", Language.getStringFromLocation(loc)));

                        hms.getServer().getScheduler().scheduleSyncDelayedTask(hms, new Runnable() {
                            @Override
                            public void run() {
                                player.sendMessage(Language.getMessagePlaceholderReplace("commandNeutpDocumentation", true, "%PREFIX%", "Neutp"));

                            }
                        }, 60);
                    }
                }, 100);

            } else
                player.sendMessage(Language.getMessagePlaceholderReplace("commandNeutpAlreadyUsed", true, "%PREFIX%", "Neutp"));

        } else sender.sendMessage(Language.getMessage("notAPlayer"));

    }
}
