package de.halfminer.hmb.mode;

import de.halfminer.hmb.arena.FFAArena;
import de.halfminer.hmb.arena.abs.Arena;
import de.halfminer.hmb.mode.abs.BattleModeType;
import de.halfminer.hmb.mode.abs.AbstractMode;
import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.util.Message;
import de.halfminer.hms.util.Utils;
import org.bukkit.ChatColor;
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
    private boolean setGlowingInArena;

    public FFAMode() {
        super(BattleModeType.FFA);
    }

    public int getRemoveAfterDeaths() {
        return removeAfterDeaths;
    }

    public int getRemoveForMinutes() {
        return removeForMinutes;
    }

    public boolean isSetGlowingInArena() {
        return setGlowingInArena;
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) {
            showList(sender);
            return true;
        }

        if (!sender.hasPermission("hmb.mode.ffa.use")) {
            Message.create("noPermission", "Battle").send(sender);
            return true;
        }

        Player player = (Player) sender;

        List<Arena> freeArenas = am.getFreeArenasFromType(type);
        if (freeArenas.size() == 0) {
            Message.create("modeGlobalBattleModeDisabled", hmb).send(sender);
            return true;
        }

        if (args.length < 1) {
            Message.create("modeFFAUsage", hmb).send(sender);
            return true;
        }

        if (pm.hasQueueCooldown(player)) {
            Message.create("modeGlobalQueueCooldown", hmb).send(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "join":
                // recheck because ffa does not cause command blockage
                if (pm.isNotIdle(player)) {
                    Message.create("modeGlobalNotIdle", hmb).send(player);
                    return true;
                }

                if (args.length > 1) {
                    Arena selected = am.getArena(type, args[1]);
                    if (selected != null && selected.isFree()) {
                        ((FFAArena) selected).addPlayer(player);
                        break;
                    }
                }

                if (freeArenas.size() == 1) {
                    ((FFAArena) freeArenas.get(0)).addPlayer(player);
                } else {
                    Message.create("modeFFAChooseArena", hmb).send(player);
                    am.sendArenaSelection(player, freeArenas, "/ffa join ", "", true);
                }
                break;
            case "leave":
                if (!pm.isInBattle(type, player)) {
                    Message.create("modeFFANotInArena", hmb).send(player);
                    return true;
                }

                boolean hasSpawnProtection = ((FFAArena) pm.getArena(player)).hasSpawnProtection(player);
                teleportWithDelay(player, hasSpawnProtection ? 0 : 2, () -> {
                    ((FFAArena) pm.getArena(player)).removePlayer(player);
                    Message.create("modeFFAArenaLeft", hmb).send(player);
                }, null);
                break;
            case "list":
                showList(sender);
                break;
            default:
                Message.create("modeFFAUsage", hmb).send(sender);
        }

        return true;
    }

    private void showList(CommandSender sendTo) {
        Message.create("modeFFAList", hmb).send(sendTo);
        for (Arena arena : am.getFreeArenasFromType(type)) {
            List<Player> inArena = arena.getPlayersInArena();
            StringBuilder playerString = new StringBuilder(ChatColor.GREEN.toString());
            if (inArena.isEmpty()) {
                playerString = new StringBuilder(Message.returnMessage("modeFFAListEmpty", hmb, false));
            } else {
                for (Player player1 : arena.getPlayersInArena()) {
                    playerString.append(player1.getName()).append("  ");
                }
            }
            Message.create("modeFFAListPlayers", hmb)
                    .togglePrefix()
                    .addPlaceholder("%ARENA%", arena.getName())
                    .addPlaceholder("%PLAYERS%", playerString.toString())
                    .send(sendTo);
        }
    }

    @Override
    public boolean onAdminCommand(CommandSender sender, String[] args) {
        return false;
    }

    @Override
    public void loadConfig() {
        removeAfterDeaths = hmb.getConfig().getInt("battleMode.ffa.removeAfterDeaths", 4);
        removeForMinutes = hmb.getConfig().getInt("battleMode.ffa.removeForMinutes", 3);
        setGlowingInArena = hmb.getConfig().getBoolean("battleMode.ffa.setGlowing", false);
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
            boolean isDead = p.isDead();
            FFAArena arena = (FFAArena) pm.getArena(p);
            arena.removePlayer(p);
            if (!isDead) p.setHealth(0.0d);
            Message.create("modeFFALoggedOutLog", hmb)
                    .addPlaceholder("%PLAYER%", p.getName())
                    .addPlaceholder("%ARENA%", arena.getName())
                    .log(Level.INFO);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHitDenyWhileProtected(EntityDamageByEntityEvent e) {

        if (e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            Player attacker = Utils.getPlayerSourceFromEntity(e.getDamager());

            if (attacker == null
                    || !pm.isInBattle(type, attacker)
                    || !pm.isInBattle(type, p))
                return;

            boolean attackerProtected = ((FFAArena) pm.getArena(p)).hasSpawnProtection(attacker);
            boolean victimProtected = ((FFAArena) pm.getArena(p)).hasSpawnProtection(p);
            e.setCancelled(attackerProtected || victimProtected);
            if (victimProtected) {
                Message.create("modeFFASpawnProtected", hmb)
                        .addPlaceholder("%PLAYER%", p.getName())
                        .send(attacker);
            }
        }
    }

    public void teleportWithDelay(Player toTeleport, int delay,
                                  @Nullable Runnable runIfSuccess, @Nullable Runnable runIfCancelled) {
        HalfminerSystem.getInstance()
                .getTeleportHandler()
                .startTeleport(toTeleport, toTeleport.getLocation(), delay, false, runIfSuccess, runIfCancelled);
    }
}
