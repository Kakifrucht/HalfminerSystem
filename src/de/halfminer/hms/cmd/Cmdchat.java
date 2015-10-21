package de.halfminer.hms.cmd;

import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.TitleSender;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings("unused")
public class Cmdchat extends BaseCommand {

    public Cmdchat() {
        this.permission = "hms.chat";
    }

    @Override
    public void run(CommandSender sender, Command cmd, String label, String[] args) {

        if (!sender.hasPermission("hms.chat.advanced")) {
            clearChat(sender);
            return;
        }

        if (args.length > 0) {

            if (args[0].equalsIgnoreCase("clear")) clearChat(sender);
            else if (args[0].equalsIgnoreCase("countdown") && args.length == 2) {
                final int countdown;
                try {
                    countdown = Integer.decode(args[1]);
                } catch (NumberFormatException e) {
                    showUsage(sender);
                    return;
                }
                if (countdown > 30) showUsage(sender);
                else {
                    hms.getServer().getScheduler().runTaskAsynchronously(hms, new Runnable() {
                        @Override
                        public void run() {
                            int count = countdown;
                            for(; count >= 0; count--) {
                                TitleSender.sendTitle(null, Language.getMessagePlaceholderReplace("commandChatCountdown",
                                        false, "%COUNT%", String.valueOf(count)), 0, 20, 5);
                                try {
                                    Thread.sleep(1000l);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                }
            } else if (args[0].equalsIgnoreCase("globalmute")) {
                boolean active = storage.getBoolean("sys.globalmute");
                storage.set("sys.globalmute", !active);

                if (active) {
                    hms.getServer().broadcast(Language.getMessagePlaceholderReplace("commandChatGlobalmuteOff",
                            true, "%PREFIX%", "Globalmute"), "hms.default");
                } else {
                    hms.getServer().broadcast(Language.getMessagePlaceholderReplace("commandChatGlobalmuteOn",
                            true, "%PREFIX%", "Globalmute"), "hms.default");
                }
            } else if (args[0].equalsIgnoreCase("title") && args.length > 1) {
                String message = storage.getString("sys.chatmessage");
                if (message.length() > 0) {
                    int time;
                    try {
                        time = Integer.decode(args[1]);
                    } catch (NumberFormatException e) {
                        showUsage(sender);
                        return;
                    }
                    TitleSender.sendTitle(null, message.replace("\\n", "\n"), 10, time * 20 - 20, 10);
                } else {
                    sender.sendMessage(Language.getMessagePlaceholderReplace("commandChatMessageNotSet", true,
                            "%PREFIX%", "Chat"));
                }
            } else if (args[0].equalsIgnoreCase("news")) {
                String message = storage.getString("sys.chatmessage");
                if (message.length() > 0) {
                    hms.getModMotd().updateMotd(message);
                    storage.set("sys.news", message);
                    if (sender instanceof Player) {
                        TitleSender.sendTitle((Player) sender, Language.getMessagePlaceholderReplace("modStaticListenersNewsFormat",
                                false, "%NEWS%", message), 40, 180, 40);
                    }
                    sender.sendMessage(Language.getMessagePlaceholderReplace("commandChatNewsSetTo", true,
                            "%PREFIX%", "Chat"));
                } else {
                    sender.sendMessage(Language.getMessagePlaceholderReplace("commandChatMessageNotSet", true,
                            "%PREFIX%", "Chat"));
                }
            }

        } else showUsage(sender);
    }

    private void clearChat(CommandSender sender) {

        String whoCleared = sender.getName();
        if (whoCleared.equals("CONSOLE")) whoCleared = Language.getMessage("consoleName");

        for (Player player : hms.getServer().getOnlinePlayers()) {
            if (!player.hasPermission("hms.chat.bypass")) {
                for (int i = 0; i < 100; i++) player.sendMessage(ChatColor.RESET.toString());
            }
            player.sendMessage(Language.getMessagePlaceholderReplace("commandChatCleared", true, "%PREFIX%", "Chat",
                    "%PLAYER%", whoCleared));
        }

        hms.getLogger().info(Language.getMessagePlaceholderReplace("commandChatClearedLog", false, "%PLAYER%", whoCleared));
    }

    private void showUsage(CommandSender sender) {
        sender.sendMessage(Language.getMessagePlaceholderReplace("commandChatUsage", true, "%PREFIX%", "Chat"));
    }

}
