package de.halfminer.hmb.arena;

import de.halfminer.hmb.arena.abs.AbstractKitArena;
import de.halfminer.hmb.enums.BattleModeType;
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
@SuppressWarnings("unused")
public class DuelArena extends AbstractKitArena {

    private BukkitTask task;
    private boolean useKit;
    private boolean restoreInventory;

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
        // ensures that the players get their stuff back if duel is stopped
        // during 5 seconds countdown, if fighting with own stuff
        restoreInventory = true;

        task = Bukkit.getScheduler().runTaskTimerAsynchronously(hmb, new Runnable() {

            private final HanTitles titles = (HanTitles) HalfminerSystem.getInstance().getHandler(HandlerType.TITLES);
            private final BukkitScheduler scheduler = hmb.getServer().getScheduler();
            private final DuelMode mode = (DuelMode) getBattleMode();
            private final int timeStart = mode.getDuelTime();
            private int timeLeft = timeStart + 6;

            @Override
            public void run() {

                timeLeft -= 1;
                if (timeLeft > timeStart) {
                    final String toSend = MessageBuilder.create("modeDuelTitleCountdown", hmb)
                            .togglePrefix()
                            .addPlaceholderReplace("%TIME%", String.valueOf(timeLeft - timeStart))
                            .returnMessage();

                    scheduler.runTask(hmb, () -> {
                        titles.sendTitle(playerA, toSend, 0, 21, 0);
                        titles.sendTitle(playerB, toSend, 0, 21, 0);
                        playSound(Sound.BLOCK_NOTE_PLING);
                    });
                }

                // Battle is starting, reset walkspeed and give the kit
                if (timeLeft == timeStart) {
                    scheduler.runTask(hmb, () -> {
                        playSound(Sound.BLOCK_ANVIL_LAND);
                        preparePlayer(playerA);
                        preparePlayer(playerB);
                        teleportIntoArena();
                    });
                }

                if (timeLeft <= 5 && timeLeft > 0) {
                    if (timeLeft == 5) {
                        MessageBuilder.create("modeDuelTimeRunningOut", hmb)
                                .sendMessage(playerA, playerB);
                    }
                    playSound(Sound.BLOCK_NOTE_PLING);
                }

                if (timeLeft <= 0) {
                    scheduler.runTask(hmb,
                            () -> mode.getQueue().gameHasFinished(playerA, false, false));
                }
            }

            private void preparePlayer(Player player) {
                player.setWalkSpeed(0.2F);
                MessageBuilder.create("modeDuelGameStarting", hmb).sendMessage(player);
                titles.sendTitle(player, MessageBuilder.returnMessage("modeDuelTitleStart", hmb, false), 0, 30, 0);

                if (useKit) equipPlayers();
                else {
                    pm.restorePlayerInventory(player);
                    restoreInventory = false;
                }
            }

            private void playSound(Sound toPlay) {
                playersInArena.forEach(p -> p.playSound(p.getLocation(), toPlay, 1.0f, 1.6f));
            }

        }, 0L, 20L);
    }

    public void gameEnd() {
        task.cancel();
        restorePlayers(useKit || restoreInventory);
    }

    public boolean isUseKit() {
        return useKit;
    }
}
