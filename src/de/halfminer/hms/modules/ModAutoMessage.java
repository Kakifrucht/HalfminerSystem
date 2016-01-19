package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ModAutoMessage extends HalfminerModule {

    private final Random rnd = new Random();
    private BukkitTask running;
    private List<String> messages;

    public ModAutoMessage() {
        reloadConfig();
    }

    @Override
    public void reloadConfig() {

        // Load messages
        List<String> messagesList = hms.getConfig().getStringList("autoMessage.messages");
        if (messagesList.size() == 0) {
            // If no messages are set disable
            if (running != null) running.cancel();
            return;
        }
        // Build messages
        String separator = Language.getMessage("lineSeparator");
        messages = new ArrayList<>(messagesList.size());
        for (String str : messagesList) {
            String toAdd = ChatColor.translateAlternateColorCodes('&', str.replace("\\n", "\n"));
            toAdd = " \n" + separator + ChatColor.RESET
                    + toAdd + ChatColor.RESET
                    + "\n" + separator + ChatColor.RESET
                    + " ";
            messages.add(toAdd);
        }

        // Set task
        if (running != null) running.cancel();

        int interval = hms.getConfig().getInt("autoMessage.intervalSeconds", 240) * 20;
        running = hms.getServer().getScheduler().runTaskTimerAsynchronously(hms, new Runnable() {
            @Override
            public void run() {
                String message = messages.get(rnd.nextInt(messages.size()));
                for (Player player : hms.getServer().getOnlinePlayers()) player.sendMessage(message);
            }
        }, interval, interval);
    }
}
