package de.halfminer.hms.util;

import de.halfminer.hms.HalfminerSystem;
import org.bukkit.entity.Player;

public class TitleSender {

    private final static HalfminerSystem hms = HalfminerSystem.getInstance();

    /**
     * Sends a title to the given player, or to every player, if player is null.
     * This will show the title with a fadeIn/Out of 10 ms and 100 ms stay time.
     * To change these default values, call sendTitlePrivate() or sendTitlePublic() directly.
     *
     * @param player to send the title to, or null to broadcast
     * @param title message containing the title, color codes do not need to be translated
     */
    public static void sendTitle(Player player, String title) {
        if (player != null) sendTitlePrivate(player, title, 10, 100, 10);
        else sendTitlePublic(title, 10, 100, 10);
    }

    /**
     * Sends a title to the given player
     *
     * @param player player to send the title to
     * @param title text containing the title
     * @param fadeIn time in milliseconds until message has fade in
     * @param stay time in milliseconds message stays
     * @param fadeOut time in milliseconds until message faded out after it stayed
     */
    public static void sendTitlePrivate(Player player, String title, int fadeIn, int stay, int fadeOut) {

        if(player == null) return;

        String command = hms.getConfig().getString("general.titleCommandPlayer");
        Language.placeholderReplace(command, "%FADEIN%", String.valueOf(fadeIn), "%STAY%", String.valueOf(stay),
                "%FADEOUT%", String.valueOf(fadeOut), "%PLAYER%", player.getName(), "%MESSAGE%", title);

        dispatchCommand(command);
    }

    /**
     * Sends a title to every player on the server.
     *
     * @param title text containing the title
     * @param fadeIn time in milliseconds until message has fade in
     * @param stay time in milliseconds message stays
     * @param fadeOut time in milliseconds until message faded out after it stayed
     */
    public static void sendTitlePublic(String title, int fadeIn, int stay, int fadeOut) {

        String command = hms.getConfig().getString("general.titleCommandBroadcast");
        Language.placeholderReplace(command, "%FADEIN%", String.valueOf(fadeIn), "%STAY%", String.valueOf(stay),
                "%FADEOUT%", String.valueOf(fadeOut), "%MESSAGE%", title);

        dispatchCommand(command);

    }

    private static void dispatchCommand(String command) {
        hms.getServer().dispatchCommand(hms.getServer().getConsoleSender(), command);
    }

}
