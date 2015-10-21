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

    public ModMotd() {
        reloadConfig();
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void serverPing(ServerListPingEvent e) {
        e.setMotd(motd[rnd.nextInt(motd.length)]);
    }

    @Override
    public void reloadConfig() {

        String setMotd = Language.getMessagePlaceholderReplace("modMotdLine", false, "%REPLACE%", storage.getString("sys.news"));

        List<String> strList = hms.getConfig().getStringList("motd.randomColors");
        motd = new String[strList.size()];
        for (int i = 0; i < strList.size(); i++)
            motd[i] = Language.placeholderReplaceColor(setMotd, "%COLOR%", '&' + strList.get(i));
    }
}
