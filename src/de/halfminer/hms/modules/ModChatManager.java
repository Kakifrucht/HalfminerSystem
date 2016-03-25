package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.Pair;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Fully fledged chatmanager
 * - Hooks into Vault to get prefix and suffix
 * - Custom chatformats
 *   - Default chatformat is bottom one
 *   - Permissions can be assigned via custom permission node
 *   - Permission to always get permission with highest priority
 * - Denies chatting if globalmute active
 * - Allows easy toggling of globalmute
 * - Plays sound on chat
 * - Disallow
 *   - Using color codes
 *   - Using formatting codes
 *   - Posting links/IPs
 *   - Writing capitalized
 */
@SuppressWarnings("unused")
public class ModChatManager extends HalfminerModule implements Listener {

    private Chat vaultChat;

    private final List<Pair<String, String>> chatFormats = new ArrayList<>();
    private String defaultFormat;
    private boolean isGlobalmuted = false;

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {

        Player p = e.getPlayer();
        if (isGlobalmuted && !p.hasPermission("hms.chat.bypassglobalmute")) {
            p.sendMessage(Language.getMessagePlaceholders("modChatManGlobalmuteDenied", true, "%PREFIX%", "Chat"));
            e.setCancelled(true);
            return;
        }

        e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.BLOCK_NOTE_HAT, 1.0f, 2.0f);

        String message = e.getMessage();

        String format;
        if (p.hasPermission("hms.chat.topformat") && chatFormats.size() > 0) format = chatFormats.get(0).getRight();
        else {
            format = defaultFormat;
            for (Pair<String, String> pair : chatFormats) {
                if (p.hasPermission("hms.chat.format." + pair.getLeft())) {
                    format = pair.getRight();
                    break;
                }
            }
        }

        format = Language.placeholderReplace(format, "%PLAYER%", "%1$s",
                "%PREFIX%", vaultChat.getPlayerPrefix(p), "%SUFFIX%", vaultChat.getPlayerSuffix(p), "%MESSAGE%", "%2$s");
        format = ChatColor.translateAlternateColorCodes('&', format);

        StringBuilder sb = new StringBuilder(message);
        for (int i = 0; i < message.length() - 1; i++) {

            char at = sb.charAt(i);
            char at2 = sb.charAt(i + 1);
            if (at == '&') {

                int charAt = "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(at2);

                if ((charAt > 22 && p.hasPermission("hms.chat.allowformatcode"))
                        || (charAt < 23 && charAt > -1 && p.hasPermission("hms.chat.allowcolor")))
                    sb.setCharAt(i, ChatColor.COLOR_CHAR);
            } else if (at == '.' && i > 0 && message.length() > 6 && !p.hasPermission("hms.chat.allowlinks")) {

                if (at2 != ' ' && sb.charAt(i - 1) != ' ') sb.setCharAt(i, ' ');
            }
        }

        message = sb.toString();

        if (!p.hasPermission("hms.chat.allowcaps") && message.length() > 3) {
            int amountUppercase = 0;
            for (Character check : message.toCharArray()) if (Character.isUpperCase(check)) amountUppercase++;
            if (amountUppercase > (message.length() / 2)) message = message.toLowerCase();
        }

        e.setMessage(message);
        e.setFormat(format);
    }

    public void toggleGlobalmute() {

        isGlobalmuted = !isGlobalmuted;
        if (isGlobalmuted) {

            server.broadcast(Language.getMessagePlaceholders("modChatManGlobalmuteOn",
                    true, "%PREFIX%", "Chat"), "hms.default");
        } else {

            server.broadcast(Language.getMessagePlaceholders("modChatManGlobalmuteOff",
                    true, "%PREFIX%", "Chat"), "hms.default");
        }
    }

    @Override
    public void reloadConfig() {

        if (vaultChat == null)
            vaultChat = server.getServicesManager().getRegistration(net.milkbowl.vault.chat.Chat.class).getProvider();

        chatFormats.clear();
        for (String formatUnparsed : hms.getConfig().getStringList("chat.formats")) {

            int indexSeparator = formatUnparsed.indexOf(":");
            if (indexSeparator < 0) continue;
            String key = formatUnparsed.substring(0, indexSeparator);
            String format = formatUnparsed.substring(indexSeparator + 1, formatUnparsed.length());
            chatFormats.add(new Pair<>(key, format));
        }

        defaultFormat = chatFormats.get(chatFormats.size() - 1).getRight();
        chatFormats.remove(chatFormats.size() - 1);
    }
}
