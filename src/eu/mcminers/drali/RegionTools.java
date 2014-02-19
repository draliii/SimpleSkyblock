package eu.mcminers.drali;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 *
 * @author dita
 */
public class RegionTools {

  private SimpleSkyblock plugin;

  public RegionTools(SimpleSkyblock plugin) {
    this.plugin = plugin;
  }

  public ProtectedRegion makeRegion(String name, int x, int z) {
    ProtectedRegion region = null;
    region = new ProtectedCuboidRegion(name, getProtectionVectorLeft(x, z), getProtectionVectorRight(x, z));
    try {
      region.setParent(this.getWorldGuard().getRegionManager(plugin.skyworld).getRegion("__Global__"));
      this.getWorldGuard().getRegionManager(plugin.skyworld).save();
    }
    catch (ProtectedRegion.CircularInheritanceException ex) {
      Logger.getLogger(RegionTools.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch (ProtectionDatabaseException ex) {
      Logger.getLogger(RegionTools.class.getName()).log(Level.SEVERE, null, ex);
    }
    return region;
  }

  public void createRegion(int x, int z, Player player)
          throws ProtectedRegion.CircularInheritanceException, InvalidFlagFormat, SQLException, ProtectionDatabaseException {

    //work with RegionManager (code is nicer that way)
    RegionManager regionManager = this.getWorldGuard().getRegionManager(plugin.skyworld);

    //he should not have a WG region yet
    if (!regionManager.hasRegion(player.getName() + "island")) {
      //player.sendMessage("Creating region!");

      //make the regionplayer.getName() + "Island"
      ProtectedRegion region = makeRegion(player.getName() + "island", x, z);

      //assign an owner to it
      DefaultDomain owners = new DefaultDomain();
      owners.addPlayer(player.getName());
      region.setOwners(owners);

      //configure flags
      //TODO: add this to config later
      region.setFlag(DefaultFlag.GREET_MESSAGE,
              DefaultFlag.GREET_MESSAGE.parseInput(this.getWorldGuard(),
              player,
              plugin.out.format("plugin.region.enter", player.getName())));
      region.setFlag(DefaultFlag.FAREWELL_MESSAGE,
              DefaultFlag.FAREWELL_MESSAGE.parseInput(this.getWorldGuard(),
              player,
              plugin.out.format("plugin.region.leave", player.getName())));
      region.setFlag(DefaultFlag.PVP,
              DefaultFlag.PVP.parseInput(this.getWorldGuard(),
              player,
              "deny"));

      //add the region into the RegionManager
      regionManager.addRegion(region);

      //save the RegionManager (otherwise regions are deleted after server restart)
      plugin.print("plugin.region.saving", true, "info");
      regionManager.save();

    }
    //if there is already an island with player's name
    //this should never happen
    else {
      player.sendMessage("Your island is already protected!");
    }
  }

  private BlockVector getProtectionVectorLeft(int x, int z) {
    return new BlockVector(x + (plugin.islandSize / 2), 255, z + (plugin.islandSize / 2));
  }

  private BlockVector getProtectionVectorRight(int x, int z) {
    return new BlockVector(x - (plugin.islandSize / 2), 0, z - (plugin.islandSize / 2));
  }

  public WorldGuardPlugin getWorldGuard() {
    Plugin wg = this.plugin.getServer().getPluginManager().getPlugin("WorldGuard");
    // WorldGuard may not be loaded
    if (wg == null || !(wg instanceof WorldGuardPlugin)) {
      return null; // Maybe you want throw an exception instead
    }
    return (WorldGuardPlugin) wg;
  }

  public void deleteRegion(String id) throws ProtectionDatabaseException {
    RegionManager regionManager = this.getWorldGuard().getRegionManager(plugin.skyworld);
    plugin.print("plugin.region.deleting", true, "info", id);
    regionManager.removeRegion(id);
    plugin.print("plugin.region.saving", true, "info");
    regionManager.save();
  }

  /*
  @Override
  protected void finalize() throws Throwable {
    //just an experiment :)
    try {
      plugin.debug("RegionTools was garbage-collected", "info");
    }
    finally {
      super.finalize();
    }
  }
  */
}
