package de.halfminer.hms.cmd;

import de.halfminer.hms.exception.CachingException;
import de.halfminer.hms.util.CustomtextCache;
import de.halfminer.hms.util.Language;
import net.md_5.bungee.api.chat.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * - Displays custom text data
 * - Should be binded via Bukkit aliases (commands.yml in server root)
 * - Utilizes CustomtextCache
 * - Extends CustomtextCache with syntax elements
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
            if (!(sender instanceof Player)) {
                for (String send : text)
                    sender.sendMessage(Language.placeholderReplace(send, "%PLAYER%", Language.getMessage("consoleName")));
                return;
            }

            for (String raw : text) {

                // placeholder replace, send message directly if not starting with '\' character
                String send = Language.placeholderReplace(raw, "%PLAYER%", sender.getName());
                if (!send.startsWith("\\")) {
                    sender.sendMessage(send);
                    continue;
                }

                // build json message via component api
                List<BaseComponent> components = new ArrayList<>();

                StringBuilder sbText = new StringBuilder();
                StringBuilder sbCommand = new StringBuilder();
                boolean readingCommand = false;

                for (int i = 1; i < send.length(); i++) {

                    char current = send.charAt(i);

                    if (current == '/') {

                        if (readingCommand) {

                            // if command was not escaped
                            if (sbCommand.length() > 1) {
                                TextComponent commandComponent = new TextComponent(sbCommand.toString());
                                BaseComponent lastComponent = components.get(components.size() - 1);

                                // set color/boldness from last component, always set clickable commands italic
                                commandComponent.setColor(lastComponent.getColor());
                                commandComponent.setBold(lastComponent.isBold());
                                commandComponent.setItalic(true);

                                commandComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                        sbCommand.toString()));
                                commandComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        new ComponentBuilder(Language.getMessage("cmdCustomtextClick")).create()));

                                components.add(commandComponent);
                            } else sbText.append("/");

                            sbCommand = new StringBuilder();
                            readingCommand = false;
                        } else {

                            addToFromLegacyText(components, sbText.toString());
                            sbText = new StringBuilder();
                            readingCommand = true;
                        }
                    }

                    if (readingCommand) sbCommand.append(current);
                    else if (current != '/') sbText.append(current);
                }

                addToFromLegacyText(components, sbText.toString());
                TextComponent componentToSend = new TextComponent();
                components.forEach(componentToSend::addExtra);

                ((Player) sender).spigot().sendMessage(componentToSend);
            }

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

    private void addToFromLegacyText(List<BaseComponent> addTo, String legacyText) {
        Collections.addAll(addTo, TextComponent.fromLegacyText(legacyText));
    }
}
