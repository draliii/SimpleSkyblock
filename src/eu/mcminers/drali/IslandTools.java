package eu.mcminers.drali;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
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
   * Returns the position of the next island - generates island locations in a
   * spiral.
   */
  public Coordinates nextIslandLocation(int x, int z) {
    // Gets the next position of an Island based on the last one.
    // Generates new Islands in a spiral.

    Coordinates crds = new Coordinates(x, z);
    if (x < z) {
      if (((-1) * x) < z) {
        crds.x = crds.x + plugin.getISLAND_SPACING();
        return crds;
      }
      crds.z = crds.z + plugin.getISLAND_SPACING();
      return crds;
    }
    if (x > z) {
      if (((-1) * x) >= z) {
        crds.x = crds.x - plugin.getISLAND_SPACING();
        return crds;
      }
      crds.z = crds.z - plugin.getISLAND_SPACING();
      return crds;
    }
    if (x <= 0) {
      crds.z = crds.z + plugin.getISLAND_SPACING();
      return crds;
    }
    crds.z = crds.z - plugin.getISLAND_SPACING();
    return crds;
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
    
    /*
    Location treeLoc = new Location(plugin.skyworld, x+4, y+3, z+4);
    plugin.skyworld.generateTree(treeLoc, TreeType.TREE);*/

    
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
      Block blockToChange = world.getBlockAt(x + 4, y_operate, z + 4);
      blockToChange.setType(Material.LOG);
    }


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
    blockToChange = world.getBlockAt(x + 2, y + 1, z + 4);
    blockToChange.setType(Material.SAND);
    blockToChange = world.getBlockAt(x + 1, y + 1, z + 4);
    blockToChange.setType(Material.SAND);
    blockToChange = world.getBlockAt(x + 1, y + 1, z + 3);
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

  public WorldEditPlugin getWorldEdit() {
    Plugin WEplugin = this.plugin.getServer().getPluginManager().getPlugin("WorldEdit");
    // WorldEdit may not be loaded
    if (WEplugin == null || !(WEplugin instanceof WorldEditPlugin)) {
      return null;
    }
    return (WorldEditPlugin) WEplugin;
  }
}