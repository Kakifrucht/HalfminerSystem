package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

import java.util.List;
import java.util.Random;

public class ModMotd extends HalfminerModule implements Listener {

    private final Random rnd = new Random();
    private String[] motd;
    private int playerCountThreshold;

    public ModMotd() {
        reloadConfig();
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void serverPingMotd(ServerListPingEvent e) {

        int playerCount = hms.getServer().getOnlinePlayers().size();
        if (playerCount >= playerCountThreshold) e.setMaxPlayers(playerCount + 1);
        else e.setMaxPlayers(playerCountThreshold);

        e.setMotd(motd[rnd.nextInt(motd.length)]);
    }

    @Override
    public void reloadConfig() {

        playerCountThreshold = hms.getConfig().getInt("motd.playerCountThreshold", 50);
        String setMotd = Language.getMessagePlaceholders("modMotdLine", false, "%REPLACE%", storage.getString("sys.news"));

        List<String> strList = hms.getConfig().getStringList("motd.randomColors");
        motd = new String[strList.size()];
        for (int i = 0; i < strList.size(); i++)
            motd[i] = Language.placeholderReplaceColor(setMotd, "%COLOR%", '&' + strList.get(i));
    }
}
