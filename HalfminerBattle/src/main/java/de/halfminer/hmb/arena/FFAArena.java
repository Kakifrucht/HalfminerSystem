package de.halfminer.hmb.arena;

import de.halfminer.hmb.arena.abs.AbstractKitArena;
import de.halfminer.hmb.enums.GameModeType;
import org.bukkit.entity.Player;

/**
 * Created by fabpw on 08.02.2017.
 */
@SuppressWarnings("unused")
public class FFAArena extends AbstractKitArena {
    public FFAArena(GameModeType gameMode, String name) {
        super(gameMode, name);
    }

    public void addPlayer(Player toAdd) {
        playersInArena.add(toAdd);
        storeClearAndTeleportPlayers(toAdd);
        healAndPreparePlayers(toAdd);
        equipPlayers(toAdd);
    }

    public void removePlayer(Player toRemove) {
        restorePlayers(true, toRemove);
        playersInArena.remove(toRemove);
    }
}
