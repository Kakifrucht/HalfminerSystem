package de.halfminer.hms.cmd;

import de.halfminer.hms.enums.DataType;
import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.handlers.HanBossBar;
import de.halfminer.hms.handlers.HanTeleport;
import de.halfminer.hms.util.HalfminerPlayer;
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
public class Cmdneutp extends HalfminerCommand {

    public Cmdneutp() {
        this.permission = "hms.neutp";
    }

    @Override
    public void execute() {

        if (!isPlayer) {
            sendNotAPlayerMessage("Disposal");
            return;
        }

        final HanTeleport tp = (HanTeleport) hms.getHandler(HandlerType.TELEPORT);
        final HalfminerPlayer hPlayer = storage.getPlayer(player);

        if (tp.hasPendingTeleport(player, true)) return;

        if (hPlayer.getBoolean(DataType.NEUTP_USED)){
            MessageBuilder.create(hms, "cmdNeutpAlreadyUsed", "Neutp").sendMessage(player);
            return;
        }

        MessageBuilder.create(hms, "cmdNeutpStart", "Neutp").sendMessage(player);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 160, 127));
        player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 160, 127));

        Random rnd = new Random();

        int boundMin = hms.getConfig().getInt("command.neutp.boundMin", 1000);
        int boundMax = hms.getConfig().getInt("command.neutp.boundMax", 10000) - boundMin;
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

        tp.startTeleport(player, loc, 5, () -> {

            hPlayer.set(DataType.NEUTP_USED, true);

            server.dispatchCommand(player, "sethome neutp");

            for (int i = 0; i < 100; i++) player.sendMessage("");
            MessageBuilder.create(hms, "cmdNeutpTpDone", "Neutp")
                    .addPlaceholderReplace("%PLAYER%", player.getName())
                    .sendMessage(player);

            MessageBuilder.create(hms, "cmdNeutpLog")
                    .addPlaceholderReplace("%PLAYER%", player.getName())
                    .addPlaceholderReplace("%LOCATION%", Utils.getStringFromLocation(loc))
                    .logMessage(Level.INFO);

            scheduler.runTaskLater(hms, () -> {
                MessageBuilder.create(hms, "cmdNeutpDocumentation", "Neutp").sendMessage(player);
                ((HanBossBar) hms.getHandler(HandlerType.BOSS_BAR)).sendBar(player,
                        MessageBuilder.returnMessage(hms, "cmdNeutpBossbar"), BarColor.BLUE, BarStyle.SOLID, 50);
            }, 120L);
        }, () -> {

            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.removePotionEffect(PotionEffectType.CONFUSION);
        });
    }
}
