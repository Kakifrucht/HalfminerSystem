package de.halfminer.hms.modules;

import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.exception.HookException;
import de.halfminer.hms.handlers.HanHooks;
import de.halfminer.hms.handlers.HanTitles;
import de.halfminer.hms.interfaces.Sweepable;
import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.Pair;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * - Hooks into Vault to get prefix and suffix
 * - Custom chatformats
 *   - Default chatformat is bottom one
 *   - Format can be assigned via custom permission node
 *   - Permission to always get format with highest priority
 *   - No format limit
 * - Denies chatting if globalmute active
 *   - Allows easy toggling of globalmute
 * - Plays sound on chat
 * - Notifies mentioned players via actionbar
 *   - Shows if mentioned player is afk
 *   - Rate limit (no mention spam)
 * - Disallow (or allow via permission)
 *   - Sending empty/one character messages
 *   - Repeating the same (or similar) message
 *   - Using color codes
 *   - Using formatting codes
 *   - Posting links/IPs
 *   - Writing capitalized
 */
public class ModChatManager extends HalfminerModule implements Listener, Sweepable {

    private final HanHooks hooks = (HanHooks) hms.getHandler(HandlerType.HOOKS);
    private final HanTitles title = (HanTitles) hms.getHandler(HandlerType.TITLES);

    private final List<Pair<String, String>> chatFormats = new ArrayList<>();
    private String topFormat;
    private String defaultFormat;
    private int mentionDelay;

    private Map<Player, Long> lastMentioned = new ConcurrentHashMap<>();
    private Map<Player, Pair<String, Long>> lastMessage = new ConcurrentHashMap<>();
    private boolean isGlobalmuted = false;

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {

        Player p = e.getPlayer();
        if (isGlobalmuted && !p.hasPermission("hms.chat.bypassglobalmute")) {
            p.sendMessage(Language.getMessagePlaceholders("modChatManGlobalmuteDenied", true, "%PREFIX%", "Chat"));
            e.setCancelled(true);
            return;
        }

        String message = filterMessage(p, e.getMessage());

        // spam filter
        if (!p.hasPermission("hms.chat.spam")) {

            // check length of message without color codes
            int colorCount = 0;
            for (char c : message.toCharArray())
                if (c == ChatColor.COLOR_CHAR)
                    colorCount += 2;

            if ((message.length() - colorCount) < 2) {

                p.sendMessage(Language.getMessagePlaceholders("modChatManTooShort", true, "%PREFIX%", "Chat"));
                e.setCancelled(true);
                return;
            }

            // check if last message is the same
            if (hasLastMessage(p)) {

                boolean cancel = false;
                String last = lastMessage.get(p).getLeft();

                /*
                  Message must start the same as last message, if message is shorter than 20 characters, check
                  if the length difference is smaller than 4. Example: Player sends "hello im a ver" and then finishes
                  his sentence by resending it's beginning "hello im a very nice person", the length check ensures
                  that the message won't be blocked.
                 */
                if (message.startsWith(last)) {

                    if (last.length() < 20) {
                        if (Math.abs(last.length() - message.length()) < 4)
                            cancel = true;
                    } else cancel = true;
                }

                if (cancel) {
                    p.sendMessage(Language.getMessagePlaceholders("modChatManRepeat", true, "%PREFIX%", "Chat"));
                    e.setCancelled(true);
                    return;
                }
            }
        }

        // determine format and fill prefix and suffix
        String format = getFormat(p);

        String prefix;
        String suffix;

        try {
            prefix = hooks.getPrefix(p);
            suffix = hooks.getSuffix(p);
        } catch (HookException ex) {
            prefix = "";
            suffix = "";
        }

        format = Language.placeholderReplace(format, "%PLAYER%", "%1$s",
                "%PREFIX%", prefix, "%SUFFIX%", suffix, "%MESSAGE%", "%2$s");
        format = ChatColor.translateAlternateColorCodes('&', format);

        // mention check
        Map<String, Player> players = new HashMap<>();
        e.getRecipients().stream()
                .filter(player -> !player.getName().equals(p.getName()))
                .forEach(player -> players.put(player.getName().toLowerCase(), player));

        Set<Player> mentioned = new HashSet<>();
        long currentTime = System.currentTimeMillis() / 1000;
        for (String str : filterNonUsernameChars(message).split(" ")) {

            if (players.containsKey(str)) {
                Player pMentioned = players.get(str);
                if (!lastMentioned.containsKey(pMentioned) || !(lastMentioned.get(pMentioned) > currentTime))
                    mentioned.add(pMentioned);
            }
        }

        for (Player wasMentioned : mentioned) {
            if (hooks.isAfk(wasMentioned))
                title.sendActionBar(p, Language.getMessagePlaceholders("modChatManIsAfk", false,
                        "%PLAYER%", wasMentioned.getName()));
            title.sendActionBar(wasMentioned, Language.getMessagePlaceholders("modChatManMentioned", false,
                    "%PLAYER%", p.getName()));
            wasMentioned.playSound(wasMentioned.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_TOUCH, 0.2f, 1.8f);
            lastMentioned.put(wasMentioned, currentTime + mentionDelay);
        }

        // play sound, set message and format, store for spam protection
        lastMessage.put(p,
                new Pair<>(message.length() > 20 ? message.substring(0, 20) : message, System.currentTimeMillis()));
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_HAT, 1.0f, 2.0f);
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

        int amountUppercase = 0;
        int notSpaceCount = 0;
        int linkThreshold = sb.length() > 10 ? 5 : 2;

        for (int i = 0; i < sb.length() - 1; i++) {

            char current = sb.charAt(i);
            char next = sb.charAt(i + 1);

            if (current == ' ') notSpaceCount = 0;
            else notSpaceCount++;

            if (current == '&') {

                int charAt = "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(next);

                if ((charAt > 22 && sender.hasPermission("hms.chat.allowformatcode"))
                        || (charAt < 23 && charAt > -1 && sender.hasPermission("hms.chat.allowcolor"))) {
                    sb.setCharAt(i, ChatColor.COLOR_CHAR);
                    i++;
                }
            } else if (current == '.'
                    && notSpaceCount > linkThreshold
                    && !sender.hasPermission("hms.chat.allowlinks")) {

                sb.setCharAt(i, ' ');
            } else if (Character.isUpperCase(current)) amountUppercase++;
        }

        msg = sb.toString();

        if (!sender.hasPermission("hms.chat.allowcaps")
                && msg.length() > 3
                && amountUppercase > (message.length() / 2)) msg = msg.toLowerCase();

        return msg;
    }

    private String filterNonUsernameChars(String toFilter) {

        StringBuilder sb = new StringBuilder(toFilter.toLowerCase());

        for (int i = 0; i < sb.length(); i++) {

            char toCheck = sb.charAt(i);
            if (Character.isLetter(toCheck) || Character.isDigit(toCheck) || toCheck == '_' || toCheck == ' ') continue;

            sb.deleteCharAt(i);
            i--;
        }

        if (sb.length() > 16) sb.setLength(16);

        return sb.toString();
    }

    private boolean hasLastMessage(Player p) {

        if (lastMessage.containsKey(p)) {

            Pair<String, Long> pair = lastMessage.get(p);
            if (pair.getRight() + 60000 < System.currentTimeMillis()) {
                lastMessage.remove(p);
                return false;
            } else return true;
        }

        return false;
    }

    @Override
    public void loadConfig() {

        sweep();
        mentionDelay = hms.getConfig().getInt("chat.mentionDelay", 10);

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
        lastMessage.keySet().forEach(this::hasLastMessage);
    }
}
