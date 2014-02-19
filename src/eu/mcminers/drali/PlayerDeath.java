package eu.mcminers.drali;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 *
 * @author dita
 */
public class PlayerDeath implements Listener {

  private final SimpleSkyblock plugin;

  public PlayerDeath(SimpleSkyblock plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void Playerdeath(PlayerDeathEvent e) {
    //TODO Check if he died on skyworld
    if (plugin.hardcore) {
      //if the dead entity is Player
      if (e.getEntity() instanceof Player) {
        //get the Player
        Player player = (Player) e.getEntity();

        player.sendMessage("You have died. Your island will be reseted.");
        plugin.debug(player.getName() + " has died, reseting his island", "info");

        Island island = null;
        try {
          island = plugin.getPlayerData(player);
        }
        catch (SQLException ex) {
          player.sendMessage("SQL Error when reseting your island. Please contact the server administrator.");
        }

        if (island != null) {
          IslandTools iTools = new IslandTools(plugin);

          //reset the area
          iTools.generateIslandBlocks(island.x, island.z, island.ownerNick);

          player.sendMessage("Your island was reseted");

          //teleport player and clear his inventory
          //plugin.skyTp(island.x, island.z, player);
          plugin.clearInventory(player);
        }
      }

    }
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void PlayerRespawn(PlayerRespawnEvent e) {
    //TODO Check if he died on skyworld
    if (plugin.hardcore) {
      //get the Player
      Player player = (Player) e.getPlayer();

      Island island = null;
      try {
        island = plugin.getPlayerData(player);
      }
      catch (SQLException ex) {
        player.sendMessage("SQL Error when reseting your island. Please contact the server administrator.");
      }

      if (island != null) {

        player.sendMessage("Your island was reseted");

        //teleport player and clear his inventory
        Location loc = new Location(plugin.skyworld, island.x, plugin.islandY, island.z);
        e.setRespawnLocation(loc);
        /*try {
          Thread.sleep(1);
        }
        catch (InterruptedException ex) {
          Logger.getLogger(PlayerDeath.class.getName()).log(Level.SEVERE, null, ex);
        }*/
        plugin.skyTp(island.x, island.z, player);
        plugin.clearInventory(player);

      }
    }

  }
}