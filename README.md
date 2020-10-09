# Halfminer 1.12.2 Repositories 
Modification repository for Minecraft Server [Two and a half Miner](https://halfminer.de).
All modules are currently compatible with version ``1.12.2`` of Spigot/Paper.

All changes on this branch will also be pushed to the main branch at some point.

Halfminer TeamSpeak Bot can be found [here](https://github.com/Kakifrucht/HalfminerBot).

## Current Modules
- **HalfminerSystem** - Base plugin API containing shared functionality and a centralized storage system.
  - **HalfminerBattle** - Battle arena Bukkit/Spigot plugin implementing various arena based game modes.
  - **HalfminerCore** - Core Bukkit/Spigot plugin containing Minecraft server modifications for various aspects of the game.
  - **HalfminerLand** - Land protection and management plugin.
  - **HalfminerREST** - Bukkit plugin running a lightweight REST HTTP server responding in JSON.

:warning: All modules are specifically tailored for our needs over at *halfminer.de*, and thus many features 
may not be toggleable/configurable. The default localization is German and some localized strings are configured 
specifically for our server. Feel free to fork and fix/translate the plugin to your likings. 
It is presently fully localizable via the plugins configuration file.

## Build
Due to *net.minecraft.server* ('NMS') dependencies, you need to run 
Spigot's [BuildTools.jar](https://www.spigotmc.org/wiki/buildtools) once to add CraftBukkit to your 
local Maven repository. This dependency is isolated to one class, *NMSUtils*, and only used in the HalfminerSystem module.
