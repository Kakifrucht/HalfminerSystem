package de.halfminer.hmb.arena;

import de.halfminer.hmb.arena.abs.AbstractArena;
import de.halfminer.hmb.mode.abs.BattleModeType;
import de.halfminer.hmb.mode.DuelMode;
import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.handler.HanTitles;
import de.halfminer.hms.util.Message;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Level;

/**
 * Custom duel kit arena used by {@link DuelMode}, implementing countdowns and preparing fight
 */
@SuppressWarnings("unused")
public class DuelArena extends AbstractArena {

    private BukkitTask task;
    private boolean useKit;

    public DuelArena(String name) {
        super(BattleModeType.DUEL, name);
    }

    @Override
    public boolean isFree() {
        return super.isFree() && playersInArena.size() == 0;
    }

    public void gameStart(Player playerA, Player playerB, boolean useKit) {

        addPlayers(playerA, playerB);
        playersInArena.forEach(p -> p.setWalkSpeed(0.0f));
        this.useKit = useKit;

        task = scheduler.runTaskTimer(hmb, new Runnable() {

            private final HanTitles titles = HalfminerSystem.getInstance().getTitlesHandler();
            private final DuelMode mode = (DuelMode) getBattleMode();
            private final int timeStart = mode.getDuelTime();
            private int timeLeft = timeStart + 6;

            @Override
            public void run() {

                timeLeft -= 1;
                if (timeLeft > timeStart) {
                    String toSend = Message.create("modeDuelTitleCountdown", hmb)
                            .togglePrefix()
                            .addPlaceholder("%TIME%", timeLeft - timeStart)
                            .returnMessage();

                    titles.sendTitle(playerA, toSend, 0, 21, 0);
                    titles.sendTitle(playerB, toSend, 0, 21, 0);
                    playPlingSound();
                }

                // Battle is starting, reset walkspeed and give the kit
                if (timeLeft == timeStart) {
                    for (Player player : playersInArena) {
                        player.setWalkSpeed(0.2F);
                        Message.create("modeDuelGameStarting", hmb).send(player);
                        titles.sendTitle(player, Message.returnMessage("modeDuelTitleStart", hmb, false),
                                0, 30, 0);
                        equipPlayer(useKit, player);
                    }
                    teleportIntoArena();
                }

                if (timeLeft <= 5 && timeLeft > 0) {
                    if (timeLeft == 5) {
                        Message.create("modeDuelTimeRunningOut", hmb).send(playerA, playerB);
                    }
                    playPlingSound();
                }

                if (timeLeft <= 0) {
                    mode.getQueue().gameHasFinished(playerA, false);
                }
            }

            private void playPlingSound() {
                playersInArena.forEach(p -> p.playSound(p.getLocation(), Sound.BLOCK_NOTE_CHIME, 1.0f, 1.0f));
            }

        }, 0L, 20L);
    }

    public void gameEnd() {
        task.cancel();
        restorePlayers();
    }

    public boolean isUseKit() {
        return useKit;
    }

    @Override
    public boolean forceGameEnd() {
        if (playersInArena.size() > 0) {
            Message.create("modeDuelGameEndForced", hmb)
                    .send(playersInArena.getFirst(), playersInArena.getLast());
            Message.create("modeDuelGameEndForcedLog", hmb)
                    .addPlaceholder("%ARENA%", getName())
                    .addPlaceholder("%PLAYERA%", playersInArena.getFirst().getName())
                    .addPlaceholder("%PLAYERB", playersInArena.getLast().getName())
                    .log(Level.INFO);
            gameEnd();
            return true;
        }

        return false;
    }
}
