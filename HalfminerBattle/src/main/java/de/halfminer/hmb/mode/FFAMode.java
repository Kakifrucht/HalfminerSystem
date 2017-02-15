package de.halfminer.hmb.mode;

import de.halfminer.hmb.arena.FFAArena;
import de.halfminer.hmb.arena.abs.Arena;
import de.halfminer.hmb.enums.BattleModeType;
import de.halfminer.hmb.mode.abs.AbstractMode;
import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.handlers.HanTeleport;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.logging.Level;

/**
 * Implementing free for all games with automatic respawns in kit arenas and killstreaks
 */
@SuppressWarnings("unused")
public class FFAMode extends AbstractMode {

    private int removeAfterDeaths;
    private int removeForMinutes;

    public FFAMode() {
        super(BattleModeType.FFA);
    }

    public int getRemoveAfterDeaths() {
        return removeAfterDeaths;
    }

    public int getRemoveForMinutes() {
        return removeForMinutes;
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) {
            MessageBuilder.create("notAPlayer", "Battle").sendMessage(sender);
            return true;
        }

        if (!sender.hasPermission("hmb.mode.ffa.use")) {
            MessageBuilder.create("noPermission", "Battle").sendMessage(sender);
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            MessageBuilder.create("modeFFAUsage", hmb).sendMessage(sender);
            return true;
        }

        if (pm.hasQueueCooldown(player)) {
            MessageBuilder.create("modeGlobalQueueCooldown", hmb).sendMessage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "join":
                // recheck because ffa does not cause command blockage
                if (pm.isNotIdle(player)) {
                    MessageBuilder.create("modeGlobalNotIdle", hmb).sendMessage(player);
                    return true;
                }

                List<Arena> freeArenas = am.getFreeArenasFromType(type);
                if (freeArenas.size() == 0) {
                    MessageBuilder.create("modeGlobalBattleModeDisabled", hmb).sendMessage(sender);
                } else if (freeArenas.size() == 1) {
                    ((FFAArena) freeArenas.get(0)).addPlayer(player);
                } else {
                    MessageBuilder.create("modeFFAChooseArena", hmb).sendMessage(player);
                    am.sendArenaSelection(player, freeArenas, "/ffa choose ", "");
                }
                break;
            case "leave":
                if (!pm.isInBattle(type, player)) {
                    MessageBuilder.create("modeFFANotInArena", hmb).sendMessage(player);
                    return true;
                }

                teleportWithDelay(player, 2, () -> {
                    ((FFAArena) pm.getArena(player)).removePlayer(player);
                    MessageBuilder.create("modeFFAArenaLeft", hmb).sendMessage(player);
                }, null);
                break;
            case "choose":
                if (args.length > 1) {
                    if (pm.isNotIdle(player)) {
                        MessageBuilder.create("modeGlobalNotIdle", hmb).sendMessage(player);
                        return true;
                    }

                    Arena selected = am.getArena(type, args[1]);
                    if (selected != null) {
                        ((FFAArena) selected).addPlayer(player);
                        break;
                    }
                }
            default:
                MessageBuilder.create("modeFFAUsage", hmb).sendMessage(sender);
        }

        return true;
    }

    @Override
    public boolean onAdminCommand(CommandSender sender, String[] args) {
        return false;
    }

    @Override
    public void onPluginDisable() {
        hmb.getServer().getOnlinePlayers()
                .stream()
                .filter(p -> pm.isInBattle(type, p))
                .forEach(p -> ((FFAArena) pm.getArena(p)).removePlayer(p));
    }

    @Override
    public void onConfigReload() {
        removeAfterDeaths = hmb.getConfig().getInt("battleMode.ffa.removeAfterDeaths", 4);
        removeForMinutes = hmb.getConfig().getInt("battleMode.ffa.removeForMinutes", 3);
    }

    @EventHandler
    public void onDeathRespawn(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (pm.isInBattle(type, p)) {
            FFAArena arena = (FFAArena) pm.getArena(p);
            arena.hasDied(p);
        }
    }

    @EventHandler
    public void onQuitKillAndRemove(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (pm.isInBattle(type, p)) {
            FFAArena arena = (FFAArena) pm.getArena(p);
            arena.removePlayer(p);
            if (!p.isDead()) p.setHealth(0.0d);
            MessageBuilder.create("modeFFALoggedOutLog", hmb)
                    .addPlaceholderReplace("%PLAYER%", p.getName())
                    .addPlaceholderReplace("%ARENA%", arena.getName())
                    .logMessage(Level.INFO);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHitDenyWhileProtected(EntityDamageByEntityEvent e) {

        if (e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            Player attacker = Utils.getDamagerFromEvent(e);

            if (attacker == null
                    || !pm.isInBattle(type, attacker)
                    || !pm.isInBattle(type, p))
                return;

            boolean attackerProtected = ((FFAArena) pm.getArena(p)).hasSpawnProtection(attacker);
            boolean victimProtected = ((FFAArena) pm.getArena(p)).hasSpawnProtection(p);
            e.setCancelled(attackerProtected || victimProtected);
            if (victimProtected) {
                MessageBuilder.create("modeFFASpawnProtected", hmb)
                        .addPlaceholderReplace("%PLAYER%", p.getName())
                        .sendMessage(attacker);
            }
        }
    }

    public void teleportWithDelay(Player toTeleport, int delay,
                                  @Nullable Runnable runIfSuccess, @Nullable Runnable runIfCancelled) {
        ((HanTeleport) HalfminerSystem.getInstance().getHandler(HandlerType.TELEPORT))
                .startTeleport(toTeleport, toTeleport.getLocation(), delay, false, runIfSuccess, runIfCancelled);
    }
}
