package de.halfminer.hmc.module;

import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * - Sends messages in a given interval
 * - Messages configurable
 *   - Commands can be made clickable (start with '~' and encapsulate command with trailing '/')
 */
@SuppressWarnings("unused")
public class ModAutoMessage extends HalfminerModule {

    private final Random rnd = new Random();
    private BukkitTask running;
    private List<String> messages;

    private int lastRandom = Integer.MAX_VALUE;

    @Override
    public void loadConfig() {

        List<String> messagesList = hmc.getConfig().getStringList("autoMessage.messages");
        String separator = MessageBuilder.returnMessage("lineSeparator");

        // If no messages are set disable
        if (messagesList.size() == 0) {
            if (running != null) running.cancel();
            return;
        }

        messages = new ArrayList<>();
        for (String message : messagesList) {

            boolean parseMessage = message.startsWith("~");

            String buildMessage = parseMessage ? message.substring(1) : message;
            buildMessage = " \n" + separator + ChatColor.RESET
                    + ChatColor.translateAlternateColorCodes('&', buildMessage) + ChatColor.RESET
                    + "\n" + separator + ChatColor.RESET
                    + " ";

            if (parseMessage) buildMessage = "~" + buildMessage;
            messages.add(buildMessage);
        }

        // Set task
        if (running != null) running.cancel();
        int interval = hmc.getConfig().getInt("autoMessage.intervalSeconds", 240) * 20;

        running = scheduler.runTaskTimer(hmc, () -> {

            // ensure that same message is not sent twice
            int messageIndex = rnd.nextInt(this.messages.size());
            if (messages.size() > 1 && messageIndex == lastRandom) {
                messageIndex = (messageIndex + 1) % messages.size();
            }
            lastRandom = messageIndex;

            String message = messages.get(messageIndex);
            MessageBuilder.create(message, hmc)
                    .setDirectString()
                    .broadcastMessage(false);
        }, interval, interval);
    }
}
