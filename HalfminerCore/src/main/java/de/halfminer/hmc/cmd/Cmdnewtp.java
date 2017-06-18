package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
import de.halfminer.hms.handler.storage.DataType;
import de.halfminer.hms.handler.HanTeleport;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;
import java.util.logging.Level;

/**
 * - Teleport to random location
 * - Set min/max x/z values
 * - Checks for safe teleport location
 * - Sets home automatically
 * - Gives some information about the server via chat and bossbar
 */
@SuppressWarnings("unused")
public class Cmdnewtp extends HalfminerCommand {

    public Cmdnewtp() {
        this.permission = "hmc.newtp";
    }

    @Override
    public void execute() {

        if (!isPlayer) {
            sendNotAPlayerMessage("Newtp");
            return;
        }

        final HanTeleport tp = hms.getTeleportHandler();
        final HalfminerPlayer hPlayer = storage.getPlayer(player);

        if (tp.hasPendingTeleport(player, true)) return;

        if (hPlayer.getBoolean(DataType.NEWTP_USED)){
            MessageBuilder.create("cmdNewtpAlreadyUsed", hmc, "Newtp").sendMessage(player);
            return;
        }

        MessageBuilder.create("cmdNewtpStart", hmc, "Newtp").sendMessage(player);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 160, 127));
        player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 160, 127));

        Random rnd = new Random();

        int boundMin = hmc.getConfig().getInt("command.newtp.boundMin", 1000);
        int boundMax = hmc.getConfig().getInt("command.newtp.boundMax", 10000) - boundMin;
        int iterations = 10;

        World world = server.getWorlds().get(0);
        Block block;

        do {
            int x = rnd.nextInt(boundMax) + boundMin;
            int z = rnd.nextInt(boundMax) + boundMin;
            if (rnd.nextBoolean()) x = -x;
            if (rnd.nextBoolean()) z = -z;
            int y = world.getHighestBlockAt(x, z).getLocation().getBlockY();
            block = world.getBlockAt(x, y - 1, z);
        }
        while (--iterations > 0 && block.getType() == Material.WATER || block.getType() == Material.LAVA
                || block.getType() == Material.STATIONARY_WATER || block.getType() == Material.STATIONARY_LAVA);

        final Location loc = block.getLocation();
        loc.setY(block.getLocation().getBlockY() + 1);
        loc.setYaw(player.getLocation().getYaw());
        loc.setPitch(player.getLocation().getPitch());

        tp.startTeleport(player, loc, 5, true, () -> {

            hPlayer.set(DataType.NEWTP_USED, true);

            server.dispatchCommand(player, "sethome newtp");

            for (int i = 0; i < 100; i++) player.sendMessage("");
            MessageBuilder.create("cmdNewtpTpDone", hmc, "Newtp")
                    .addPlaceholderReplace("%PLAYER%", player.getName())
                    .sendMessage(player);

            MessageBuilder.create("cmdNewtpLog", hmc)
                    .addPlaceholderReplace("%PLAYER%", player.getName())
                    .addPlaceholderReplace("%LOCATION%", Utils.getStringFromLocation(loc))
                    .logMessage(Level.INFO);

            scheduler.runTaskLater(hmc, () -> {
                MessageBuilder.create("cmdNewtpDocumentation", hmc, "Newtp").sendMessage(player);
                hms.getBarHandler().sendBar(player,
                        MessageBuilder.returnMessage("cmdNewtpBossbar", hmc), BarColor.BLUE, BarStyle.SOLID, 50);
            }, 120L);
        }, () -> {

            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.removePotionEffect(PotionEffectType.CONFUSION);
        });
    }
}
