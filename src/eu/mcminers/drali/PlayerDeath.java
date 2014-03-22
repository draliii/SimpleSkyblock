package eu.mcminers.drali;

import java.sql.SQLException;
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

  /**
   * Resets the player's island after his death (if hardcore is enabled).
   *
   * @param e
   */
  @EventHandler(priority = EventPriority.NORMAL)
  public void Playerdeath(PlayerDeathEvent e) {
    //TODO Check if he died on skyworld
    if (plugin.hardcore) {
      //if the dead entity is Player
      if (e.getEntity() instanceof Player) {
        //get the Player
        Player player = (Player) e.getEntity();
        System.out.println("onDeath");

        if (player.getWorld() == plugin.skyworld) {

          player.sendMessage("You have died. Your island will be reseted.");
          plugin.debug(player.getName() + " has died, reseting his island", "info");


          Island island = new Island(player.getName(), plugin);
          try {
            island.load();
          }
          catch (SQLException ex) {
            //CATCH
          }
          if (island.exists) {

            island.reset();

            player.sendMessage("Your island was reseted");

            /*
             //teleport player and clear his inventory
             island.tpHome(player);
             plugin.clearInventory(player);*/
          }
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

      System.out.println("onRespawn");
      Island island = new Island(player.getName(), plugin);
      try {
        island.load();
      }
      catch (SQLException ex) {
        //CATCH
      }

      if (!island.exists) {
        //tp to spawn
        Location loc = new Location(plugin.skyworld, 0, plugin.islandY, 0);
        e.setRespawnLocation(loc);
      }
      else {
        Location loc = new Location(plugin.skyworld, island.x, plugin.islandY, island.z);
        e.setRespawnLocation(loc);
      }
    }

  }
}