package de.halfminer.hmb.arena;

import de.halfminer.hmb.HalfminerBattle;
import de.halfminer.hmb.arena.abs.AbstractKitArena;
import de.halfminer.hmb.enums.GameModeType;
import de.halfminer.hmb.mode.DuelMode;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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

        task = Bukkit.getScheduler().runTaskTimer(hmb, new Runnable() {

            private final DuelMode mode = (DuelMode) hmb.getGameMode(GameModeType.DUEL);
            private int timeLeft = mode.getDuelTime();
            private final int timeStart = timeLeft - 5;

            @Override
            public void run() {

                timeLeft -= 5;
                if (timeLeft == timeStart) { // Battle is starting, reset walkspeed and give the kit
                    preparePlayer(playerA);
                    preparePlayer(playerB);
                    teleportIntoArena();
                }
                if (timeLeft <= 15 && timeLeft > 0) {
                    MessageBuilder.create(hmb, "modeDuelTimeRunningOut", HalfminerBattle.PREFIX)
                            .addPlaceholderReplace("%TIME%", Integer.toString(timeLeft))
                            .sendMessage(playerA, playerB);
                }
                if (timeLeft <= 0) {
                    mode.getQueue().gameHasFinished(playerA, false, false);
                    return;
                }
                if (timeLeft <= -1) { //just to safeguard from having alot of tasks that do not cancel and throw exceptions
                    task.cancel();
                }
            }

            private void preparePlayer(Player player) {
                player.setWalkSpeed(0.2F);
                MessageBuilder.create(hmb, "modeDuelGameStarting", HalfminerBattle.PREFIX).sendMessage(player);
                equipPlayers();
            }

        }, 100L, 100L);
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
