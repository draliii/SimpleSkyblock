SimpleSkyblock
==============

SimpleSkyblock is a plugin for Minecraft Bukkit servers. It brings the famous Skyblock game to multiplayer servers.

Requirements:
--------------
- Bukkit server (Minecraft version 1.7.2 or higher)
- WorldEdit
- WorldGuard
- Vault
- MySQL database

Features:
--------------

Setup (for servers that weren't using skyblock before):
-----------------------------------------------------------
1. Place the SimpleSkyblock.jar to your ./plugins/ folder.
2. Make sure you have also installed all other plugins SimpleSkyblock needs to run
   properly.
3. Start your server. SimpleSkyblock will generate all its files and also a plugin.yml,
   which can be found in ./plugins/SimpleSkyblock/plugin.yml.
4. In your database, create two tables (skys_islands and skys_members) with the following command:
   ```sql

   SET NAMES utf8;

CREATE TABLE `sbtest_islands` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `nick` varchar(255) NOT NULL,
  `x` int(10) NOT NULL,
  `z` int(10) NOT NULL,
  `date` int(10) NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8;


CREATE TABLE `sbtest_members` (
  `id` int(10) NOT NULL DEFAULT '0',
  `island_id` int(10) NOT NULL,
  `member` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
   ```

   skys_islands

   |Column  |Type                   | 
   | ------- | ---------------------- |
   |id	     |int(10) Auto Increment |
   |nick    |varchar(255)           |
   |x	     |int(10)	             |
   |z	     |int(10)                |
   |date    |int(10)                |
   |active  |tinyint(1)[1]          |


   skys_members
   
   |Column    |Type          |
   |----------|--------------|
   |id	       |int(10)[0]    |
   |island_id |int(10)       |
   |member    |varchar(255)  |

   *You can change the prefix "skys" to whatever you like
5. Open the plugin.yml and fill in the data about your database (including the prefix,
   if you changed it).
6. Create an empty world (you can use McEdit or CleanRoomGenerator). However, it is
   important that there are no blocks at all in the world you are going to use for Skyblock.
   Name the world as you wish, and write the name to the config.yml. Also, you might
   want to use something like Multiverse to make the worlds' management easier.
  
  
  
  
