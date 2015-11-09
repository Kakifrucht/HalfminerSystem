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
    private int playerCountBuffer;
    private int playerCountLimit;

    public ModMotd() {
        reloadConfig();
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void serverPingMotd(ServerListPingEvent e) {

        int fakeLimit = hms.getServer().getOnlinePlayers().size();

        if (fakeLimit > playerCountThreshold - playerCountBuffer) fakeLimit += playerCountBuffer;
        else fakeLimit = playerCountThreshold;
        if (fakeLimit >= playerCountLimit) fakeLimit = playerCountLimit;

        e.setMaxPlayers(fakeLimit);
        e.setMotd(motd[rnd.nextInt(motd.length)]);
    }

    @Override
    public void reloadConfig() {

        playerCountThreshold = hms.getConfig().getInt("motd.playerCountThreshold", 50);
        playerCountBuffer = hms.getConfig().getInt("motd.playerCountBuffer", 1);
        playerCountLimit = hms.getServer().getMaxPlayers();
        String setMotd = Language.getMessagePlaceholders("modMotdLine", false, "%REPLACE%", storage.getString("sys.news"));

        List<String> strList = hms.getConfig().getStringList("motd.randomColors");
        motd = new String[strList.size()];
        for (int i = 0; i < strList.size(); i++)
            motd[i] = Language.placeholderReplaceColor(setMotd, "%COLOR%", '&' + strList.get(i));
    }
}
