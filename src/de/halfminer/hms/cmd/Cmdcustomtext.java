package de.halfminer.hms.cmd;

import de.halfminer.hms.exception.CachingException;
import de.halfminer.hms.util.CustomtextCache;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.logging.Level;

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
    public void execute() {

        if (args.length == 0) return;

        CustomtextCache cache;
        try {
            cache = storage.getCache("customtext.txt");
        } catch (CachingException e) {
            MessageBuilder.create(hms, "errorOccurred", "Info").sendMessage(sender);
            e.printStackTrace();
            return;
        }

        try {

            List<String> chapter = cache.getChapter(args);

            for (String rawLine : chapter) {

                MessageBuilder builder = MessageBuilder.create(hms, rawLine)
                        .setMode(MessageBuilder.Mode.DIRECT_STRING)
                        .toggleClickableCommands()
                        .addPlaceholderReplace("%PLAYER%", Utils.getPlayername(sender))
                        .addPlaceholderReplace("%ARGS%", Utils.arrayToString(args, 0, false));

                // check for command (only for players)
                if (isPlayer) {

                    String message = builder.returnMessage();

                    Player player = (Player) sender;
                    if (message.startsWith("~>")) {

                        player.chat(message.substring(2).trim());
                        continue;
                    } else if (message.startsWith("~~>")) {

                        String command = message.substring(3).trim();
                        if (command.startsWith("/")) command = command.substring(1);

                        server.dispatchCommand(server.getConsoleSender(), command);
                        continue;
                    }
                }

                builder.toggleClickableCommands().sendMessage(sender);
            }

        } catch (CachingException e) {

            if (e.getReason().equals(CachingException.Reason.CHAPTER_NOT_FOUND)
                    || e.getReason().equals(CachingException.Reason.FILE_EMPTY)) {
                MessageBuilder.create(hms, "cmdCustomtextNotFound", "Info").sendMessage(sender);
            } else {
                MessageBuilder.create(hms, "errorOccurred", "Info").sendMessage(sender);
                MessageBuilder.create(hms, "utilCustomtextCacheParseError")
                        .addPlaceholderReplace("%ERROR%", e.getCleanReason())
                        .logMessage(Level.WARNING);
            }
        }
    }
}
