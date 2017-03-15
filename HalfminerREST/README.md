# Halfminer REST Server
Bukkit plugin containing a REST HTTP server, responding in JSON.

To use SSL it is recommended to setup a reverse proxy pointing to the API, such as [nginx](https://www.nginx.com/).

## Current features
- Includes very light HTTP server ([nanohttpd](https://github.com/NanoHttpd/nanohttpd))
- Define port and whitelisted IP's via config
- Returns JSON messages
- **REST Commands**
  - /status
    - *GET*
      - Get current player count
  - /storage
    - Data creation/modification/retrieval, where URI is the path to the given resource
    - *DELETE* /\<path[/...]>[content:key&...]
      - Delete a whole section or just the values at the supplied keys
    - *GET* /\<path[/...]|player/\<uuid>>[?:key&...]
      - Get the whole section or just the values at the supplied keys
      - Get all recorded stats of player with subcommand /player
    - *POST/PUT* /\<path[/...]>[content:key=value&...&expiry=seconds]
      - Add data to the given path, supplied via content body as *application/x-www-form-urlencoded*
        - POST only for creation, not modification, PUT for both
      - Expiry timestamp can be passed as part of the content body, otherwise default of one hour will be used
        - Timestamp always refers to whole section, even if only a single key was updated, pass 0 for no expiry
  - /uuid
    - *GET* /\<uuid|playername>
      - Get a players last known name from UUID or vice versa
      - Adds dashes to UUID's, if not supplied
      - Returns namechanged boolean, true if supplied username is not current one
    