package de.halfminer.hms.modules;

import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.handlers.HanTitles;
import de.halfminer.hms.interfaces.Sweepable;
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
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * - Hooks into Vault to get prefix and suffix
 * - Custom chatformats
 *   - Default chatformat is bottom one
 *   - Permissions can be assigned via custom permission node
 *   - Permission to always get permission with highest priority
 * - Denies chatting if globalmute active
 * - Allows easy toggling of globalmute
 * - Plays sound on chat
 * - Notifies mentioned players via actionbar
 *   - Rate limit (no mention spam)
 * - Disallow
 *   - Using color codes
 *   - Using formatting codes
 *   - Posting links/IPs
 *   - Writing capitalized
 */
@SuppressWarnings("unused")
public class ModChatManager extends HalfminerModule implements Listener, Sweepable {

    private Chat vaultChat;

    private final List<Pair<String, String>> chatFormats = new ArrayList<>();
    private String topFormat;
    private String defaultFormat;

    private Map<Player, Long> lastMentioned = new ConcurrentHashMap<>();
    private boolean isGlobalmuted = false;

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {

        Player p = e.getPlayer();
        if (isGlobalmuted && !p.hasPermission("hms.chat.bypassglobalmute")) {
            p.sendMessage(Language.getMessagePlaceholders("modChatManGlobalmuteDenied", true, "%PREFIX%", "Chat"));
            e.setCancelled(true);
            return;
        }

        String format = getFormat(p);

        String prefix = "";
        String suffix = "";
        if (vaultChat != null) {
            prefix = vaultChat.getPlayerPrefix(p);
            suffix = vaultChat.getPlayerSuffix(p);
        }

        format = Language.placeholderReplace(format, "%PLAYER%", "%1$s",
                "%PREFIX%", prefix, "%SUFFIX%", suffix, "%MESSAGE%", "%2$s");
        format = ChatColor.translateAlternateColorCodes('&', format);

        String message = filterMessage(p, e.getMessage());

        Map<String, Player> players = new HashMap<>();
        for (Player player : e.getRecipients())
            if (!player.getName().equals(p.getName())) players.put(player.getName().toLowerCase(), player);

        Set<Player> mentioned = new HashSet<>();
        long currentTime = System.currentTimeMillis() / 1000;
        for (String str : message.split(" ")) {
            String filteredName = charFilter(str);
            if (players.containsKey(filteredName)) {
                Player pMentioned = players.get(filteredName);
                if (!lastMentioned.containsKey(pMentioned) || !(lastMentioned.get(pMentioned) > currentTime))
                    mentioned.add(pMentioned);
            }
        }

        for (Player wasMentioned : mentioned) {
            ((HanTitles) hms.getHandler(HandlerType.TITLES)).sendActionBar(wasMentioned,
                    Language.getMessagePlaceholders("modChatManMentioned", false, "%PLAYER%", p.getName()));
            wasMentioned.playSound(wasMentioned.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_TOUCH, 0.2f, 1.8f);
            lastMentioned.put(wasMentioned, currentTime + 10);
        }

        e.setMessage(message);
        e.setFormat(format);
        e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.BLOCK_NOTE_HAT, 1.0f, 2.0f);
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

    private String getFormat(Player sender) {

        if (sender.hasPermission("hms.chat.topformat") && chatFormats.size() > 0) return topFormat;
        else {
            String format;
            format = defaultFormat;
            for (Pair<String, String> pair : chatFormats) {
                if (sender.hasPermission("hms.chat.format." + pair.getLeft())) {
                    format = pair.getRight();
                    break;
                }
            }
            return format;
        }
    }

    private String filterMessage(Player sender, String message) {

        String msg = message;
        StringBuilder sb = new StringBuilder(msg);
        for (int i = 0; i < message.length() - 1; i++) {

            char at = sb.charAt(i);
            char at2 = sb.charAt(i + 1);
            if (at == '&') {

                int charAt = "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(at2);

                if ((charAt > 22 && sender.hasPermission("hms.chat.allowformatcode"))
                        || (charAt < 23 && charAt > -1 && sender.hasPermission("hms.chat.allowcolor")))
                    sb.setCharAt(i, ChatColor.COLOR_CHAR);
            } else if (at == '.' && i > 0 && message.length() > 6 && !sender.hasPermission("hms.chat.allowlinks")) {

                if (at2 != ' ' && sb.charAt(i - 1) != ' ') sb.setCharAt(i, ' ');
            }
        }

        msg = sb.toString();

        if (!sender.hasPermission("hms.chat.allowcaps") && msg.length() > 3) {
            int amountUppercase = 0;
            for (Character check : message.toCharArray()) if (Character.isUpperCase(check)) amountUppercase++;
            if (amountUppercase > (message.length() / 2)) msg = message.toLowerCase();
        }

        return msg;
    }

    private String charFilter(String toFilter) {

        StringBuilder sb = new StringBuilder(toFilter.toLowerCase());

        for (int i = 0; i < sb.length(); i++) {

            char toCheck = sb.charAt(i);
            if (!Character.isLetter(toCheck)
                    && !Character.isDigit(toCheck)
                    && toCheck != '_') {
                sb.deleteCharAt(i);
                i--;
            }
        }

        return sb.toString();
    }

    @Override
    public void loadConfig() {

        if (vaultChat == null) {
            RegisteredServiceProvider<Chat> provider = server.getServicesManager()
                    .getRegistration(net.milkbowl.vault.chat.Chat.class);
            if (provider != null) vaultChat = provider.getProvider();
        }

        chatFormats.clear();
        for (String formatUnparsed : hms.getConfig().getStringList("chat.formats")) {

            int indexSeparator = formatUnparsed.indexOf(":");
            if (indexSeparator < 0) continue;
            String key = formatUnparsed.substring(0, indexSeparator);
            String format = formatUnparsed.substring(indexSeparator + 1, formatUnparsed.length());
            chatFormats.add(new Pair<>(key, format));
        }

        if (chatFormats.size() == 0) {

            topFormat = "<%PLAYER%> %MESSAGE%";
            defaultFormat = "<%PLAYER%> %MESSAGE%";
        } else {

            topFormat = chatFormats.get(0).getRight();

            if (chatFormats.size() == 1) defaultFormat = chatFormats.get(0).getRight();
            else defaultFormat = chatFormats.get(chatFormats.size() - 1).getRight();

            chatFormats.remove(chatFormats.size() - 1);
        }
    }

    @Override
    public void sweep() {
        this.lastMentioned = new ConcurrentHashMap<>();
    }
}
