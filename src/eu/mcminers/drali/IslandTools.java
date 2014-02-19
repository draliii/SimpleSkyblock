package eu.mcminers.drali;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

/**
 *
 * @author dita
 */
public class IslandTools {

  private final SimpleSkyblock plugin;

  public IslandTools(SimpleSkyblock plugin) {
    this.plugin = plugin;
  }

  /**
   * Checks for a place to generate an island (write data about island into
   * database, delete data about previous owner).
   *
   * @param player the new owner of the island
   * @throws SQLException
   */
  public boolean createIslandSQL(Player player) throws SQLException {
    //get an abandoned island (closest to [0, 0])
    int[] emptyIsland = plugin.getEmptyIsland();

    //if the id is 0 (which can never happen in the table), it means that no empty islands (or no islands at all) were found
    if (emptyIsland[0] == 0) {
      //get x, z of the island with highest ID (the latest generated, on the edge of the grid)
      int[] lastIsland = plugin.getLastIsland();
      //look for coordinates of the next island to be generated
      Island next = this.nextIslandLocation(lastIsland[0], lastIsland[1]);
      //insert data about the island into database
      String insert = "INSERT INTO " + plugin.getMysqlPrefix() + "_islands (`x`, `z`, `nick`, `date`)"
              + "VALUES (" + next.x + ", " + next.z + ", '" + player.getName() + "', '" + System.currentTimeMillis() / 1000 + "');";
      plugin.database.updateSQL(insert);
    } //if an abandoned island was found
    else {
      //delete all players that could tphome to that island before
      String deleteOldMembers = "DELETE FROM " + plugin.getMysqlPrefix() + "_members"
              + " WHERE island_id = " + emptyIsland[0] + ";";
      int rowsDeleted = plugin.database.updateSQL(deleteOldMembers);
      plugin.debug("rows deleted: " + rowsDeleted, "info");

      //get the previous owner's nick and delete the WG region
      String previousPlayerSql = "SELECT `nick` FROM " + plugin.getMysqlPrefix() + "_islands WHERE id = '" + emptyIsland[0] + "';";
      ResultSet rs = plugin.database.querySQL(previousPlayerSql);
      if (rs.next()) {//if the query found something
        //delete the region
        String previousPlayer = rs.getString("nick");
        RegionTools rTools = new RegionTools(plugin);
        rTools.getWorldGuard().getRegionManager(plugin.skyworld).removeRegion(previousPlayer + "Island");
        plugin.debug("Deleting region of " + previousPlayer, "info");
      }
      else {
        plugin.debug("ERROR, inactive nick wasn't found", "severe");
      }

      //clear previous data (owner, date) and replace them with new data
      String updateOldData = "UPDATE " + plugin.getMysqlPrefix() + "_islands "
              + " SET nick = '" + player.getName() + "', date = '" + System.currentTimeMillis() / 1000 + "', active = 1 "
              + " WHERE id = " + emptyIsland[0]
              + " LIMIT 1;";
      int rowsUpdated = plugin.database.updateSQL(updateOldData);
      plugin.debug("rows updated: " + rowsUpdated, "info");
    }
    plugin.debug("SQL queries done", "info");
    return true;
  }

  /**
   * Returns the position of the next island - generates island locations in a
   * spiral.
   */
  private Island nextIslandLocation(int x, int z) {
    // Gets the next position of an Island based on the last one.
    // Generates new Islands in a spiral.

    Island nextPos = new Island(x, z, plugin);
    if (x < z) {
      if (((-1) * x) < z) {
        nextPos.x = nextPos.x + plugin.getISLAND_SPACING();
        return nextPos;
      }
      nextPos.z = nextPos.z + plugin.getISLAND_SPACING();
      return nextPos;
    }
    if (x > z) {
      if (((-1) * x) >= z) {
        nextPos.x = nextPos.x - plugin.getISLAND_SPACING();
        return nextPos;
      }
      nextPos.z = nextPos.z - plugin.getISLAND_SPACING();
      return nextPos;
    }
    if (x <= 0) {
      nextPos.z = nextPos.z + plugin.getISLAND_SPACING();
      return nextPos;
    }
    nextPos.z = nextPos.z - plugin.getISLAND_SPACING();
    return nextPos;
  }

  public void generateIslandBlocks(int x, int z, String nick) {
    
    deleteIslandBlocks(x, z); //clean the space before making the island there
     
    int y = plugin.getISLANDS_Y(); //blub
    World world = plugin.skyworld;


    for (int x_operate = x; x_operate < x + 3; x_operate++) {
      for (int y_operate = y; y_operate < y + 3; y_operate++) {
        for (int z_operate = z; z_operate < z + 6; z_operate++) {
          Block blockToChange = world.getBlockAt(x_operate, y_operate, z_operate);
          blockToChange.setType(Material.GRASS);    // chest area
        }
      }
    }

    for (int x_operate = x + 3; x_operate < x + 6; x_operate++) {
      for (int y_operate = y; y_operate < y + 3; y_operate++) {
        for (int z_operate = z + 3; z_operate < z + 6; z_operate++) {
          Block blockToChange = world.getBlockAt(x_operate, y_operate, z_operate);
          blockToChange.setType(Material.GRASS);    // 3x3 corner
        }
      }
    }
    
    Location treeLoc = new Location(plugin.skyworld, x+4, y+3, z+4);
    plugin.skyworld.generateTree(treeLoc, TreeType.TREE);

    /*
    //tree
    for (int x_operate = x + 3; x_operate < x + 7; x_operate++) {
      for (int y_operate = y + 7; y_operate < y + 10; y_operate++) {
        for (int z_operate = z + 3; z_operate < z + 7; z_operate++) {
          Block blockToChange = world.getBlockAt(x_operate, y_operate, z_operate);
          blockToChange.setType(Material.LEAVES);
        }
      }
    }


    
    for (int y_operate = y + 3; y_operate < y + 9; y_operate++) {
      Block blockToChange = world.getBlockAt(x + 5, y_operate, z + 5);
      blockToChange.setType(Material.LOG);
    }*/


    // chest
    Block blockToChange = world.getBlockAt(x + 1, y + 3, z + 1);
    blockToChange.setType(Material.CHEST);
    Chest chest = (Chest) blockToChange.getState();
    Inventory inventory = chest.getInventory();
    inventory.clear();
    ItemStack item;
    item = new ItemStack(Material.STRING, 12); //String
    inventory.addItem(item);
    item = new ItemStack(Material.LAVA_BUCKET, 1); //Bucket lava
    inventory.addItem(item);
    item = new ItemStack(Material.BONE, 1); //Bone
    inventory.addItem(item);
    item = new ItemStack(Material.SUGAR_CANE, 1); //Sugar Cane
    inventory.addItem(item);
    item = new ItemStack(Material.RED_MUSHROOM, 1); //Mushroom red
    inventory.addItem(item);
    item = new ItemStack(Material.ICE, 2); //Ice
    inventory.addItem(item);
    item = new ItemStack(Material.PUMPKIN_SEEDS, 1); //pumpkin seeds
    inventory.addItem(item);
    item = new ItemStack(Material.BROWN_MUSHROOM, 1); //mushroom brown
    inventory.addItem(item);
    item = new ItemStack(Material.MELON, 1); //melon slice
    inventory.addItem(item);
    item = new ItemStack(Material.CACTUS, 1); //cactus
    inventory.addItem(item);
    item = new ItemStack(Material.SIGN, 1); //sign
    inventory.addItem(item);

    item = new ItemStack(Material.PAPER, 1); //paper with a custom name
    ItemMeta m = item.getItemMeta();
    m.setDisplayName(plugin.out.format("welcome", nick)); //the paper name
    item.setItemMeta(m);
    inventory.addItem(item);

    //bedrock
    blockToChange = world.getBlockAt(x, y, z);
    blockToChange.setType(Material.BEDROCK);

    //sand
    blockToChange = world.getBlockAt(x + 2, y + 1, z + 1);
    blockToChange.setType(Material.SAND);
    blockToChange = world.getBlockAt(x + 2, y + 1, z + 2);
    blockToChange.setType(Material.SAND);
    blockToChange = world.getBlockAt(x + 2, y + 1, z + 3);
    blockToChange.setType(Material.SAND);

  }

  public void deleteIslandBlocks(int centerX, int centerZ) {
    // Create a edit session with unlimited blocks
    /*
    EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(c.getWorld(), -1);
    try {
      editSession.setBlocks(c, new BaseBlock(BlockID.AIR));
    }
    catch (MaxChangedBlocksException e) {
      // As of the blocks are unlimited this should not be called
    }*/
    for (int x = centerX - (plugin.islandSize / 2); x < centerX + (plugin.islandSize / 2); x++) {
      for (int y = 0; y < plugin.skyworld.getMaxHeight(); y++) {
        for (int z = centerZ - (plugin.islandSize / 2); z < centerZ + (plugin.islandSize / 2); z++) {
          Block block = plugin.skyworld.getBlockAt(x, y, z);
          if (block.getType() != Material.AIR) {
            //This commented line causes a lot of lag if executed
            //plugin.debug("x: " + x + ", y: " + y + ", z: " + z, "info");
            block.setType(Material.AIR);
          }
        }
      }
    }
  }

  public void deactivateIsland(String playerName) throws SQLException {
    String updateInactiveSql = "UPDATE " + plugin.getMysqlPrefix() + "_islands SET `active` = 0 WHERE `nick` = '" + playerName + "';";
    plugin.database.updateSQL(updateInactiveSql);
  }

  public void activateIsland(String playerName) throws SQLException {
    String updateInactiveSql = "UPDATE " + plugin.getMysqlPrefix() + "_islands SET `active` = 1 WHERE `nick` = '" + playerName + "';";
    plugin.database.updateSQL(updateInactiveSql);
  }

  public WorldEditPlugin getWorldEdit() {
    Plugin plugin = this.plugin.getServer().getPluginManager().getPlugin("WorldEdit");
    // WorldEdit may not be loaded
    if (plugin == null || !(plugin instanceof WorldEditPlugin)) {
      return null;
    }
    return (WorldEditPlugin) plugin;
  }
}