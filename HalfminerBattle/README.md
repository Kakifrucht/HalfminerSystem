# Halfminer Battle
Battle Arena Bukkit/Spigot plugin with multiple game modes.

Current features
-------
- Full battle arena solution allowing easy addition of new arena game modes
- Seamless integration into existing Survival PvP worlds
- Optimized for performance, battle tested and generally robust
- Localization configurable, commands are clickable
- **Gamemodes**
  - Global functionality
    - Endless amount of arenas
    - Custom kits per arena, if gamemode wants to support kits
      - Adds lore to kits to easily identify ones that were possibly extracted from a badly secured arena
      - Can toggle inventory store to disk, to prevent any inventory data loss from ever ocurring
        - Command */hmb openinventory* allows retrieval of said items by file name
        - Automatically cleaning up old files
    - Recovers players completely after fight (position, health/status, inventory if kit was used, potion effects)
      - Sets gamemode to adventure during fight
    - Allows hitting even if damage was cancelled, for example due to fighting a clan member
    - Prevents teleporting into arenas via tp delay glitches (Essentials */tpa*) if not fighting
    - Disables while fighting:
      - Hunger loss in duel (optional)
      - Item dropping/pickup
      - Command usage
  - Duel mode
    - Robust queue system
      - Not using a per arena queue, if all arenas are in use, the next duel pair will be added automatically once it becomes free
      - Kicks player from queue 
        - on disconnect
        - when engaging in PvP outside of arena
        - when entering a bed, since else teleportation is not possible
      - Cooldown after leaving queue
    - Duelling per request (*/duel playername*) or via auto match (*/duel match*)
      - When waiting too long for match, will broadcast that a player is waiting, configurable
      - Will start duel if a player duel requests a player that is waiting for a match
    - Dynamic arena selection system, only shows vacant arenas
      - Randomly selects map choosing player
    - If player logs out while in battle, kills player and ensures that opponent gets the kill
    - Shows current arena status with */duel list*
    - Countdown before game start
    - Set maximum game time in config
    