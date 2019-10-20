# Halfminer Haro

*Ha*lfminer *Ro*leplay (<i>Haro</i>) plugin, that fully converts a given server into a Haro gamemode server.

# Current features

- This plugin is meant to fully convert your server into a Haro server, and thus not really compatible with other Arena plugins, like HalfminerBattle.
- Implements it's own whitelist, only added players can join, or players that have the permission *hmh.admin*.
  - Added players can always join the server, even if the game wasn't started yet, use Vanilla whitelist in conjuction to prevent this if necessary.
- When the game is running, shows the time left and playercount in the tablist.
  - Kicks players after their playtime is over.
  - Displays countdown to warn players before their time is running out.
  - Kicks a player after being eliminated from the game.
- Custom health mode, where players don't get eliminated after dying and instead lose health.
  - Elimination only happens in PvP.
  - Max/min health configurable.
  - Configurable health gain for PvP killer.
  - Configurable health loss on non PvP death.
  - Custom health will be reset when players leave the server.
- **Commands**
  - */haro <add|remove> \<Player>*
    - Add or remove a player from the game.
    - Added players can join the server before the game was started.
    - Players can be removed during the game, as a disqualification measure.
    - Players that haven't joined the server before can also be added.
  - */haro addtime <-day|-all|Player> [time]*
    - Add play time to either all players or a specific player.
    - When using */haro addtime -day*, all players will get the time in seconds specified in config.
      - Set this command to execute whenever a new day is starting via some external task scheduler.
    - To remove time from a player, use a negative value.
    - Players who are online will receive a notification that their playtime was changed.
    - Will take into account the maximum time a player can accumulate.
  - */haro end*
    - End the currently running game, by resetting it's state and clearing all added players.
    - Will print a warning, if more than one player is still in the game.
  - */haro setspawn*
    - Will set the spawn point to the location the executing player is currently at.
    - Spawn point will be used for respawns, and as the starting point when the game starts.
    - Access the spawn point by using */harospawn* (or */spawn*, if not overloaded).
  - */haro start*
    - Start a game, at least two players must be added to the game.
    - Will run custom commands defined in the config file once when the game starts.
    - Will run custom commands to initialize all online players, players who are not online during the start will be initialized after their first join.
      - Configurable max health on game start.
    - Will teleport all players to the specified spawn point, if their distance to it is higher than specified in the config file.
    - Will add the starting time (specified in config file) to all players as their remaining time left.
  - */haro status*
    - Prints current game information (is the game running/over?).
    - Shows all added players, their online/elimination status, and their remaining time, if not yet eliminated.