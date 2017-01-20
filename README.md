# Halfminer System
Core plugin for Minecraft Server [Two and a half Miner](https://halfminer.de). Runs as Spigot plugin powered by Bukkit, depends on [Essentials](https://github.com/drtshock/Factions).

Dueling/PvP solution HalfminerBattle can be found [here](https://github.com/Kakifrucht/HalfminerBattle).

## Current features
- Modular, lightweight, efficient
- Messages completely, functionality mostly configurable
  - Supports clickable commands by using '~' character and encapsulating command in '/'
- **Handlers**
  - BossBar
    - Send bar to specific player or broadcast
    - Set time until bar fades out
    - Broadcast bar and player bar separate, only one bar at a time for each
  - Hooks
    - Hooks external plugins
    - Checks if plugins are loaded
    - Shortcuts to external api
  - Storage
    - Autosave
    - Flatfiles in .yml format
      - Own UUID storage/cache
      - Player data storage
      - Storage for other types of data
    - Can easily be queried with YAML API
    - Caches customtext files
      - To mark a chapter, use "#chaptername argument" (argument optional and not limited, case insensitive)
        - Supports aliases via comma such as "#chaptername argument,alias argument"
        - Supports wildcards, such as "#chaptername argument *" or "#chaptername *"
        - Supports aliases in between via '|' char, such as #chapter subchapter|subchapteralias
      - Automatic replacement of '&' with Bukkit color code
      - If line ends with space char, add next line to current line
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
    - Counts amount of kills between two players
    - After set amount of kills has been reached, blocks players for a set amount of time
      - Checks interval between kills, resets if interval exceeds given amount
      - Broadcasts block to all players
        - Pre warns players one kill before they get blocked
        - Also prints informational message that killfarming is not allowed
      - Blocks further PvP (both denies for blocked players and prevent other players from hitting aswell)
        - Direct hitting
        - TnT killing
        - Arrow shooting
        - Splash/Lingering potion throwing
      - Blocks commands
      - Prints message with remaining block time
    - Allows other modules to check if a kill was farmed
    - Punishment doubles for every additional block
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
      - Commands can be made clickable (start with '~' and encapsulate command with trailing '/')
  - ChatManager
    - Hooks into Vault to get prefix and suffix
    - Custom chatformats
      - Default chatformat is bottom one
      - Format can be assigned via custom permission node
      - Permission to always get format with highest priority
      - No format limit
    - Denies chatting if globalmute active
      - Allows easy toggling of globalmute
    - Plays sound on chat
    - Notifies mentioned players via actionbar
      - Shows if mentioned player is afk
      - Rate limit (no mention spam)
    - Disallow (or allow via permission)
      - Sending empty/one character messages
      - Repeating the same (or similar) message
      - Using color codes
      - Using formatting codes
      - Posting links/IPs
      - Writing capitalized
  - CombatLog
    - Tags players when hitting/being hit
    - Shows actionbar message containing time left in fight
    - On logout
      - Combat logging player dies
      - Last attacker will get the kill and get untagged
      - Message will be broadcast, containing last attacker
    - Untags players after timer runs out, player logs out or a player is killed
    - Disables during fight:
      - Taking off armor
      - Commands
      - Enderpearls
  - CommandPersistence
    - Stores and calls registered persistent commands to be executed when a given event is fired for a player
  - GlitchProtection
    - Notifies staff about potential wall glitching
    - Detects dismount glitches, forces player to spawn
    - Override Spigot teleport safety between worlds
    - Prevents glitching with chorus fruit, instead teleports down
    - Kills players above netherroof / notifies staff
  - HealthBar
    - Shows healthbar of attacking/attacked player/entity in bossbar
      - Contains playername / mobname
      - If player, shows players skilllevel
      - Updates on damage or health regain for every player who hit the entity
      - Dynamic bar segmentation, solid if none available client side
    - Only shows one bar at a time
      - Shows bar and updates for 8 seconds max, or until other entity was hit
      - When entity was killed, shows bar in green and only for 2 seconds
  - Motd
    - Configurable Serverlist Motd
      - Can be set via command
    - Dynamic playerlimit indicator, configurable with buffers and limits
  - Performance
    - Limits redstone usage
      - Redstone will not work if triggered to often
    - Limits piston usage
      - Only a given amount of pistons can be extended in a given time
    - Limits hopper placement
      - Checks radius, if too many hoppers denies placement
    - Limits mobspawns
      - Checks radius, if too many mobs stops the mobspawn
  - PvP
    - Halves PvP cooldown
      - Reduces damage immunity
    - Strength potions damage nerfed, configurable
    - Bow spamming disabled
    - Disable hitting self with bow
    - Killstreak via actionbar
    - Run custom actions with custom probabilities on kill
      - See customactions.txt for example actions
    - Sounds on kill/death
    - Remove effects on teleport
    - Halves satiation health regeneration during combat
    - Remove regeneration potion effect when eating golden apple
      - Ensures that absorption does not fully regenerate when eating a non Notch golden apple
    - Broadcast resurrect via Totem of Undying
  - Respawn
    - Respawns player at custom location
    - Adds a first time join
      - Else, removes join message
      - Execute custom command on first join
    - Adds random chance to get own head dropped if new players are being welcomed
      - Custom welcome words
      - Cooldown to prevent misuse
      - Custom probability
      - Uses (sub)title and bossbar for information and plays sounds
  - Sell
    - Auto sells chests on inventory close
      - Needs to be toggled
    - Sell items that are sellable
      - Custom multiplier per permission
      - Price settable via config
    - Can be accessed via command
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
    - Keeps itemname of colored items in anvil
    - Commandfilter
      - Disables commands in bed (teleport glitch)
      - Rewrites /pluginname:command to just /command
    - Disables tab completes that are too long, defaults to help instead
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
      - Exempt permission
      - Show if player is AFK
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
    - Notifies staff when servers ticks TPS is too low
- **Commands**
  - /chat
    - Chat manipulation tools
    - Toggle globalmute
    - Clear chat
    - Send clickable command message to all players
    - Title broadcast
    - Bossbar broadcast
    - Countdown via bossbar
    - Send custom messages to player or broadcast
    - Set news and motd message
  - /customtext
    - Displays custom text data
    - Should be binded via Bukkit aliases (commands.yml in server root)
    - Utilizes and extends CustomtextCache with syntax elements
      - Placeholder support for text
      - Make commands clickable by ending them with '/' character
        - A line must be started with '~' to be parsed
        - Commands will be printed in italic
      - Support for command execution
        - Lines starting with "~>" will make the player execute following text
        - Lines starting with "~~>" will make the console execute following text as command
  - /disposal
    - Opens portable disposal
  - /hms
    - Copy a WorldEdit schematic to another directory (copyschematic)
    - Give a custom item defined in customitems.txt to a player (give)
    - Reload config (reload)
    - Rename items, supports lore (rename)
    - Ring players to get their attention (ring)
    - Remove a players /home block (rmhomeblock)
    - Run an action defined in customactions.txt (runaction)
    - Search for homes in a given radius, hooking into Essentials (searchhomes)
    - Edit skillelo of player (updateskill)
    - List all currently by antixray watched players (xraybypass)
      - Exempt a player from AntiXRay
  - /hmsapi
    - Small features for script integration
      - Show titles
      - Check if player has room in inv
      - Remove head in casino
      - Remove case in casino
      - Set script vars accordingly
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
  - /newtp
    - Teleport to random location
      - Set min/max x/z values
    - Checks for safe teleport location
    - Sets home automatically
    - Gives some information about the server via chat and bossbar
  - /repair
    - Repair the held item or whole inventory
      - Permissions for access restriction
    - Adds configurable variable cooldown per level
      - Will only apply cooldown if item was actually repaired
    - If repairing single item, checks if it is a stack (permission required)
  - /sell
    - Sell sellable items via command
    - Uses ModSell for the actual sale
    - Possibility to sell multiple inventories at once
  - /rank
    - Give out ranks to players
    - Executes a custom action to give out rewards and run custom commands
      - If player is offline waits until he is back online to execute action
      - If action fails executes custom fallback action
      - Add custom parameters that will be multiplied by a custom amount per rank (see config)
        - Possibility to deduct reward multipliers for previous ranks
    - Prevents giving out same or lower rank
    - Instead of defining upgrade rank on command execution can define number of ranks that player will be upranked
  - /signedit
    - Copy signs, define copy amount
    - Edit signs, define line number
  - /spawn
    - Teleport player to spawn
    - Teleport other players to spawn with permission
    - Teleport offline players to spawn once they login
    - Set the spawnpoint (Command /spawn s, only with permission)
  - /stats
    - View own / other players stats
    - Allows to compare statistics easily
  - /vote
    - Shows vote links (custom per player) and current votecount
    - Execute custom actions when vote is received (configure Votifier to "/vote voted %PLAYER%")
    - Execute command if certain votecount has been reached (event notifier for instance)
    - If offline or inventory full, stores reward for retrieval later (/vote getreward)
    - Counts votes for /stats
    - Unblocks access to /home
      - Will also unblock other users with same ip
