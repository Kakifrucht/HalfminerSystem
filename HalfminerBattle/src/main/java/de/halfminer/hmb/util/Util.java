package de.halfminer.hmb.util;

import de.halfminer.hmb.HalfminerBattle;
import de.halfminer.hmb.arena.abs.Arena;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class Util {

    //TODO replace lang utils with MessageBuilder
    private static final HalfminerBattle hmb = HalfminerBattle.getInstance();

    /**
     * Send a message to a player with specified key and given replacements
     *
     * @param sendTo       player or recipient to send the message to
     * @param messageKey   key of the message in config.yml
     * @param replacements array, where first entry is string to replace, second one with what to replace, this can be repeated
     */
    public static void sendMessage(CommandSender sendTo, String messageKey, String... replacements) {
        String message = getMessage(messageKey, replacements);
        if (message.length() > 0) sendTo.sendMessage(message); //Do not send empty lines
    }

    /**
     * Broadcasts a message to all players
     *
     * @param messageKey    message to broadcast
     * @param exemptPlayers players the message should not be sent to
     * @param replacements  array, where first entry is string to replace, second one with what to replace, this can be repeated
     */
    public static void broadcastMessage(String messageKey, Player[] exemptPlayers, String... replacements) {

        String message = getMessage(messageKey, replacements);

        if (message.length() > 0) {
            Set<Player> exempt = new HashSet<>();
            Collections.addAll(exempt, exemptPlayers);
            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !exempt.contains(p))
                    .forEach(p -> p.sendMessage(message));
        }
    }

    /**
     * Get the message from config
     *
     * @param messageKey   key of the message
     * @param replacements array, where first entry is string to replace, second one with what to replace, this can be repeated
     * @return String containing the final and parsed message
     */
    private static String getMessage(String messageKey, String... replacements) {

        String toReturn = hmb.getConfig().getString("localization." + messageKey);
        if (toReturn == null || toReturn.length() == 0) return ""; // Allows messages to be removed

        toReturn = ChatColor.translateAlternateColorCodes('&',
                hmb.getConfig().getString("localization.prefix") + toReturn).replace("\\n", "\n");

        if (replacements != null) {
            for (int i = 0; i < replacements.length; i += 2) {
                toReturn = toReturn.replace(replacements[i], replacements[i + 1]);
            }
        }

        return toReturn;
    }

    public static String getStringFromArenaList(List<Arena> arenas, boolean addRandom) {

        StringBuilder sb = new StringBuilder();
        int number = 1;
        for (Arena arena : arenas) {
            sb.append(ChatColor.GREEN)
                    .append(number)
                    .append(": ")
                    .append(ChatColor.GRAY)
                    .append(arena.getName())
                    .append(' ');
            number++;
        }

        if (addRandom) {
            sb.append(ChatColor.YELLOW)
                    .append(number)
                    .append(": ")
                    .append(ChatColor.GRAY)
                    //TODO messagebuilder
                    .append(hmb.getConfig().getString("localization.randomArena"));
        } else sb.setLength(sb.length() - 1);
        return sb.toString();
    }
}
