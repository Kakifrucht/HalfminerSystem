# Halfminer System
Core plugin for Minecraft Server [Two and a half Miner](https://halfminer.de).

Dueling/PvP solution HalfminerBattle can be found [here](/Kakifrucht/HalfminerBattle).

Current features
-------
- Modular, lightweight, efficient
- Messages completely, functionality mostly configurable
- Essentials hook
  - For economy support
  - To read players homes
- **Handlers**
  - BossBar
    - Set time until bar fades out
    - Only show one bar at a time
  - Storage
    - Autosave
    - Flatfile in .yml format
    - Can easily be queried with YAML API
    - Thread safe
  - Teleport
    - Disallows movement
    - Change default time in config
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
  - AutoMessage
    - Sends messages in a given interval
    - Messages configurable
  - CombatLog
    - Tags players when hitting/being hit
    - Shows health and name of attacker/victim via BossBar
    - Combatlogging causes instant death
    - Shows titles containing time left in fight
    - Untags players after timer runs out, player logs out or a player is killed
    - Disables during fight:
      - Switching armor
      - Commands
      - Enderpearls
  - GlitchProtection
    - Notifies staff about potential bedrock/obsidian glitching
    - Override spigot teleport safety
    - Prevents glitching with chorus fruit, instead teleports down
    - Kills players above netherroof / notifies staff
  - Motd
    - Configurable Serverlist Motd
      - Can be set via command
    - Dynamic playerlimit indicator, configurable with buffers and limits
  - Performance
    - Limits redstone usage (configurable)
      - Redstone will not work if triggered to often
    - Limits piston usage
      - Only a given amount of pistons can be triggered in a given time
    - Limits hopper placement (configurable)
      - Checks radius, if too many hoppers denies placement
    - Limits mobspawns (configurable)
      - Checks radius, if too many mobs stops the mobspawn
  - PvP
    - Strength potions damage nerfed
    - Nerfs bowspamming
      - Delay between each shot
    - Shows kill/death streaks via titles
    - Adds sounds to kills/deaths
    - Remove some effects on teleport
  - Respawn
    - Respawns player at custom location
    - Adds a first time join message or removes completely
    - Execute custom command on first join
  - SignEdit
    - Allows editing of signs
    - Use command /signedit
  - SkillLevel
    - PvP based skilllevel system / ELO
    - Dynamic ELO determination
    - Adds level to scoreboard
    - Colors name depending on skillgroup
  - StaticListeners
    - Removes quit message
    - Disables some deals in villager trades
    - Chatfilter
      - Checks for globalmute (set via command /c globalmute)
      - Plays sound on chat
      - Filters capslock
    - Commandfilter
      - Disables commands in bed (teleport glitch)
      - Disables /pluginname:command for users (to improve commandblocks)
  - Stats
    - Records lots of statistics about a player
      - Blocks broken / placed
      - Kills / Deaths / KD Ratio
      - Time Online
      - Mobs killed
      - Money earned
    - Rightclick player for quick overview
  - Titles
    - Shows join title
      - Players online / mones
      - Configurable message
      - Shows news after delay
    - Displays information for new players
    - Tab titles containing amount of money and playercount
  - Tps
    - Calculates ticks per second
    - Notifies staff when servers Tps is too low
- **Commands**
  - /chat
    - Chat manipulation tools
    - Toggle Globalmute
    - Clear chat
    - Title broadcast
    - Send custom messages to player or broadcast
    - Set news and motd message
    - Show a countdown
  - /hms
    - Reload config (reload)
    - Search for homes in a given radius (searchhomes)
    - Rename items, supports lore (rename)
    - Ring players to get their attention (ring)
    - Edit skillelo of player (updateskill)
    - Remove a players /home block (rmhomeblock)
  - /hmstore
    - Edit HalfminerSystem storage
    - Set and delete vars
    - Save to disk
  - /home
    - Executes Essentials /home after unblock from vote
    - Allows usage up to 15 minutes after join
    - Doesn't block for new users (< 300 Minutes)
    - Doesn't block users ip has already voted twice
  - /lag
    - Information if player or server lags
    - View other players latency/ping
  - /neutp
    - Teleport to random location
      - Set min/max x/z values
    - Checks for safe teleport location
    - Sets home automatically
  - /signedit
    - Copy signs, define copy amount
    - Edit signs, define line number
  - /spawn
    - Teleport player to spawn
    - Teleport other players to spawn with permission
    - Use /spawn s to set the spawn with permission
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