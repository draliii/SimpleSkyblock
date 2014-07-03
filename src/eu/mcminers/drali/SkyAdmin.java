package eu.mcminers.drali;

import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.FilenameException;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.data.DataException;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author dita
 */
public class SkyAdmin implements CommandExecutor {

  private final SimpleSkyblock plugin;

  public SkyAdmin(SimpleSkyblock plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

    //do not execute the command if plugin isn't loaded correctly
    if (!plugin.checkOK) {
      //output to console
      if (!(sender instanceof Player)) {
        plugin.print("admin.loading-failed", false, "severe", plugin.checkReason);
        return true;
      }
      //output to admin player
      if(plugin.checkPerk((Player) sender, "simpleskyblock.admin")){
        sender.sendMessage(plugin.out.format("admin.loading-failed", plugin.checkReason));
        return true;
      }
      //output to all other players
      sender.sendMessage(plugin.out.get("plugin.loading-failed"));
      return true;
    }
    
    if (args.length == 0) {
      sender.sendMessage(plugin.out.get("command.error.unknown"));
      return true;
    }
    else {
      boolean isConsole = !(sender instanceof Player);
      if (isConsole | plugin.checkPerk((Player) sender, "simpleskyblock.admin")) {
        switch (args[0]) {
          case "home":
            if (args.length == 2) {
              if (isConsole) {
                plugin.print("Sorry, this command is not available in the console", false, "info");
                return true;
              }
              try {
                plugin.skyCommand.cmdHomeFriend((Player) sender, args[1]);
              }
              catch (Exception e) {
                sender.sendMessage("Sorry, Exception");
                e.printStackTrace();
              }
            }
            break;
          case "clearcache":
            if (!isConsole) {
              if (!plugin.checkPerk((Player) sender, "simpleskyblock.admin.clearcache")) {
                sender.sendMessage("You don't have permission");
                return true;
              }
            }
            plugin.playerIslands.clear();
            break;
          case "switch":
            String nick1;
            String nick2;
            try {
              nick1 = args[1];
              nick2 = args[2];
            }
            catch(Exception e){
              System.out.println("No nick given!");
              //NO NICKS GIVEN
              return true;
            }
            if(!nick1.matches("[0-9a-zA-Z@_!\\-]{2,14}") || !nick2.matches("[0-9a-zA-Z@_!\\-]{2,14}")){
              //IVALID NICKS
              System.out.println("Invalid nick!");
              return true;
            }
            Island drali = new Island(nick1, plugin);
            Island DJTommek = new Island(nick2, plugin);
            try {
              drali.load();
              DJTommek.load();
            }
            catch (SQLException ex) {
              System.out.println("Islands not loaded!");
              ex.printStackTrace();
            }

            this.switchIslands(drali, DJTommek);
            break;


        }
      }
    }
    return true;
  }

  public boolean switchIslands(Island aIsland, Island bIsland) {
    if (!aIsland.exists || !bIsland.exists) {
      return false;
    }

    Island a;
    Island b;

    //order islands by id
    if (aIsland.id > bIsland.id) {
      b = aIsland;
      a = bIsland;
    }
    else {
      a = aIsland;
      b = bIsland;
    }

    //teleport everybody out
    a.tpVisitors();
    b.tpVisitors();

    /* skys_islands */
    String idChange1 = "UPDATE " + plugin.mysqlPrefix + "_islands SET x='" + b.x + "', z='" + b.z + "',id='" + 0 + "' WHERE nick = '" + a.ownerNick + "' LIMIT 1;";
    String idChange2 = "UPDATE " + plugin.mysqlPrefix + "_islands SET x='" + a.x + "', z='" + a.z + "',id='" + a.id + "' WHERE nick = '" + b.ownerNick + "' LIMIT 1;";
    String idChange3 = "UPDATE " + plugin.mysqlPrefix + "_islands SET x='" + b.x + "', z='" + b.z + "',id='" + b.id + "' WHERE nick = '" + a.ownerNick + "' LIMIT 1;";

    /* skys_members */
    String memberChange1 = "UPDATE " + plugin.mysqlPrefix + "_members SET island_id='0' WHERE island_id = '" + a.id + "' LIMIT 1;";
    String memberChange2 = "UPDATE " + plugin.mysqlPrefix + "_members SET island_id='" + a.id + "' WHERE island_id = '" + b.id + "' LIMIT 1;";
    String memberChange3 = "UPDATE " + plugin.mysqlPrefix + "_members SET island_id='" + b.id + "' WHERE island_id = '0' LIMIT 1;";

    try {
      plugin.database.updateSQL(idChange1);
      plugin.database.updateSQL(idChange2);
      plugin.database.updateSQL(idChange3);
      plugin.database.updateSQL(memberChange1);
      plugin.database.updateSQL(memberChange2);
      plugin.database.updateSQL(memberChange3);
    }
    catch (Exception e) {
      System.out.println("sql queries failed");
      e.printStackTrace();
    }

    try {
      RegionTools rTools = new RegionTools(plugin);

      System.out.println("Saving flags...");

      ProtectedRegion aRegion = rTools.getWorldGuard().getRegionManager(plugin.skyworld).getRegion(a.ownerNick + "island");
      ProtectedRegion bRegion = rTools.getWorldGuard().getRegionManager(plugin.skyworld).getRegion(b.ownerNick + "island");

      Map<Flag<?>, Object> aFlags = aRegion.getFlags();
      Map<Flag<?>, Object> bFlags = bRegion.getFlags();

      System.out.println("Saving perms...");

      rTools.savePerms(bRegion, b);
      rTools.savePerms(aRegion, a);

      System.out.println("Deleting regions...");

      rTools.deleteRegion(a.ownerNick + "island");
      rTools.deleteRegion(b.ownerNick + "island");
      rTools.getWorldGuard().getRegionManager(plugin.skyworld).save();

      System.out.println("Creating region for A");

      ProtectedRegion newARegion = rTools.makeRegion(b.ownerNick, a.x, a.z);
      newARegion.setFlags(bFlags);
      rTools.restorePerms(newARegion, b);
      rTools.getWorldGuard().getRegionManager(plugin.skyworld).save();

      System.out.println("Creating region for B");

      ProtectedRegion newBRegion = rTools.makeRegion(a.ownerNick, b.x, b.z);
      newARegion.setFlags(aFlags);
      rTools.restorePerms(newBRegion, a);
      rTools.getWorldGuard().getRegionManager(plugin.skyworld).save();

    }
    catch (ProtectionDatabaseException ex) {
      System.out.println("Region swithing failed!");
      Logger.getLogger(SkyAdmin.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch (SQLException ex) {
      Logger.getLogger(SkyAdmin.class.getName()).log(Level.SEVERE, null, ex);
    }

    //get WorldEditPlugin and prepare the TerrainManager

    System.out.println("Preparing terrain manipulation");
    WorldEditPlugin wep = (WorldEditPlugin) plugin.getServer().getPluginManager().getPlugin("WorldEdit");
    TerrainManager tm = new TerrainManager(wep, plugin.skyworld);

    //Location aMin = new Location(plugin.skyworld, a.x - 50, 0, a.z - 50);
    //Location aMax = new Location(plugin.skyworld, a.x + 50, 255, a.z + 50);

    //Location bMin = new Location(plugin.skyworld, b.x - 50, 0, b.z - 50);
    //Location bMax = new Location(plugin.skyworld, b.x + 50, 255, b.z + 50);

    //File bSave = new File(plugin.getDataFolder() + File.separator + b.ownerNick + "island");
    //File aSave = new File(plugin.getDataFolder() + File.separator + a.ownerNick + "island");

    File b1 = new File(plugin.getDataFolder() + File.separator + b.ownerNick + "island1");
    File b2 = new File(plugin.getDataFolder() + File.separator + b.ownerNick + "island2");
    File b3 = new File(plugin.getDataFolder() + File.separator + b.ownerNick + "island3");

    Location bA = new Location(plugin.skyworld, b.x - 50, 0, b.z - 50);
    Location bB = new Location(plugin.skyworld, b.x + 50, 85, b.z + 50);
    Location bC = new Location(plugin.skyworld, b.x - 50, 85, b.z - 50);
    Location bD = new Location(plugin.skyworld, b.x + 50, 170, b.z + 50);
    Location bE = new Location(plugin.skyworld, b.x - 50, 170, b.z - 50);
    Location bF = new Location(plugin.skyworld, b.x + 50, 255, b.z + 50);

    File a1 = new File(plugin.getDataFolder() + File.separator + a.ownerNick + "island1");
    File a2 = new File(plugin.getDataFolder() + File.separator + a.ownerNick + "island2");
    File a3 = new File(plugin.getDataFolder() + File.separator + a.ownerNick + "island3");

    Location aA = new Location(plugin.skyworld, a.x - 50, 0, a.z - 50);
    Location aB = new Location(plugin.skyworld, a.x + 50, 85, a.z + 50);
    Location aC = new Location(plugin.skyworld, a.x - 50, 85, a.z - 50);
    Location aD = new Location(plugin.skyworld, a.x + 50, 170, a.z + 50);
    Location aE = new Location(plugin.skyworld, a.x - 50, 170, a.z - 50);
    Location aF = new Location(plugin.skyworld, a.x + 50, 255, a.z + 50);

    System.gc();

    try {

      System.out.println("Saving terrain of B...");
      tm.saveTerrain(b1, bA, bB);
      tm.saveTerrain(b2, bC, bD);
      tm.saveTerrain(b3, bE, bF);

      System.out.println("Saving terrain of A...");
      tm.saveTerrain(a1, aA, aB);
      tm.saveTerrain(a2, aC, aD);
      tm.saveTerrain(a3, aE, aF);
      //tm.saveTerrain(aSave, aMin, aMax);
      //tm.saveTerrain(bSave, bMin, bMax);


      System.out.println("Loading new terrain...");

      System.out.println("Loading b1");
      tm.loadSchematic(b1, aA);
      System.out.println("Loading b2");
      tm.loadSchematic(b2, aC);
      System.out.println("Loading b3");
      tm.loadSchematic(b3, aE);
      System.out.println("Loading a1");
      tm.loadSchematic(a1, bA);
      System.out.println("Loading a2");
      tm.loadSchematic(a2, bC);
      System.out.println("Loading a3");
      tm.loadSchematic(a3, bE);
      //tm.loadSchematic(bSave, aMin);
      //tm.loadSchematic(aSave, bMin);

    }
    catch (FilenameException e) {
      System.out.println("terrain saves failed");
      e.printStackTrace();
      // thrown by WorldEdit - it doesn't like the file name/location etc.
    }
    catch (DataException e) {
      System.out.println("terrain saves failed");
      e.printStackTrace();
      // thrown by WorldEdit - problem with the data
    }
    catch (IOException e) {
      System.out.println("terrain saves failed");
      e.printStackTrace();
      // problem with opening/reading the file
    }
    catch (MaxChangedBlocksException e) {
      System.out.println("terrain saves failed");
      e.printStackTrace();
      // problem with opening/reading the file
    }
    catch (EmptyClipboardException e) {
      System.out.println("terrain saves failed");
      e.printStackTrace();
      // problem with opening/reading the file
    }

    //fix regions


    System.out.println("Done!");
    //clear cache to load all data properly again
    plugin.playerIslands.clear();

    return true;
  }
}
