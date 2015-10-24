package de.halfminer.hms.util;

import de.halfminer.hms.HalfminerSystem;
import org.bukkit.entity.Player;

public class TitleSender {

    private final static HalfminerSystem hms = HalfminerSystem.getInstance();

    /**
     * Sends a title to the given player, or broadcasts the title, if player is null.
     * This will show the title with defaults for fadeIn/Out of 10 ms and 100 ms stay time.
     *
     * @param player to send the title to, or null to broadcast
     * @param title  message containing the title, color codes do not need to be translated
     */
    public static void sendTitle(Player player, String title) {
        sendTitle(player, title, 10, 100, 10);
    }

    /**
     * Sends a title to the given player, or broadcasts the title, if player is null.
     *
     * @param player  to send the title to, or null to broadcast
     * @param title   message containing the title, color codes do not need to be translated
     * @param fadeIn  time in milliseconds until message has fade in
     * @param stay    time in milliseconds message stays
     * @param fadeOut time in milliseconds until message faded out after it stay
     */
    public static void sendTitle(Player player, String title, int fadeIn, int stay, int fadeOut) {

        String command;
        if (player == null) {
            command = hms.getConfig().getString("general.titleCommandBroadcast");
            command = Language.placeholderReplace(command, "%FADEIN%", String.valueOf(fadeIn), "%STAY%", String.valueOf(stay),
                    "%FADEOUT%", String.valueOf(fadeOut), "%MESSAGE%", title);
        } else {
            if (!player.isOnline()) return;
            command = hms.getConfig().getString("general.titleCommandPlayer");
            command = Language.placeholderReplace(command, "%FADEIN%", String.valueOf(fadeIn), "%STAY%", String.valueOf(stay),
                    "%FADEOUT%", String.valueOf(fadeOut), "%PLAYER%", player.getName(), "%MESSAGE%", title);
        }
        command = command.replace("\n", "<nl>");
        if (command.endsWith("<nl>")) command = command.substring(0, command.length() - 4);
        dispatchCommand(command.replace("\n", "<nl>"));
    }

    private static void dispatchCommand(String command) {
        hms.getServer().dispatchCommand(hms.getServer().getConsoleSender(), command);
    }

}
