package de.halfminer.hms.modules;

import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.util.Language;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

import java.util.List;
import java.util.Random;

public class ModMOTD implements HalfminerModule, Listener {

    private final static HalfminerSystem hms = HalfminerSystem.getInstance();
    private final Random rnd = new Random();
    private String[] motd;

    public ModMOTD() {
        reloadConfig();
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void serverPing(ServerListPingEvent e) {
        e.setMotd(motd[rnd.nextInt(motd.length)]);
    }

    public void updateMotd(String newMotd) {
        hms.getConfig().set("motd.placeholder", newMotd.replace('§', '&'));
        hms.saveConfig();
        reloadConfig();
    }

    @Override
    public void reloadConfig() {

        String setMotd = hms.getConfig().getString("motd.topLine")
                + "\n" + Language.placeholderReplace(hms.getConfig().getString("motd.bottomLine"), "%REPLACE%", hms.getConfig().getString("motd.placeholder"));
        List<String> strList = hms.getConfig().getStringList("motd.randomColors");
        motd = new String[strList.size()];
        for (int i = 0; i < strList.size(); i++) {
            motd[i] = Language.placeholderReplaceColor(setMotd, "%COLOR%", '&' + strList.get(i));
        }
    }
}
