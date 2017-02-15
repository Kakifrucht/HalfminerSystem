package de.halfminer.hms.cmd;

import de.halfminer.hms.cmd.abs.HalfminerCommand;
import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.enums.ModuleType;
import de.halfminer.hms.handlers.HanBossBar;
import de.halfminer.hms.handlers.HanTitles;
import de.halfminer.hms.modules.ModChatManager;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Level;

/**
 * - Chat manipulation tools
 * - Toggle globalmute
 * - Clear chat
 * - Send clickable command message to all players
 * - Title broadcast
 * - Bossbar broadcast
 * - Countdown via bossbar
 * - Send custom messages to player or broadcast
 * - Set news and motd message
 */
@SuppressWarnings({"unused", "BooleanMethodIsAlwaysInverted"})
public class Cmdchat extends HalfminerCommand {

    private final HanBossBar bossBar = (HanBossBar) hms.getHandler(HandlerType.BOSS_BAR);
    private final HanTitles titles = (HanTitles) hms.getHandler(HandlerType.TITLES);
    private String message;

    public Cmdchat() {
        this.permission = "hms.chat";
    }

    @Override
    public void execute() {

        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "bossbar":
                case "title":
                    if (!hasAdvancedPermission()) return;
                    sendTitleAndBossbar();
                    break;
                case "clear":
                case "cl":
                    clearChat();
                    break;
                case "countdown":
                    if (!hasAdvancedPermission()) return;
                    countdown();
                    break;
                case "globalmute":
                    if (!hasAdvancedPermission()) return;
                    ((ModChatManager) hms.getModule(ModuleType.CHAT_MANAGER)).toggleGlobalmute();
                    break;
                case "news":
                    if (!hasAdvancedPermission()) return;
                    setNews();
                    break;
                case "send":
                    if (!hasAdvancedPermission()) return;
                    sendMessage();
                    break;
                case "sendcommand":
                case "sc":
                    sendCommand();
                    break;
                case "set":
                    if (!hasAdvancedPermission()) return;
                    setMessage();
                    break;
                default:
                    showUsage();
            }
        } else showUsage();
    }

    private void sendTitleAndBossbar() {

        if (!verifyMessage()) return;

        boolean useBossbar = args[0].equalsIgnoreCase("bossbar");

        int time = 1;
        Player sendTo = null;

        if (args.length > 1) {

            try {
                time = Integer.decode(args[1]);
                if (time < 1) time = 1;
            } catch (NumberFormatException e) {
                time = 1;
            }

            if (args.length > 2) sendTo = server.getPlayer(args[2]);
        }

        String recipientName;
        if (sendTo == null) recipientName = MessageBuilder.returnMessage(hms, "cmdChatAll");
        else recipientName = sendTo.getName();

        String notifySender = useBossbar ? "cmdChatBossbar" : "cmdChatTitle";

        MessageBuilder.create(hms, notifySender, "Chat")
                .addPlaceholderReplace("%SENDTO%", recipientName)
                .addPlaceholderReplace("%TIME%", String.valueOf(time))
                .addPlaceholderReplace("%MESSAGE%", message)
                .sendMessage(sender);

        if (useBossbar) {

            BarColor color = BarColor.PURPLE;
            int potentialColor = 3;
            if (sendTo == null) potentialColor = 2;
            if (args.length > potentialColor) {
                for (BarColor colors : BarColor.values()) {
                    if (colors.toString().equalsIgnoreCase(args[potentialColor])) {
                        color = colors;
                        break;
                    }
                }
            }

            if (sendTo != null) bossBar.sendBar(sendTo, message, color, BarStyle.SOLID, time);
            else bossBar.broadcastBar(message, color, BarStyle.SOLID, time);

        } else {
            if (message.startsWith("\n")) message = " " + message;
            titles.sendTitle(sendTo, message, 10, time * 20 - 20, 10);
        }
    }

    private void clearChat() {

        String whoCleared = Utils.getPlayername(sender);

        final StringBuilder clearMessage = new StringBuilder();
        for (int i = 0; i < 100; i++) clearMessage.append(" \n");

        server.getOnlinePlayers().stream()
                .filter(p -> !p.hasPermission("hms.chat.advanced"))
                .forEach(p -> p.sendMessage(clearMessage.toString()));

        MessageBuilder.create(hms, "cmdChatCleared", "Chat")
                .addPlaceholderReplace("%PLAYER%", whoCleared)
                .broadcastMessage(false);

        MessageBuilder.create(hms, "cmdChatClearedLog")
                .addPlaceholderReplace("%PLAYER%", whoCleared)
                .logMessage(Level.INFO);
    }

    private void countdown() {

        if (args.length < 2) {
            showUsage();
            return;
        }

        final int countdown;
        try {
            countdown = Integer.decode(args[1]);
        } catch (NumberFormatException e) {
            showUsage();
            return;
        }

        if (countdown > 30) showUsage();
        else {

            MessageBuilder.create(hms, "cmdChatCountdownStarted", "Chat")
                    .addPlaceholderReplace("%COUNT%", String.valueOf(countdown))
                    .sendMessage(sender);

            final HanBossBar bar = (HanBossBar) hms.getHandler(HandlerType.BOSS_BAR);
            final BukkitTask task = scheduler.runTaskTimer(hms, new Runnable() {

                int count = countdown;
                @Override
                public void run() {

                    if (count < 0) return;
                    bar.broadcastBar(MessageBuilder.create(hms, "cmdChatCountdown")
                                    .addPlaceholderReplace("%COUNT%", String.valueOf(count))
                                    .returnMessage(),
                            BarColor.GREEN, BarStyle.SOLID, 35, (double) count / countdown);
                    if (count-- == 0) {
                        for (Player p : hms.getServer().getOnlinePlayers()) {
                            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
                        }
                    }
                }
            }, 0, 20L);

            scheduler.runTaskLater(hms, () -> {

                task.cancel();
                bar.removeBar();
            }, 20 * (countdown + 4));
        }
    }

    private void setNews() {

        if (!verifyMessage()) return;

        storage.set("news", message);
        hms.getModule(ModuleType.MOTD).loadConfig();
        if (sender instanceof Player) {
            bossBar.sendBar((Player) sender, MessageBuilder.create(hms, "modTitlesNewsFormat")
                    .addPlaceholderReplace("%NEWS%", message)
                    .returnMessage(), BarColor.YELLOW, BarStyle.SOLID, 5);
            titles.sendTitle((Player) sender, " \n" + message, 10, 100, 10);
        }

        MessageBuilder.create(hms, "cmdChatNewsSetTo", "Chat").sendMessage(sender);
    }

    private void sendMessage() {

        if (!verifyMessage()) return;

        String sendToString;

        if (args.length > 1) {

            Player player = server.getPlayer(args[1]);
            if (player != null) {

                player.sendMessage(message);
                sendToString = player.getName();
            } else {

                MessageBuilder.create(null, "playerNotOnline", "Chat").sendMessage(sender);
                return;
            }
        } else {

            server.broadcast(message, "hms.default");
            sendToString = MessageBuilder.returnMessage(hms, "cmdChatAll");
        }

        MessageBuilder.create(hms, "cmdChatSend", "Chat")
                .addPlaceholderReplace("%SENDTO%", sendToString)
                .sendMessage(sender);
    }

    private void sendCommand() {

        if (args.length < 3) {
            showUsage();
            return;
        }

        Player directRecipient = server.getPlayer(args[1]);
        if (directRecipient == null) {
            MessageBuilder.create(null, "playerNotOnline", "Chat").sendMessage(sender);
            return;
        }

        String command = Utils.arrayToString(args, 2, false);
        if (!command.startsWith("/")) command = "/" + command;

        MessageBuilder.create(hms, "cmdChatClickableCommand", Utils.getPlayername(sender))
                .addPlaceholderReplace("%PLAYER%", directRecipient.getName())
                .addPlaceholderReplace("%COMMAND%", command + "/") // append slash since it will be parsed
                .broadcastMessage(false);

        directRecipient.playSound(directRecipient.getLocation(), Sound.BLOCK_NOTE_HARP, 0.5f, 1.7f);
    }

    private void setMessage() {

        if (args.length < 2) {
            showUsage();
            return;
        }

        String message = Utils.arrayToString(args, 1, true).replace("\\n", "\n");
        storage.set("chatmessage", message);
        storage.set("chatmessagetime", (System.currentTimeMillis() / 1000));
        MessageBuilder.create(hms, "cmdChatMessageSet", "Chat")
                .addPlaceholderReplace("%MESSAGE%", message)
                .sendMessage(sender);
    }

    /**
     * Ensures that the message has been set, while setting it as a field var
     *
     * @return true if a message has been set
     */
    private boolean verifyMessage() {

        message = storage.getString("chatmessage");

        if (message.length() == 0) {

            MessageBuilder.create(hms, "cmdChatMessageNotSet", "Chat").sendMessage(sender);
            return false;
        } else {

            //Clear message if it is old, only keep it for 10 minutes
            long time = storage.getLong("chatmessagetime");
            if (time + 600 < (System.currentTimeMillis() / 1000)) {

                message = "";
                storage.set("chatmessage", null);

                MessageBuilder.create(hms, "cmdChatMessageNotSet", "Chat").sendMessage(sender);
                return false;
            } else return true;
        }
    }

    private boolean hasAdvancedPermission() {
        boolean hasPerm = sender.hasPermission("hms.chat.advanced");
        if (!hasPerm) sendNoPermissionMessage("Chat");
        return hasPerm;
    }

    private void showUsage() {
        MessageBuilder.create(hms, "cmdChatUsage", "Chat").sendMessage(sender);
    }
}
