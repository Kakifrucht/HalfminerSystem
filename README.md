# Halfminer System
Core plugin for Minecraft Server [Two and a half Miner](https://halfminer.de).

Dueling/PvP solution HalfminerBattle can be found [here](https://github.com/Kakifrucht/HalfminerBattle).

Current features
-------
- Modular, lightweight, efficient
- Messages completely, functionality mostly configurable
- **Handlers**
  - BossBar
    - Send bar to specific player or broadcast
    - Set time until bar fades out
    - Broadcast bar and player bar separate, only one at a time of each
  - Storage
    - Autosave
    - Flatfiles in .yml format
      - Own UUID storage/cache
      - Player data storage
      - Storage for other types of data
    - Can easily be queried with YAML API
    - Thread safe
  - Teleport
    - Disallows movement
    - Change default time in config
    - Show bossbar during teleport
    - Stops when player taking damage
    - Execute runnable after successful (or unsuccessful) teleport
    - Only one teleport at a time
  - Titles
    - Main title/subtitle
    - Actionbar title
    - Tablist titles
- **Modules**
  - AntiKillfarming
      - Blocks players, who repeatedly kill each other
    - Dynamic system, to determine block time
  - AntiXray
    - Counts players block breaks
      - Clears after no protected blocks were broken
    - Set protected blocks via config
    - Threshold ratio between broken blocks and broken protected blocks
    - Threshold Y level until counted
    - Notifies staff if threshold was passed
      - Shows last location
      - Notifies on join, if staff was offline
  - AutoMessage
    - Sends messages in a given interval
    - Messages configurable
  - ChatManager
    - Hooks into Vault to get prefix and suffix
    - Custom chatformats
      - Default chatformat is bottom one
      - Permissions can be assigned via custom permission node
      - Permission to always get format with highest priority
      - No format limit
    - Denies chatting if globalmute active
      - Allows easy toggling of globalmute
    - Plays sound on chat
    - Notifies mentioned players via actionbar
      - Rate limit (no mention spam)
    - Disallow (or allow via permission)
      - Using color codes
      - Using formatting codes
      - Posting links/IPs
      - Writing capitalized
  - CombatLog
    - Tags players when hitting/being hit
    - Shows health, name of attacker/victim and level via BossBar
    - Combatlogging causes instant death
    - Shows actionbar message containing time left in fight
    - Untags players after timer runs out, player logs out or a player is killed
    - Halves satiation health regeneration during combat
    - Disables during fight:
      - Switching armor
      - Commands
      - Enderpearls
  - GlitchProtection
    - Notifies staff about potential wall glitching
    - Detects dismount glitches, forces player to spawn
    - Override spigot teleport safety
    - Prevents glitching with chorus fruit, instead teleports down
    - Kills players above netherroof / notifies staff
  - Motd
    - Configurable Serverlist Motd
      - Can be set via command
    - Dynamic playerlimit indicator, configurable with buffers and limits
  - Performance
    - Limits redstone usage
      - Redstone will not work if triggered to often
    - Limits piston usage
      - Only a given amount of pistons can be triggered in a given time
    - Limits hopper placement
      - Checks radius, if too many hoppers denies placement
    - Limits mobspawns
      - Checks radius, if too many mobs stops the mobspawn
  - PvP
    - Strength potions damage nerfed
    - Bow spamming disabled
    - Killstreak via actionbar
    - Sounds on kill/death
    - Remove effects on teleport
  - Respawn
    - Respawns player at custom location
    - Adds a first time join
      - Else, removes join message
      - Execute custom command on first join
  - SignEdit
    - Allows editing of signs
    - Use command /signedit
  - SkillLevel
    - PvP based skilllevel system / ELO
    - Dynamic ELO determination
      - Auto derank on inactivity (when rank threshold is met)
      - Doesn't count farmed kills
    - Adds level to scoreboard
    - Colors name depending on skillgroup
    - Sorts tablist in descending order
  - StaticListeners
    - Removes quit message
    - Disables some deals in villager trades
    - Commandfilter
      - Disables commands in bed (teleport glitch)
      - Disables /pluginname:command for users (to improve commandblocks)
  - Stats
    - Records lots of statistics about a player
      - Online time
      - Last names
      - Kill/death count
      - K/D ratio
      - Blocks placed/broken
      - Mobkills
      - Money earned
    - View stats on rightclicking a player
  - Titles
    - Shows join title
      - Players online / money
      - Configurable message
      - Shows news after delay in bossbar and title
    - Displays information for new players
    - Tab titles containing amount of money and playercount
    - Money through Essentials hook, automatic update
  - Tps
    - Calculates ticks per second
    - Notifies staff when servers Tps is too low
- **Commands**
  - /chat
    - Chat manipulation tools
    - Toggle Globalmute
    - Clear chat
    - Title broadcast
    - Bossbar broadcast
    - Countdown via bossbar
    - Send custom messages to player or broadcast
    - Set news and motd message
  - /hms
    - Reload config (reload)
    - Search for homes in a given radius, hooking into Essentials (searchhomes)
    - Rename items, supports lore (rename)
    - Ring players to get their attention (ring)
    - Edit skillelo of player (updateskill)
    - Remove a players /home block (rmhomeblock)
    - List all currently by antixray watched players (xraybypass)
      - Exempt a player from AntiXRay
  - /hmstore
    - Edit HalfminerSystem storage
    - Set, get and delete variables
    - Check if playerdata is being edited
    - Save to disk
  - /home
    - Executes Essentials /home after unblock from vote
    - Allows usage up to 15 minutes after join
    - Doesn't block for new users (< 300 Minutes)
    - Doesn't block users whose ip has already voted twice
  - /lag
    - Information if player or server lags
    - View other players latency/ping
  - /neutp
    - Teleport to random location
      - Set min/max x/z values
    - Checks for safe teleport location
    - Sets home automatically
    - Gives some information about the server via chat and bossbar
  - /signedit
    - Copy signs, define copy amount
    - Edit signs, define line number
  - /spawn
    - Teleport player to spawn
    - Teleport other players to spawn with permission
    - Teleport offline players to spawn once they login
    - Use /spawn s to set the spawn (only with permission)
  - /stats
    - View own / other players stats
    - Allows to compare statistics easily
  - /verkauf
    - Sell farm items
    - Revenue configurable
    - Possibility to sell multiple inventories at once
    - Multipliers for ranks (via permissions)
  - /vote
    - Shows vote links (custom per player) and current votecount
    - Execute custom command when vote is received (configure Votifier to "/vote voted %PLAYER%")
    - Execute command if certain votecount has been reached (event notifier for instance)
    - If offline or inventory full, stores reward for retrieval later (/vote getreward)
    - Counts votes for /stats
    - Unblocks access to /home
      - Will also unblock other users with same ip
  - /vtapi
    - Small features for VariableTriggers
      - Remove head in casino
      - Remove case in casino
      - Set VariableTriggers vars accordingly