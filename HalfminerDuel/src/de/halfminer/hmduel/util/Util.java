package de.halfminer.hmduel.util;

import de.halfminer.hmduel.HalfminerDuel;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class Util {

    private static final HalfminerDuel hmd = HalfminerDuel.getInstance();

    /**
     * Send a message to a player with specified key.
     * Overloaded method that calls Util.sendMessage(sendTo, messageKey, null)
     * @param sendTo player or recipient to send the message to
     * @param messageKey key of the message in config.yml
     */
    public static void sendMessage(CommandSender sendTo, String messageKey) {
        sendMessage(sendTo,messageKey,null);
    }

    /**
     * Send a message to a player with specified key and given replacements
     * @param sendTo player or recipient to send the message to
     * @param messageKey key of the message in config.yml
     * @param replacements array, where first entry is string to replace, second one with what to replace, this can be repeated
     */
    public static void sendMessage(CommandSender sendTo, String messageKey, String[] replacements) {
        String message = getMessage(messageKey, replacements);
        if(message.length() > 0) sendTo.sendMessage(message); //Do not send empty lines
    }

    /**
     * Broadcasts a message to all players
     * @param messageKey message to broadcast
     * @param replacements array, where first entry is string to replace, second one with what to replace, this can be repeated
     * @param exemptPlayers players the message should not be sent to
     */
    public static void broadcastMessage(String messageKey, String[] replacements, List<Player> exemptPlayers) {

        String message = getMessage(messageKey, replacements);

        if(message.length() > 0) {
            for(Player player: Bukkit.getOnlinePlayers()) {
                if(!exemptPlayers.contains(player)) {
                    player.sendMessage(message);
                }
            }
        }
    }

    /**
     * Get the message from config
     * @param messageKey key of the message
     * @param replacements array, where first entry is string to replace, second one with what to replace, this can be repeated
     * @return String containing the final and parsed message
     */
    private static String getMessage(String messageKey, String[] replacements) {

        String toReturn = hmd.getConfig().getString("localization." + messageKey);
        if(toReturn == null || toReturn.length() == 0) return ""; //Allow messages to be removed
        //Get proper color codes and newlines, add prefix
        toReturn = ChatColor.translateAlternateColorCodes('&', hmd.getConfig().getString("localization.prefix") + toReturn).replace("\\n", "\n");

        if(replacements != null) {
            for(int i = 0;i < replacements.length;i += 2) {
                toReturn = toReturn.replace(replacements[i], replacements[i + 1]);
            }
        }

        return toReturn;
    }

}
