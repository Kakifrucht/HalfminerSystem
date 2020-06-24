package de.halfminer.hmc.module;

import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * - Broadcasts messages in a given interval
 * - Messages configurable with support for clickable commands
 * - Text separators and blank lines can be toggled for every message
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
        boolean addSeparators = hmc.getConfig().getBoolean("autoMessage.addSeparators", true);
        boolean addBlankLines = hmc.getConfig().getBoolean("autoMessage.addBlankLines", true);
        String separator = MessageBuilder.returnMessage("lineSeparator") + ChatColor.RESET;

        // If no messages are set disable
        if (messagesList.size() == 0) {
            if (running != null) running.cancel();
            return;
        }

        messages = new ArrayList<>();
        for (String message : messagesList) {

            boolean parseMessage = message.startsWith("~");

            String buildMessage = parseMessage ? message.substring(1) : message;
            buildMessage = ChatColor.translateAlternateColorCodes('&', buildMessage);
            if (addSeparators) {
                buildMessage = separator + "\n" + buildMessage + ChatColor.RESET + "\n" + separator;
            }

            if (addBlankLines) {
                buildMessage = ChatColor.RESET + " \n" + buildMessage + "\n ";
            }

            if (parseMessage) {
                buildMessage = "~" + buildMessage;
            }

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
