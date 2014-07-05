SimpleSkyblock
==============

SimpleSkyblock is a plugin for Minecraft Bukkit servers. It brings the famous Skyblock game to multiplayer servers. It is based on a similar plugin by deQGel, but it has some new features and uses less memory.

Requirements:
--------------
- Bukkit server (Minecraft version 1.7.2 or higher)
- WorldEdit
- WorldGuard
- Vault
- MySQL database

Features:
--------------
 - one island for each player
 - islands protected by region
 - islands are resetable, in case player wants to start over
 - unused islands can be replaced with new ones to save space
 - teleport to own island (or friend's)
 - more players on one island
 - admins can switch islands' positions to let two friend play next to each other
 - optional *hardcore* mode -> islands are reset after player death
 - highly customizable by `config.yml` file

How does it work?
-----------------
The islands are generated in a spiral around the center area, which stays empty and serves as a spawn area. By default, each island is sorrounded by a region 100 * 100 * 255 blocks. After a 20 block free gap, there begins another region. Each region is named after his owner (player Gigla would have a *giglaisland*). Each player can have just on island, but he can add other players to join him, as well as he can play on other islands, too. 
 
Setup (for servers that weren't using skyblock before):
-----------------------------------------------------------
1. Place the `SimpleSkyblock.jar` to your `./plugins/` folder.
2. Make sure you have also installed all other plugins SimpleSkyblock needs to run
   properly.
3. Start your server. SimpleSkyblock will generate all its files and also a `config.yml`,
   which can be found in `./plugins/SimpleSkyblock/config.yml`.
4. In your database, create two tables (`skys_islands` and `skys_members`) with the following command:
   ```sql

   SET NAMES utf8;
   CREATE TABLE `skys_islands` (
     `id` int(10) NOT NULL AUTO_INCREMENT,
     `nick` varchar(255) NOT NULL,
     `x` int(10) NOT NULL,
     `z` int(10) NOT NULL,
     `date` int(10) NOT NULL,
     `active` tinyint(1) NOT NULL DEFAULT '1',
     PRIMARY KEY (`id`)
   ) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
   CREATE TABLE `skys_members` (
     `id` int(10) NOT NULL AUTO_INCREMENT,
     `island_id` int(10) NOT NULL,
     `member` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL
   ) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
   ```
   *You can change the prefix "skys" to whatever you like*
5. Open the `config.yml` and fill in the data about your database (including the prefix,
   if you changed it).
6. Create an empty world (you can use McEdit or CleanRoomGenerator). However, it is
   important that there are no blocks at all in the world you are going to use for Skyblock.
   Name the world as you wish, and write the name to the `config.yml`. Also, you might
   want to use some plugin like Multiverse to make the worlds' management easier.
7. Before any islands are created, you can change their size, height and sapces between them.
   This cannot be done later, because it would cause existing islands to collapse.

Servers that want to switch to SimpleSkyblock from deQGel's SkySMP plugin:
-------------------------------------------------------------------------------
The SimpleSkyblock plugin was built to replace the SkySMP plugin, so it is partly compatible with it. Default island size, island spacing, region properties and island look are same. However, since SimpleSkyblock uses database to contain island data instead of `.bin` files, it can't read anything about existing islands from the old plugin.

I have created a simple terminal application that can read these files. The output of this app, a SQL INSERT query, will  enter all data about the islands you had to the database, making them accessible to SimpleSkyblcok plugin. However, it is recommended to check the output first.

You might face one of these bugs:
 - There are duplicite nicks in the island list, such as *gigla* and *GigLa* (note that SimpleSkyblock is case insensitive, and this might cause trouble).
 - SimpleSkyblock uses regular expressions to protect the database. Some of the old island names might not be safe.

To get rid of the unwanted lines, replace them with a random nick and set their `active` to `false`. Also carefully delete their regions (`./plugins/WorldGuard/worlds/SkyworldName/regions.yml`), so the place on the map is free for a new island.


Player commands:
------------
 - `/sb new` creates a new island for the player
 - `/sb reset` resets player's island, deletes invenory
 - `/sb home [nick]` teleports to island center (bedrock). Use this to teleport to your friends by typing their nick.
 - `/sb info` outputs information about island
 - `/sb delete` sets the island to inactive, when player decides to quit playing. New islands can be generated on the inactive ones. Note: inventory is deleted, region too.
 - `/sb active` re-activates an island, if one changes his mind. Can be used only if the island still exists.
 - `/sb spawn` teleports to the world's spawn location [0,0] by default
 - `/sb friend add <nick>` adds the player to your island (he can teleport there and he is a member of the region)
 - `/sb friend remove <nick>` removes the player from your island
 - `/sb friend list` list of your friends and also the islands, who added you as their friend
 - `/sb friend clear` removes all friends you have added


Admin commands:
--------------
 - `/sb home <nick>` teleports you to the specified island, even if you are not added as friend there.
 - `/sbadmin clearcache` clears the plugin cache. This causes all islands to be loaded from database again.
 - `/sbadmin getpos` returns X and Z of the island, that will be generated next on the edge of the spiral
 - `/sbadmin switch <nick1> <nick2>` switches locations of the two islands.
Permissions:
------------------
