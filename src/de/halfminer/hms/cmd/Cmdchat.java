package de.halfminer.hms.cmd;

import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.TitleSender;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings("unused")
public class Cmdchat extends BaseCommand {

    private CommandSender sender;
    private String message;

    public Cmdchat() {
        this.permission = "hms.chat";
    }

    @Override
    public void run(CommandSender sender, String label, String[] args) {

        this.sender = sender;

        if (!sender.hasPermission("hms.chat.advanced")) {
            clearChat();
            return;
        }

        if (args.length > 0) {

            if (args[0].equalsIgnoreCase("clear")) {

                clearChat();

            } else if (args[0].equalsIgnoreCase("countdown") && args.length == 2) {

                final int countdown;
                try {
                    countdown = Integer.decode(args[1]);
                } catch (NumberFormatException e) {
                    showUsage();
                    return;
                }
                if (countdown > 30) showUsage();
                else {
                    hms.getServer().getScheduler().runTaskAsynchronously(hms, new Runnable() {
                        @Override
                        public void run() {
                            int count = countdown;
                            for (; count >= 0; count--) {
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

            } else if (args[0].equalsIgnoreCase("title") && args.length > 1 && verifyMessage()) {

                if (message.length() > 0) {
                    int time;
                    try {
                        time = Integer.decode(args[1]);
                    } catch (NumberFormatException e) {
                        showUsage();
                        return;
                    }
                    TitleSender.sendTitle(null, message.replace("\\n", "\n"), 10, time * 20 - 20, 10);
                }

            } else if (args[0].equalsIgnoreCase("news") && verifyMessage()) {

                storage.set("sys.news", message);
                hms.getModMotd().reloadConfig();
                if (sender instanceof Player) {
                    TitleSender.sendTitle((Player) sender, Language.getMessagePlaceholderReplace("modStaticListenersNewsFormat",
                            false, "%NEWS%", message), 40, 180, 40);
                }
                sender.sendMessage(Language.getMessagePlaceholderReplace("commandChatNewsSetTo", true,
                        "%PREFIX%", "Chat"));

            } else if (args[0].equalsIgnoreCase("alle") && verifyMessage()) {

                hms.getServer().broadcast(message, "hms.default");
                sender.sendMessage(Language.getMessagePlaceholderReplace("commandChatSendToAll", true, "%PREFIX%", "Chat"));

            } else if (args[0].equalsIgnoreCase("spieler") && args.length > 1 && verifyMessage()) {

                Player player = hms.getServer().getPlayer(args[1]);
                if (player != null) {
                    player.sendMessage(message);
                    sender.sendMessage(Language.getMessagePlaceholderReplace("commandChatSendToPlayer", true, "%PREFIX%",
                            "Chat", "%PLAYER%", player.getName()));
                } else {
                    sender.sendMessage(Language.getMessagePlaceholderReplace("playerNotOnline", true, "%PREFIX%", "Chat"));
                }

            } else {

                String message = Language.arrayToString(args, 0, true);
                storage.set("sys.chatmessage", message);
                sender.sendMessage(Language.getMessagePlaceholderReplace("commandChatMessageSet", true, "%PREFIX%", "Chat",
                        "%MESSAGE%", message));

            }

        } else showUsage();
    }

    /**
     * Ensures that the message has been set, while setting it as a field var
     *
     * @return true if a message has been set
     */
    private boolean verifyMessage() {
        message = storage.getString("sys.chatmessage");
        if (message.length() == 0) {
            sender.sendMessage(Language.getMessagePlaceholderReplace("commandChatMessageNotSet", true,
                    "%PREFIX%", "Chat"));
            return false;
        } else return true;
    }

    private void clearChat() {

        String whoCleared = sender.getName();
        if (whoCleared.equals("CONSOLE")) whoCleared = Language.getMessage("consoleName");

        for (Player player : hms.getServer().getOnlinePlayers()) {
            if (!player.hasPermission("hms.chat.advanced")) {
                for (int i = 0; i < 100; i++) player.sendMessage(ChatColor.RESET.toString());
            }
            player.sendMessage(Language.getMessagePlaceholderReplace("commandChatCleared", true, "%PREFIX%", "Chat",
                    "%PLAYER%", whoCleared));
        }

        hms.getLogger().info(Language.getMessagePlaceholderReplace("commandChatClearedLog", false, "%PLAYER%", whoCleared));
    }

    private void showUsage() {
        sender.sendMessage(Language.getMessagePlaceholderReplace("commandChatUsage", true, "%PREFIX%", "Chat"));
    }

}
