package de.halfminer.hmb.arena;

import de.halfminer.hmb.HalfminerBattle;
import de.halfminer.hmb.arena.abs.AbstractKitArena;
import de.halfminer.hmb.enums.GameModeType;
import de.halfminer.hmb.mode.DuelMode;
import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.handlers.HanTitles;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

/**
 * Custom duel kit arena used by {@link DuelMode}, implementing countdowns and preparing fight
 */
public class DuelArena extends AbstractKitArena {

    private BukkitTask task;

    public DuelArena(String name) {
        super(GameModeType.DUEL, name);
    }

    public void gameStart(Player playerA, Player playerB) {

        if (!isFree())
            throw new RuntimeException("Arena " + this.getName()
                    + " currently not free while trying to start duel between " + playerA + " and " + playerB);

        addPlayers(playerA, playerB);
        storeAndClearPlayers();
        healPlayers();
        for (Player player : playersInArena) {
            player.setWalkSpeed(0.0f);
        }

        MessageBuilder.create(hmb, "modeDuelCountdownStart", HalfminerBattle.PREFIX)
                .addPlaceholderReplace("%PLAYER%", playerB.getName())
                .addPlaceholderReplace("%ARENA%", getName())
                .sendMessage(playerA);
        MessageBuilder.create(hmb, "modeDuelCountdownStart", HalfminerBattle.PREFIX)
                .addPlaceholderReplace("%PLAYER%", playerA.getName())
                .addPlaceholderReplace("%ARENA%", getName())
                .sendMessage(playerB);

        task = Bukkit.getScheduler().runTaskTimerAsynchronously(hmb, new Runnable() {

            private final HanTitles titles = (HanTitles) HalfminerSystem.getInstance().getHandler(HandlerType.TITLES);
            private final BukkitScheduler scheduler = hmb.getServer().getScheduler();
            private final DuelMode mode = (DuelMode) hmb.getGameMode(GameModeType.DUEL);
            private final int timeStart = mode.getDuelTime();
            private int timeLeft = timeStart + 5;

            @Override
            public void run() {

                if (timeLeft > timeStart) {
                    final String toSend = MessageBuilder.create(hmb, "modeDuelTitleCountdown")
                            .addPlaceholderReplace("%TIME%", String.valueOf(timeLeft - timeStart))
                            .returnMessage();

                    scheduler.runTask(hmb, () -> {
                        titles.sendTitle(playerA, toSend, 0, 21, 0);
                        titles.sendTitle(playerB, toSend, 0, 21, 0);
                        playSound(playerA, Sound.BLOCK_NOTE_PLING);
                        playSound(playerB, Sound.BLOCK_NOTE_PLING);
                    });
                }
                // Battle is starting, reset walkspeed and give the kit
                if (timeLeft == timeStart) {
                    scheduler.runTask(hmb, () -> {
                        preparePlayer(playerA);
                        preparePlayer(playerB);
                        teleportIntoArena();
                    });
                }
                if (timeLeft <= 5 && timeLeft > 0) {
                    playSound(playerA, Sound.BLOCK_NOTE_PLING);
                }
                if (timeLeft <= 1) {
                    scheduler.runTask(hmb,
                            () -> mode.getQueue().gameHasFinished(playerA, false, false));
                }
                timeLeft -= 1;
            }

            private void preparePlayer(Player player) {
                player.setWalkSpeed(0.2F);
                MessageBuilder.create(hmb, "modeDuelGameStarting", HalfminerBattle.PREFIX).sendMessage(player);
                titles.sendTitle(player, MessageBuilder.returnMessage(hmb, "modeDuelTitleStart"), 0, 30, 0);
                playSound(player, Sound.BLOCK_ANVIL_LAND);
                equipPlayers();
            }

            private void playSound(Player player, Sound toPlay) {
                player.playSound(player.getLocation(), toPlay, 1.0f, 1.6f);
            }

        }, 0L, 20L);
    }

    /**
     * Called once a game ends. Will issue restoring of players and resets arena.
     * This happens due to death of a player (duel win), time running out, reloading of plugin
     * or logging out.
     */
    public void gameEnd() {
        task.cancel();
        restorePlayers();
        playersInArena.clear();
    }

    @Override
    public boolean isFree() {
        return super.isFree() && playersInArena.size() == 0;
    }
}
