# Halfminer Repositories
Modification repository for Minecraft Server [Two and a half Miner](https://halfminer.de).

Halfminer Teamspeak Bot can be found [here](https://github.com/Kakifrucht/HalfminerBot).

## Current Modules
- **HalfminerSystem** - Base plugin API containing shared functionality and a centralized storage system.
  - **HalfminerBattle** - Battle arena Bukkit/Spigot plugin implementing various arena based game modes.
  - **HalfminerCore** - Core Bukkit/Spigot plugin containing most server side functionality.
  - **HalfminerLand** - Land protection and management plugin.
  - **HalfminerREST** - Bukkit plugin running a lightweight REST HTTP server responding in JSON.

:warning: All modules are specifically tailored for our needs over at *halfminer.de*, and thus many features 
may not be toggleable/configurable. The default localization is German and some localized strings are configured 
specifically for our server. Feel free to fork and fix/translate the plugin to your likings. 
It is presently fully localizable via the plugins configuration file.