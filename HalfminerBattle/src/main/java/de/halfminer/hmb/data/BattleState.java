package de.halfminer.hmb.data;

/**
 * Possible states, tracked by {@link de.halfminer.hmb.data.BattlePlayer}
 */
public enum BattleState {
    IDLE,
    IN_QUEUE,
    IN_BATTLE,
    QUEUE_COOLDOWN
}
