package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;

public class ModStats extends HalfminerModule implements Listener {

    private final ModStorage storage = hms.getModStorage();
    private final HashMap<Player, Long> timeOnline = new HashMap<>();

    public ModStats() {
        reloadConfig();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @SuppressWarnings("unused")
    public void playerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        storage.incrementPlayerInt(player, "joins", 1);
        timeOnline.put(player, System.currentTimeMillis() / 1000);

        //Name checking
        String lastName = storage.getPlayerString(player, "lastname");
        //Called on first join
        if (lastName.length() == 0) storage.setPlayer(player, "lastname", player.getName());
        else if (!lastName.equals(player.getName())) {
            String lastNames = storage.getPlayerString(player, "lastnames");
            hms.getServer().broadcast(Language.getMessagePlaceholderReplace("modStatsNameChange", true,
                    "%PREFIX%", "Name", "%OLDNAME%", lastName, "%NEWNAME%", player.getName()), "hms.default");
            storage.setPlayer(player, "lastnames", lastNames + ' ' + lastName);
            storage.setPlayer(player, "lastname", player.getName());
        }
        storage.set("uid." + player.getName().toLowerCase(), player.getUniqueId().toString());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @SuppressWarnings("unused")
    public void playerLeave(PlayerQuitEvent e) {
        setOnlineTime(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @SuppressWarnings("unused")
    public void playerDeath(PlayerDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        Player victim = e.getEntity().getPlayer();
        if (killer != null) {
            storage.incrementPlayerInt(killer, "kills", 1);
            storage.setPlayer(killer, "kdratio", calculateKDRatio(killer));
            storage.setPlayer(killer, "lastkill", System.currentTimeMillis() / 1000);
        }
        storage.incrementPlayerInt(victim, "deaths", 1);
        storage.setPlayer(victim, "kdratio", calculateKDRatio(victim));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @SuppressWarnings("unused")
    public void playerMobkill(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof Player) && e.getEntity().getKiller() != null)
            storage.incrementPlayerInt(e.getEntity().getKiller(), "mobkills", 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void blockPlace(BlockPlaceEvent e) {
        storage.incrementPlayerInt(e.getPlayer(), "blocksplaced", 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void blockBreak(BlockBreakEvent e) {
        storage.incrementPlayerInt(e.getPlayer(), "blocksbroken", 1);
    }

    private double calculateKDRatio(Player player) {
        double calc = storage.getPlayerInt(player, "kills") / storage.getPlayerInt(player, "deaths");
        return Math.round(calc * 100) / 100;
    }

    private void setOnlineTime(Player player) {
        int time;

        time = (int) ((System.currentTimeMillis() / 1000) - timeOnline.get(player));
        storage.incrementPlayerInt(player, "timeonline", time);
        timeOnline.remove(player);
    }

    @Override
    public void onDisable() {
        for (Player player : timeOnline.keySet()) setOnlineTime(player);
        timeOnline.clear(); //make sure that calculations are not done twice
    }

}
