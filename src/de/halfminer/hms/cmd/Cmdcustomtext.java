package de.halfminer.hms.cmd;

import de.halfminer.hms.exception.CachingException;
import de.halfminer.hms.util.CustomtextCache;
import de.halfminer.hms.util.Language;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * - Displays custom text data
 * - Should be binded via Bukkit aliases (commands.yml in server root)
 * - Utilizes and extends CustomtextCache with syntax elements
 *   - Placeholder support for text
 *   - Make commands clickable by ending them with '/' character
 *     - A line must be started with '~' to be parsed
 *     - Commands will be printed in italic
 *   - Support for command execution
 *     - Lines starting with "~>" will make the player execute following text
 *     - Lines starting with "~~>" will make the console execute following text as command
 */
@SuppressWarnings("unused")
public class Cmdcustomtext extends HalfminerCommand {

    public Cmdcustomtext() {
        this.permission = "hms.customttext";
    }

    @Override
    public void run(CommandSender sender, String label, String[] args) {

        if (args.length == 0) return;

        CustomtextCache cache;
        try {
            cache = storage.getCache("customtext.txt");
        } catch (CachingException e) {
            sender.sendMessage(Language.getMessagePlaceholders("errorOccurred", true, "%PREFIX%", "Info"));
            e.printStackTrace();
            return;
        }

        String chapterName = Language.arrayToString(args, 0, false);

        try {

            List<String> chapter = cache.getChapter(chapterName);
            boolean senderIsPlayer = sender instanceof Player;

            for (String raw : chapter) {

                String placeholderReplaced = Language.placeholderReplace(raw, "%PLAYER%",
                        senderIsPlayer ? sender.getName() : Language.getMessage("consoleName"),
                        "%ARGS%", chapterName);

                // check for command (only for players)
                if (senderIsPlayer) {

                    Player player = (Player) sender;
                    if (placeholderReplaced.startsWith("~>")) {

                        player.chat(placeholderReplaced.substring(2).trim());
                        continue;
                    } else if (placeholderReplaced.startsWith("~~>")) {

                        String command = placeholderReplaced.substring(3).trim();
                        if (command.startsWith("/")) command = command.substring(1);

                        server.dispatchCommand(server.getConsoleSender(), command);
                        continue;
                    }
                }

                Language.sendParsedText(sender, placeholderReplaced);
            }


        } catch (CachingException e) {

            if (e.getReason().equals(CachingException.Reason.CHAPTER_NOT_FOUND)
                    || e.getReason().equals(CachingException.Reason.FILE_EMPTY)) {
                sender.sendMessage(Language.getMessagePlaceholders("cmdCustomtextNotFound", true, "%PREFIX%", "Info"));
            } else {
                sender.sendMessage(Language.getMessagePlaceholders("errorOccurred", true, "%PREFIX%", "Info"));
                hms.getLogger().warning(Language.getMessagePlaceholders("cmdCustomtextErrorLog",
                        false, "%ERROR%", e.getCleanReason()));
            }
        }
    }
}
