package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

import java.util.List;
import java.util.Random;

/**
 * - Configurable Serverlist Motd
 *   - Can be set via command
 * - Dynamic playerlimit indicator, configurable with buffers and limits
 */
@SuppressWarnings("unused")
public class ModMotd extends HalfminerModule implements Listener {

    private final Random rnd = new Random();
    private String[] motd;
    private int playerCountThreshold;
    private int playerCountBuffer;
    private int playerCountLimit;

    @EventHandler
    public void serverPingMotd(ServerListPingEvent e) {

        int fakeLimit = server.getOnlinePlayers().size();

        if (fakeLimit > playerCountThreshold - playerCountBuffer) fakeLimit += playerCountBuffer;
        else fakeLimit = playerCountThreshold;
        if (fakeLimit >= playerCountLimit) fakeLimit = playerCountLimit;

        e.setMaxPlayers(fakeLimit);
        e.setMotd(motd[rnd.nextInt(motd.length)]);
    }

    @Override
    public void loadConfig() {

        playerCountThreshold = config.getInt("motd.playerCountThreshold", 50);
        playerCountBuffer = config.getInt("motd.playerCountBuffer", 1);
        playerCountLimit = server.getMaxPlayers();
        String setMotd = Language.getMessagePlaceholders("modMotdLine", false, "%REPLACE%", storage.getString("news"));

        List<String> strList = config.getStringList("motd.randomColors");
        motd = new String[strList.size()];
        for (int i = 0; i < strList.size(); i++)
            motd[i] = Language.placeholderReplaceColor(setMotd, "%COLOR%", '&' + strList.get(i));
    }
}
