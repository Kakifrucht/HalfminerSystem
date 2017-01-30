# Halfminer Battle
Battle Arena Bukkit plugin with multiple game modes.

Current features
-------
- Battle arena solution allowing easy addition of new arena game modes
- Language completely customizable
- **Gamemodes**
  - Duel mode
    - Endless amount of arenas
    - Custom kits per arena
    - Tested and should be bug free
    - Recovers players completely after fight
    - Robust queue system
      - Kicks player from queue when engaging in PvP outside of arena
    - Duelling per request (/duel playername) or via match (/duel match)
    - When waiting too long for match, will broadcast that a player is waiting
    - Dynamic arena selection system, only shows vacant arenas
    - Consists of a single queue (pipeline), not per arena queue, improving performance
    - Shows current arena status with /duel list
    - Countdown before game start
    - Set maximum game time
    - Disables while fighting:
      - Hunger loss in duel (optional)
      - Item pickup
      - Command usage