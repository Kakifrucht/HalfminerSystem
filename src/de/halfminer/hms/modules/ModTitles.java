package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.StatsType;
import de.halfminer.hms.util.TitleSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ModTitles extends HalfminerModule {

    private final Map<UUID, Integer> killStreaks = new HashMap<>();
    private final Map<UUID, Integer> deathStreaks = new HashMap<>();

    public ModTitles() {
        reloadConfig();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @SuppressWarnings("unused")
    public void joinTitles(PlayerJoinEvent e) {

        final Player joined = e.getPlayer();
        int timeOnline = storage.getStatsInt(joined, StatsType.TIME_ONLINE);

        if (timeOnline == 0) TitleSender.sendTitle(joined, Language.getMessage("modStaticListenersNewPlayerFormat"));
        else {

            hms.getServer().getScheduler().runTaskAsynchronously(hms, new Runnable() {
                @Override
                public void run() {
                    TitleSender.sendTitle(joined, Language.getMessagePlaceholderReplace("modStaticListenersJoinFormat",
                            false, "%NEWS%", storage.getString("sys.news")));

                    try {
                        Thread.sleep(6000l);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }

                    TitleSender.sendTitle(joined, Language.getMessagePlaceholderReplace("modStaticListenersNewsFormat",
                            false, "%NEWS%", storage.getString("sys.news")), 40, 180, 40);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @SuppressWarnings("unused")
    public void deathActionbarSender(PlayerDeathEvent e) {

        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        if (killer != null) {

            UUID killerUid = killer.getUniqueId();
            UUID victimUid = victim.getUniqueId();

            deathStreaks.remove(killerUid);
            killStreaks.remove(victimUid);

            int killerStreak = killStreaks.get(killerUid);
            int victimStreak = deathStreaks.get(victimUid);

            killStreaks.put(killerUid, ++killerStreak);
            deathStreaks.put(victimUid, ++victimStreak);

            if (killerStreak > 4) {
                TitleSender.sendActionBar(null, Language.getMessagePlaceholderReplace("modTitlesKillStreak", false,
                        "%PLAYER%", killer.getName(), "%STREAK%", String.valueOf(killerStreak)));
            }
            if (victimStreak > 4) {
                TitleSender.sendActionBar(null, Language.getMessagePlaceholderReplace("modTitlesDeathStreak", false,
                        "%PLAYER%", victim.getName(), "%STREAK%", String.valueOf(victimStreak)));
            }
        }
    }
}
