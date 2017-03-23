package de.halfminer.hmc.module;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.halfminer.hms.exceptions.FormattingException;
import de.halfminer.hms.exceptions.HookException;
import de.halfminer.hms.manageable.Sweepable;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Pair;
import de.halfminer.hms.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.*;
import java.util.concurrent.TimeUnit;

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

    private List<Pair<String, String>> chatFormats;
    private String topFormat;
    private String defaultFormat;

    private Cache<Player, Boolean> wasMentioned;

    private final Cache<Player, String> lastSentMessage = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .weakKeys()
            .build();

    private boolean isGlobalmuted = false;

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {

        Player p = e.getPlayer();
        if (isGlobalmuted && !p.hasPermission("hmc.chat.bypassglobalmute")) {
            MessageBuilder.create("modChatManGlobalmuteDenied", hmc, "Chat").sendMessage(p);
            e.setCancelled(true);
            return;
        }

        String message = filterMessage(p, e.getMessage());

        // spam filter
        if (!p.hasPermission("hmc.chat.spam")) {

            // check length of message without color codes
            int colorCount = 0;
            for (char c : message.toCharArray())
                if (c == ChatColor.COLOR_CHAR)
                    colorCount += 2;

            if ((message.length() - colorCount) < 2) {
                MessageBuilder.create("modChatManTooShort", hmc, "Chat").sendMessage(p);
                e.setCancelled(true);
                return;
            }

            // check if last message is the same
            String lastMessage = lastSentMessage.getIfPresent(p);
            if (lastMessage != null) {

                boolean cancel = false;

                /*
                  Message must start the same as last message, if message is shorter than 20 characters, check
                  if the length difference is smaller than 4. Example: Player sends "hello im a ver" and then finishes
                  his sentence by resending it's beginning "hello im a very nice person", the length check ensures
                  that the message won't be blocked.
                 */
                if (message.startsWith(lastMessage)) {

                    if (lastMessage.length() < 20) {
                        if (Math.abs(lastMessage.length() - message.length()) < 4)
                            cancel = true;
                    } else cancel = true;
                }

                if (cancel) {
                    MessageBuilder.create("modChatManRepeat", hmc, "Chat").sendMessage(p);
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
            prefix = hookHandler.getPrefix(p);
            suffix = hookHandler.getSuffix(p);
        } catch (HookException ex) {
            prefix = "";
            suffix = "";
        }

        format = MessageBuilder.create(format, hmc)
                .setDirectString()
                .addPlaceholderReplace("%PLAYER%", "%1$s")
                .addPlaceholderReplace("%CPREFIX%", prefix)
                .addPlaceholderReplace("%SUFFIX%", suffix)
                .addPlaceholderReplace("%MESSAGE%", "%2$s")
                .returnMessage();

        e.setFormat(format);

        // mention check
        Map<String, Player> players = new HashMap<>();
        e.getRecipients().stream()
                .filter(player -> !player.getName().equals(p.getName()))
                .forEach(player -> players.put(player.getName().toLowerCase(), player));

        Set<Player> mentioned = new HashSet<>();
        for (String str : Utils.filterNonUsernameChars(message).split(" ")) {

            if (players.containsKey(str)) {
                Player pMentioned = players.get(str);
                if (wasMentioned.getIfPresent(pMentioned) == null)
                    mentioned.add(pMentioned);
            }
        }

        for (Player wasMentioned : mentioned) {

            if (hookHandler.isAfk(wasMentioned)) {
                titleHandler.sendActionBar(p, MessageBuilder.create("modChatManIsAfk", hmc)
                                .addPlaceholderReplace("%PLAYER%", wasMentioned.getName())
                                .returnMessage());
            }

            titleHandler.sendActionBar(wasMentioned, MessageBuilder.create("modChatManMentioned", hmc)
                            .addPlaceholderReplace("%PLAYER%", p.getName())
                            .returnMessage());

            wasMentioned.playSound(wasMentioned.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.2f, 1.8f);
            this.wasMentioned.put(wasMentioned, true);
        }

        // play sound, set message and format, store for spam protection
        lastSentMessage.put(p, message.length() > 20 ? message.substring(0, 20) : message);
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_HAT, 1.0f, 2.0f);
        e.setMessage(message);
    }

    public void toggleGlobalmute() {

        isGlobalmuted = !isGlobalmuted;
        MessageBuilder.create(isGlobalmuted ? "modChatManGlobalmuteOn" : "modChatManGlobalmuteOff", hmc, "Chat")
                .broadcastMessage(true);
    }

    private String getFormat(Player sender) {

        if (sender.hasPermission("hmc.chat.topformat") && chatFormats.size() > 0) return topFormat;
        else {
            String format;
            format = defaultFormat;
            for (Pair<String, String> pair : chatFormats) {
                if (sender.hasPermission("hmc.chat.format." + pair.getLeft())) {
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

                if ((charAt > 21 && sender.hasPermission("hmc.chat.allowformatcode"))
                        || (charAt < 22 && charAt > -1 && sender.hasPermission("hmc.chat.allowcolor"))) {
                    sb.setCharAt(i, ChatColor.COLOR_CHAR);
                    i++;
                }
            } else if (current == '.'
                    && notSpaceCount > linkThreshold
                    && !sender.hasPermission("hmc.chat.allowlinks")) {

                sb.setCharAt(i, ' ');
            } else if (Character.isUpperCase(current)) amountUppercase++;
        }

        msg = sb.toString();

        if (!sender.hasPermission("hmc.chat.allowcaps")
                && msg.length() > 3
                && amountUppercase > (message.length() / 2)) msg = msg.toLowerCase();

        return msg;
    }

    @Override
    public void loadConfig() {

        int mentionDelay = hmc.getConfig().getInt("chat.mentionDelay", 10);

        wasMentioned = Utils.copyValues(wasMentioned,
                CacheBuilder.newBuilder()
                .expireAfterWrite(mentionDelay, TimeUnit.SECONDS)
                .build());

        chatFormats = new ArrayList<>();
        for (String formatUnparsed : hmc.getConfig().getStringList("chat.formats")) {
            try {
                Pair<String, String> keyValue = Utils.getKeyValuePair(formatUnparsed);
                keyValue.setLeft(keyValue.getLeft().toLowerCase());
                chatFormats.add(keyValue);
            } catch (FormattingException ignored) {}
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
        wasMentioned.cleanUp();
        lastSentMessage.cleanUp();
    }
}
