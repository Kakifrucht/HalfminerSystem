package de.halfminer.hms.modules;

import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.ChatColor;
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

        playerCountThreshold = hms.getConfig().getInt("motd.playerCountThreshold", 50);
        playerCountBuffer = hms.getConfig().getInt("motd.playerCountBuffer", 1);
        playerCountLimit = server.getMaxPlayers();

        // add reset code to start of news
        String newsString = storage.getString("news");
        newsString = ChatColor.RESET + newsString;

        String setMotd = MessageBuilder.create("modMotdLine", hms)
                .addPlaceholderReplace("%REPLACE%", newsString)
                .returnMessage();

        List<String> strList = hms.getConfig().getStringList("motd.randomColors");
        motd = new String[strList.size()];
        for (int i = 0; i < strList.size(); i++)
            motd[i] = MessageBuilder.create(setMotd, hms)
                    .setDirectString()
                    .addPlaceholderReplace("%COLOR%", '&' + strList.get(i))
                    .returnMessage();
    }
}
