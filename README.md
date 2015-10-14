# Halfminer System
Core plugin for Minecraft Server Two and a half Miner  
Website: https://halfminer.de

Current features
-------
- Modular, lightweight, efficient
- **Complete dueling solution HalfminerDuel** // information below
- Statistics for every player with /stats
- View latency and server status with /lag
  - Information if player or server laggs
  - View other players latency/ping
  - Notifies staff when Tps is too low
- Inbuilt protections against:
  - Killfarming
  - CombatLogging
  - Bedrock glitching (alert and log)
  - Lag caused by Redstone/Pistons/Hoppers
- Automessager
- Custom Motd with colors and settable via command
- Edit and copy signs /signedit
- Rename items /hms rename
- Functionality and messages configurable
- Small extras:
  - Removes default join/leave/death messages
  - Capslock filter
  - Disables hitting self with bow
  - Blocks usage of /pluginname:command
  - Small features for VariableTriggers /vtapi

HalfminerDuel
-------
- Lightweight and efficient
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
- Language completely customizable