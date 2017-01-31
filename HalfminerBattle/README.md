# Halfminer Battle
Battle Arena Bukkit plugin with multiple game modes.

Current features
-------
- Full battle arena solution allowing easy addition of new arena game modes
- Seamless integration into existing Survival PvP worlds
- Localization configurable
- **Gamemodes**
  - Global functionality
    - Endless amount of arenas
    - Custom kits per arena, if gamemode supports kits
    - Recovers players completely after fight (position, health/status, inventory)
    - Enables fights even if damage was cancelled, for example due to fighting a clan member
    - Prevents teleporting into arena via tp delay glitches if not fighting
    - Disables while fighting:
      - Hunger loss in duel (optional)
      - Item dropping/pickup
      - Command usage
  - Duel mode
    - Robust queue system
      - Not using a per arena queue, if all arenas are in use the next pair will be added automatically
      - Kicks player from queue 
        - on disconnect
        - when engaging in PvP outside of arena
        - when entering a bed, since else teleportation is not possible
    - Duelling per request (/duel playername) or via auto match (/duel match)
      - When waiting too long for match, will broadcast that a player is waiting
    - Dynamic arena selection system, only shows vacant arenas
    - Shows current arena status with /duel list
    - Countdown before game start
    - Set maximum game time
    