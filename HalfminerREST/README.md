# Halfminer REST Server
Bukkit plugin running a REST HTTP server, responding in JSON.

To use SSL it is recommended to setup a reverse proxy pointing to the configured port, such as [nginx](https://nginx.org/en/), while blocking direct access via firewall.

## Current features
- Includes very light HTTP server ([nanohttpd](https://github.com/NanoHttpd/nanohttpd))
- Define port and whitelisted IP's via config
  - Toggle proxy mode to read real IP from header if running behind reverse proxy
- Returns JSON messages
- **REST Commands**
  - ``/player``
    - *GET* /\<uuid|playername>\[/stats|/nodashes]
      - Get a players last known name from UUID or vice versa
      - Adds dashes to UUID's, if not supplied
      - Returns namechanged boolean, ``true`` if supplied username is not current one
      - If ``/stats`` argument supplied via URI, will append all recorded stats about player
      - If ``/nodashes`` argument supplied, will remove dashes from returned UUID
  - ``/status``
    - *GET*
      - Get the current player count
  - ``/storage``
    - Data creation/modification/retrieval, where URI is the path to the given resource
    - *DELETE* /\<path\[/...]>\[content:key&...]
      - Delete a whole section or just the values at the supplied keys
    - *GET* /\<path\[/...]>\[?:key&...]
      - Get the whole section or just the values at the supplied keys
    - *POST/PUT* /\<path\[/...]>\[content:key=value&...&expiry=seconds]
      - Add data to the given path, supplied via content body as ``application/x-www-form-urlencoded``
        - ``POST`` only for creation, not modification, ``PUT`` for both
      - Expiry timestamp can be passed as part of the content body, otherwise default of one hour will be used
        - Timestamp always refers to whole section, even if only a single key was updated, pass 0 for no expiry
