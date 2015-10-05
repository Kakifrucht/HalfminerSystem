package de.halfminer.hms.modules;

import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.util.Language;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ModAutoMessage implements HalfminerModule {

    private final static HalfminerSystem hms = HalfminerSystem.getInstance();
    private final Random rnd = new Random();
    private BukkitRunnable running;
    private List<String> messages;
    private String placeholder;

    public ModAutoMessage() {
        reloadConfig();
    }

    @Override
    public void reloadConfig() {
        //Load messages
        List<String> messagesList = hms.getConfig().getStringList("autoMessage.messages");
        if (messagesList.size() == 0) { //if no messages are set disable
            if (running != null) running.cancel();
            return;
        }
        messages = new ArrayList<>(messagesList.size());
        for (String str : messagesList) messages.add(ChatColor.translateAlternateColorCodes('&', str));
        placeholder = Language.getMessage("autoMessagePlaceholder", false);

        //Set task
        if (running != null) running.cancel();
        Integer interval = hms.getConfig().getInt("autoMessage.intervalSeconds", 240) * 20; //20 ticks per second
        running = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : hms.getServer().getOnlinePlayers()) {
                    player.sendMessage(" \n" + placeholder + "\n" + ChatColor.RESET + messages.get(rnd.nextInt(messages.size())) + ChatColor.RESET + "\n" + placeholder + ChatColor.RESET + "\n ");
                }
            }
        };
        running.runTaskTimerAsynchronously(hms, interval, interval);
    }
}
