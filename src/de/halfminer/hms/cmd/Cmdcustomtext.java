package de.halfminer.hms.cmd;

import de.halfminer.hms.exception.CachingException;
import de.halfminer.hms.util.CustomtextCache;
import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * - Displays custom text data
 * - Should be binded via Bukkit aliases (commands.yml in server root)
 * - Utilizes and extends CustomtextCache with syntax elements
 *   - Placeholder support for text
 *   - Make commands clickable by ending them with '/' character
 *     - A line must be started with '\' (backslash character) to be parsed
 *     - Escape auto command via "//"
 *     - Commands are written in italic
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

        String chapter = Language.arrayToString(args, 0, false);

        try {
            List<String> text = cache.getChapter(chapter);

            if (sender instanceof Player) {

                for (String raw : text) {

                    // placeholder replace, send message directly if not starting with '\' character
                    String send = Language.placeholderReplace(raw, "%PLAYER%", sender.getName());
                    if (!send.startsWith("\\")) sender.sendMessage(send);
                    else ((Player) sender).spigot().sendMessage(Utils.makeCommandsClickable(send.substring(1)));
                }
            } else for (String send : text)
                sender.sendMessage(Language.placeholderReplace(send, "%PLAYER%", Language.getMessage("consoleName")));

        } catch (CachingException e) {

            if (e.getReason().equals(CachingException.Reason.CHAPTER_NOT_FOUND)
                    || e.getReason().equals(CachingException.Reason.FILE_EMPTY)) {
                sender.sendMessage(Language.getMessagePlaceholders("cmdCustomtextNotFound", true, "%PREFIX%", "Info"));
            } else {
                sender.sendMessage(Language.getMessagePlaceholders("errorOccurred", true, "%PREFIX%", "Info"));
                hms.getLogger().warning(Language.getMessagePlaceholders("cmdCustomtextErrorLog",
                        false, "%ERROR%", e.getReason().toString()));
            }
        }
    }
}
