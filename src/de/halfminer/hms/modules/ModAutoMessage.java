package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ModAutoMessage extends HalfminerModule {

    private final Random rnd = new Random();
    private BukkitRunnable running;
    private List<String> messages;
    private String separator;

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
        separator = Language.getMessage("lineSeparator");

        //Set task
        if (running != null) running.cancel();
        int interval = hms.getConfig().getInt("autoMessage.intervalSeconds", 240) * 20; //20 ticks per second
        running = new BukkitRunnable() {
            @Override
            public void run() {
                String message = messages.get(rnd.nextInt(messages.size()));
                for (Player player : hms.getServer().getOnlinePlayers()) {

                    player.sendMessage(" \n"
                            + separator + ChatColor.RESET
                            + message + ChatColor.RESET
                            + "\n" + separator + ChatColor.RESET
                            + " ");
                }
            }
        };
        running.runTaskTimerAsynchronously(hms, interval, interval);
    }
}
