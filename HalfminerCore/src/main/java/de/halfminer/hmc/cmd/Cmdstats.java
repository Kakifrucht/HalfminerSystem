package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
import de.halfminer.hmc.module.ModSkillLevel;
import de.halfminer.hmc.module.ModuleType;
import de.halfminer.hms.handler.storage.DataType;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.handler.storage.PlayerNotFoundException;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * - View own / other players stats
 * - Allows to compare statistics easily
 */
@SuppressWarnings("unused")
public class Cmdstats extends HalfminerCommand {

    public Cmdstats() {
        this.permission = "hmc.stats";
    }

    @Override
    public void execute() {

        if (args.length > 0) {

            boolean compare = args.length > 1 && args[1].equalsIgnoreCase("compare") && sender instanceof Player;
            try {
                showStats(storage.getPlayer(args[0]), compare);
            } catch (PlayerNotFoundException e) {
                e.sendNotFoundMessage(sender, "Stats");
            }

        } else {

            if (isPlayer) {
                showStats(storage.getPlayer((Player) sender), false);
            } else {
                sendNotAPlayerMessage("Stats");
            }
        }
    }

    private void showStats(final HalfminerPlayer player, boolean compare) {

        HalfminerPlayer compareWith = null;
        if (compare && !sender.equals(player.getBase())) {
            compareWith = storage.getPlayer((Player) sender);
        }

        MessageBuilder.create("cmdStatsHeader", hmc).sendMessage(sender);
        MessageBuilder.create("cmdStatsShow", hmc)
                .addPlaceholder("%PLAYER%", player.getName())
                .addPlaceholder("%SKILLGROUP%",
                        ((ModSkillLevel) hmc.getModule(ModuleType.SKILL_LEVEL)).getSkillgroup(player.getBase()))
                .addPlaceholder("%SKILLLEVEL%", getIntAndCompare(player, DataType.SKILL_LEVEL, compareWith))
                .addPlaceholder("%ONLINETIME%", getIntAndCompare(player, DataType.TIME_ONLINE, compareWith))
                .addPlaceholder("%KILLS%", getIntAndCompare(player, DataType.KILLS, compareWith))
                .addPlaceholder("%DEATHS%", getIntAndCompare(player, DataType.DEATHS, compareWith))
                .addPlaceholder("%KDRATIO%", getDoubleAndCompare(player, DataType.KD_RATIO, compareWith))
                .addPlaceholder("%VOTES%", getIntAndCompare(player, DataType.VOTES, compareWith))
                .addPlaceholder("%REVENUE%", getDoubleAndCompare(player, DataType.REVENUE, compareWith))
                .addPlaceholder("%MOBKILLS%", getIntAndCompare(player, DataType.MOB_KILLS, compareWith))
                .addPlaceholder("%BLOCKSPLACED%", getIntAndCompare(player, DataType.BLOCKS_PLACED, compareWith))
                .addPlaceholder("%BLOCKSBROKEN%", getIntAndCompare(player, DataType.BLOCKS_BROKEN, compareWith))
                .sendMessage(sender);

        // filter out current name
        List<String> previousNames = player.getPreviousNames().stream()
                .filter(previousName -> !previousName.equalsIgnoreCase(player.getName()))
                .collect(Collectors.toList());

        if (!previousNames.isEmpty()) {

            List<String> previousNamesToDisplay = new ArrayList<>();

            int toDisplayMax = hmc.getConfig().getInt("command.stats.previousNamesMax", 4);
            if (previousNames.size() > toDisplayMax && previousNames.size() > 1) {

                // add first and last known name, fill up with random names
                Set<String> displayedNamesSet = new HashSet<>();
                displayedNamesSet.add(previousNames.get(0));
                displayedNamesSet.add(previousNames.get(previousNames.size() - 1));

                List<String> previousNamesCopy = new ArrayList<>(previousNames);
                previousNamesCopy.removeAll(displayedNamesSet);
                Collections.shuffle(previousNamesCopy);
                for (int i = 0; i < (toDisplayMax - 2); i++) {
                    displayedNamesSet.add(previousNamesCopy.get(i));
                }

                // add selected names sorted to displayed list
                for (String previousName : previousNames) {
                    if (displayedNamesSet.contains(previousName)) {
                        previousNamesToDisplay.add(previousName);
                    }
                }

            } else {
                previousNamesToDisplay.addAll(previousNames);
            }

            final String spacer = MessageBuilder.returnMessage("cmdStatsPreviousNamesSpacer", hmc, false);
            StringBuilder sb = new StringBuilder();
            for (String displayedName : previousNamesToDisplay) {
                sb.append(displayedName).append(spacer);
            }
            sb.setLength(sb.length() - spacer.length());

            MessageBuilder.create("cmdStatsPreviousNames", hmc)
                    .addPlaceholder("%PREVIOUSNAMES%", sb.toString())
                    .sendMessage(sender);
        }

        if (sender.equals(player.getBase())) {
            MessageBuilder.create("cmdStatsShowotherStats", hmc).sendMessage(sender);
        } else if (compare) {
            MessageBuilder.create("cmdStatsCompareLegend", hmc).sendMessage(sender);
        } else if (sender instanceof Player) {
            MessageBuilder.create("cmdStatsCompareInfo", hmc)
                    .addPlaceholder("%PLAYER%", player.getName())
                    .sendMessage(sender);
        }

        MessageBuilder.create("lineSeparator").sendMessage(sender);
    }

    @SuppressWarnings("Duplicates")
    private String getIntAndCompare(HalfminerPlayer player, DataType type, HalfminerPlayer compareWith) {
        String returnString = "";
        int playerVar = player.getInt(type);

        if (compareWith != null) {
            int compareVar = compareWith.getInt(type);
            if (playerVar < compareVar) returnString = ChatColor.GREEN.toString();
            else if (playerVar > compareVar) returnString = ChatColor.RED.toString();
            else returnString = ChatColor.YELLOW.toString();
        }

        if (type == DataType.TIME_ONLINE) playerVar /= 60;
        return returnString + playerVar;
    }

    @SuppressWarnings("Duplicates")
    private String getDoubleAndCompare(HalfminerPlayer player, DataType type, HalfminerPlayer compareWith) {

        // code duplicated, as Java doesn't offer simple ways to abstract primitive types
        String returnString = "";
        double playerVar = player.getDouble(type);

        if (compareWith != null) {
            double compareVar = compareWith.getDouble(type);
            if (playerVar < compareVar) returnString = ChatColor.GREEN.toString();
            else if (playerVar > compareVar) returnString = ChatColor.RED.toString();
            else returnString = ChatColor.YELLOW.toString();
        }

        return returnString + playerVar;
    }
}
