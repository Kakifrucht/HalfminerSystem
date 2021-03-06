# Halfminer System
Base Spigot plugin API for Minecraft Server [Two and a half Miner](https://halfminer.de).

Contains handlers, caches and utilities that are shared between all Halfminer Bukkit/Spigot plugins in this repository.

## Current features
- Central ``HalfminerManager`` serves as hub for persistence across plugins
  - Manages configuration file updates
  - (Un-)registers classes from Bukkit event manager implementing ``Listener`` interface
  - Registered classes may implement additional interfaces …
    - ``Disableable`` - to be called when the parent plugin shuts down
    - ``Reloadable`` - to be called when the plugins config was reloaded
    - ``Sweepable`` - to be called in a set interval for cleanup
- **Caches**
  - ``ActionProbabilityContainer``
    - Class reading a list containing key:value pairings, where key is the probability as a number relative to other list elements and value is the action name
      - Example: List containing ``2:action1`` and ``1:action2`` has 66,6% chance to run action1, 33,3% to run action2
    - Returns a random CustomAction on demand
  - ``CustomAction``
    - Reads customactions.txt in plugin directory and executes defined actions
      - Uses ``CustomtextCache`` syntax, where chapter is action name and content is a list of actions
    - Supports placeholders for every action
      - Custom placeholders can be passed before execution of action
      - ``%PLAYER%`` and ``%PLAYER1-N%`` always active
    - Currently supported actions
      - ``players |number|``: define a minimum amount of players that must be passed to this action to execute
      - ``hasroom |number| [stacksize]``: execution flow will only continue if given amount of slots are free, optional stacksize parameter that will divide the given number
      - ``cmd |command|``: execute command as console
      - ``give |customitem| [amount]``: hands out customitem defined in customitems.txt
      - ``broadcast |message|``: broadcast a given message
      - ``tell |message|``: send a given message to main player
    - Stops execution flow if previous action fails 
      - Example: If ``broadcast`` action is below ``give`` action and item could not be given, there will be no broadcast
  - ``CustomitemCache``
    - Uses ``CustomtextCache`` syntax
    - Chaptername is itemname
    - First line in chapter must be Minecraft Material name
    - Supports ``%PLAYER%`` placeholder (player name that receives item) and custom ones can be passed aswell
    - Available item customisations
      - ``name``: itemname, supporting ``&`` as color code character
      - ``lore``: custom item lore, lines are separated with ``|`` character, supports color
      - ``damage``: durability, number as argument
      - ``enchant``: custom enchants, separate enchantment name and level with ``:``, can include multiple per line
      - ``skullowner``: set the owner of the skull, only works if given material is ``SKULL_ITEM``
    - Will throw ``ItemCacheException`` if item wasn't given to player
  - ``CustomtextCache``
    - Flatfile based text cache, segmented in chapters
    - To mark a chapter, use ``#chaptername argument`` (argument optional and not limited to one, case insensitive)
       - Supports aliases via ``,``, like ``#chaptername argument,alias argument``
       - Supports wildcards via ``*``, like ``#chaptername argument *`` or ``#chaptername *``
       - Supports inlined aliases via ``|``, like ``#chapter subchapter|subchapteralias``
    - Automatic replacement of ``&`` with Minecraft color code
    - If line ends with space char, add next line to current line
- **Handlers**
  - BossBar
    - Send bossbar to specific player or broadcast
    - Set time until bar fades out
    - Broadcast bar and player bar separate, only one bar at a time for each
  - Hooks
    - Hooks external soft-dependant plugins
    - Checks if plugins are loaded
    - Shortcuts to external API
  - Menu
    - Opens inventory based menus, classes must implement ``MenuCreator`` interface to create them
      - Get all currently opened menus that were created by a given ``MenuCreator``
    - Prevents entering/removing items from menu
    - Classes can optionally pass a ``MenuClickHandler`` to handle inventory interaction
    - Automatically adds pagination, previous/next page buttons will be added if necessary
      - Pagination constants can be configured, such as items per page
  - Storage
    - Data stored in YAML flatfiles
      - Player data storage
        - Collects default information about every player
          - Online time
          - Last login time
          - Current and past usernames (does broadcast if name was changed)
      - UUID<>Username storage/cache
        - Database is being built automatically when a player logs in
      - Storage for other types of data
      - Changes are being autosaved
    - Can easily be queried via Bukkit YAML API
      - Get ``HalfminerPlayer`` object to grab stored user data
      - Thread safe
    - Holds ``CustomtextCache``'s
  - Teleport
    - Disallows movement before teleport
      - Displays a countdown bossbar before teleport
      - Change default time in config
    - Stop teleport when player is taking damage
    - Execute runnable after successful (or unsuccessful) teleport
    - Only one teleport at a time per player
  - Titles
    - Send main title/subtitle
      - Send title with delay, to prioritize titles
    - Send actionbar title
    - Send tablist header/footer
- **Utils**
  - Message
    - Used for messaging, broadcasting and logging of messages
    - Supports custom placeholders
    - Supports clickable commands via ``~`` prefix and encapsulation of command with ``/``
    - Supports colors via ``&`` code
  - ReflectUtils
    - Collection of static methods accessing NMS (``net.minecraft.server``) and CraftBukkit classes via Reflection
    - These might break with updates to Minecraft, since accessing them isn't supported by Spigot API
    - Set the last killer of a player, get players ping and send actionbar packets
  - StringArgumentSeparator
    - Helper class splitting a given string and allowing easy access of arguments
    - Allows to easily retrieve numbers from the given String
  - Utils
    - Collection of miscellaneous static methods
    - Object to String converters, String filters, macros etc.
