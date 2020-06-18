package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
import de.halfminer.hmc.module.ModChatManager;
import de.halfminer.hmc.module.ModuleDisabledException;
import de.halfminer.hmc.module.ModuleType;
import de.halfminer.hms.handler.HanBossBar;
import de.halfminer.hms.handler.HanTitles;
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
 * - Ring players to get their attention
 * - Countdown via bossbar
 * - Send custom messages to player or broadcast
 * - Set news and motd message
 */
@SuppressWarnings({"unused", "BooleanMethodIsAlwaysInverted"})
public class Cmdchat extends HalfminerCommand {

    private final HanBossBar bossBar = hms.getBarHandler();
    private final HanTitles titles = hms.getTitlesHandler();
    private String message;

    public Cmdchat() {
        this.permission = "hmc.chat";
    }

    @Override
    public void execute() throws ModuleDisabledException {

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
                    ((ModChatManager) hmc.getModule(ModuleType.CHAT_MANAGER)).toggleGlobalmute();
                    break;
                case "news":
                    if (!hasAdvancedPermission()) return;
                    setNews();
                    break;
                case "ring":
                    ringPlayer();
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
            } catch (NumberFormatException ignored) {}

            if (args.length > 2) sendTo = server.getPlayer(args[2]);
        }

        String recipientName;
        if (sendTo == null) recipientName = MessageBuilder.returnMessage("cmdChatAll", hmc);
        else recipientName = sendTo.getName();

        String notifySender = useBossbar ? "cmdChatBossbar" : "cmdChatTitle";

        MessageBuilder.create(notifySender, hmc, "Chat")
                .addPlaceholder("%SENDTO%", recipientName)
                .addPlaceholder("%TIME%", time)
                .addPlaceholder("%MESSAGE%", message)
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
                .filter(p -> !p.hasPermission("hmc.chat.advanced"))
                .forEach(p -> p.sendMessage(clearMessage.toString()));

        MessageBuilder.create("cmdChatCleared", hmc, "Chat")
                .addPlaceholder("%PLAYER%", whoCleared)
                .broadcastMessage(false);

        MessageBuilder.create("cmdChatClearedLog", hmc)
                .addPlaceholder("%PLAYER%", whoCleared)
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

            MessageBuilder.create("cmdChatCountdownStarted", hmc, "Chat")
                    .addPlaceholder("%COUNT%", countdown)
                    .sendMessage(sender);

            final HanBossBar bar = hms.getBarHandler();
            final BukkitTask task = scheduler.runTaskTimer(hmc, new Runnable() {

                int count = countdown;
                @Override
                public void run() {

                    if (count < 0) {
                        return;
                    }

                    bar.broadcastBar(MessageBuilder.create("cmdChatCountdown", hmc)
                                    .addPlaceholder("%COUNT%", count)
                                    .returnMessage(),
                            BarColor.GREEN, BarStyle.SOLID, 35, (double) count / countdown);
                    if (count-- == 0) {
                        for (Player p : hmc.getServer().getOnlinePlayers()) {
                            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
                        }
                    }
                }
            }, 0, 20L);

            scheduler.runTaskLater(hmc, () -> {

                task.cancel();
                bar.removeBar();
            }, 20 * (countdown + 4));
        }
    }

    private void ringPlayer() {

        if (args.length < 2) {
            showUsage();
            return;
        }

        final Player toRing = server.getPlayer(args[1]);
        String senderName = Utils.getPlayername(sender);

        if (toRing == null) {
            MessageBuilder.create("playerNotOnline", "Chat").sendMessage(sender);
            return;
        }

        hms.getTitlesHandler().sendTitle(toRing,
                MessageBuilder.create("cmdChatRingTitle", hmc)
                        .addPlaceholder("%PLAYER%", senderName)
                        .returnMessage());

        MessageBuilder.create("cmdChatRingMessage", hmc, "Chat")
                .addPlaceholder("%PLAYER%", senderName)
                .sendMessage(toRing);

        MessageBuilder.create("cmdChatRingSent", hmc, "Chat")
                .addPlaceholder("%PLAYER%", toRing.getName())
                .sendMessage(sender);

        scheduler.runTaskAsynchronously(hmc, () -> {
            float ringHeight = 2.0f;
            boolean drop = true;
            for (int i = 0; i < 19; i++) {

                toRing.playSound(toRing.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, ringHeight);

                if (ringHeight == 2.0f) drop = true;
                else if (ringHeight == 0.5f) drop = false;

                if (drop) ringHeight -= 0.5f;
                else ringHeight += 0.5f;

                try {
                    Thread.sleep(110L);
                } catch (InterruptedException ignored) {}
            }
        });
    }

    private void setNews() {

        if (!verifyMessage()) return;

        coreStorage.set("news", message);
        if (sender instanceof Player) {
            bossBar.sendBar((Player) sender, MessageBuilder.create("modTitlesNewsFormat", hmc)
                    .addPlaceholder("%NEWS%", message)
                    .returnMessage(), BarColor.YELLOW, BarStyle.SOLID, 5);
            titles.sendTitle((Player) sender, " \n" + message, 10, 100, 10);
        }

        // try to reload MOTD module, as it reads it's string from storage (which we have now updated)
        try {
            hmc.getModule(ModuleType.MOTD).loadConfig();
        } catch (ModuleDisabledException ignored) {}
        MessageBuilder.create("cmdChatNewsSetTo", hmc, "Chat").sendMessage(sender);
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

                MessageBuilder.create("playerNotOnline", "Chat").sendMessage(sender);
                return;
            }
        } else {

            server.broadcast(message, "hmc.default");
            sendToString = MessageBuilder.returnMessage("cmdChatAll", hmc);
        }

        MessageBuilder.create("cmdChatSend", hmc, "Chat")
                .addPlaceholder("%SENDTO%", sendToString)
                .sendMessage(sender);
    }

    private void sendCommand() {

        if (args.length < 3) {
            showUsage();
            return;
        }

        Player directRecipient = server.getPlayer(args[1]);
        if (directRecipient == null) {
            MessageBuilder.create("playerNotOnline", "Chat").sendMessage(sender);
            return;
        }

        String command = Utils.arrayToString(args, 2, false);
        if (!command.startsWith("/")) command = "/" + command;

        MessageBuilder.create("cmdChatClickableCommand", hmc, Utils.getPlayername(sender))
                .addPlaceholder("%PLAYER%", directRecipient.getName())
                .addPlaceholder("%COMMAND%", command + "/") // append slash since it will be parsed
                .broadcastMessage(false);

        directRecipient.playSound(directRecipient.getLocation(), Sound.BLOCK_NOTE_BELL, 0.5f, 1.0f);
    }

    private void setMessage() {

        if (args.length < 2) {
            showUsage();
            return;
        }

        String message = Utils.arrayToString(args, 1, true).replace("\\n", "\n");
        coreStorage.set("chatmessage", message);
        coreStorage.set("chatmessagetime", (System.currentTimeMillis() / 1000));
        MessageBuilder.create("cmdChatMessageSet", hmc, "Chat")
                .addPlaceholder("%MESSAGE%", message)
                .sendMessage(sender);
    }

    /**
     * Ensures that the message has been set, while setting it as a field var
     *
     * @return true if a message has been set
     */
    private boolean verifyMessage() {

        message = coreStorage.getString("chatmessage");

        if (message.length() == 0) {

            MessageBuilder.create("cmdChatMessageNotSet", hmc, "Chat").sendMessage(sender);
            return false;
        } else {

            //Clear message if it is old, only keep it for 10 minutes
            long time = coreStorage.getLong("chatmessagetime");
            if (time + 600 < (System.currentTimeMillis() / 1000)) {

                message = "";
                coreStorage.set("chatmessage", null);

                MessageBuilder.create("cmdChatMessageNotSet", hmc, "Chat").sendMessage(sender);
                return false;
            } else return true;
        }
    }

    private boolean hasAdvancedPermission() {
        boolean hasPerm = sender.hasPermission("hmc.chat.advanced");
        if (!hasPerm) sendNoPermissionMessage("Chat");
        return hasPerm;
    }

    private void showUsage() {
        MessageBuilder.create("cmdChatUsage", hmc, "Chat").sendMessage(sender);
    }
}
