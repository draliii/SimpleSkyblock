package eu.mcminers.drali;

import java.sql.SQLException;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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
    if (plugin.hardcore) {
      //if the dead entity is Player
      if (e.getEntity() instanceof Player) {
        //get the Player
        Player player = (Player) e.getEntity();
        //System.out.println("onDeath");

        if (player.getWorld() == plugin.skyworld) {

          plugin.write(null, "debug.player-death", "info", true, player.getName());
          plugin.write(player, "plugin.ondeath.starting", "info", false);

          Island island = new Island(player.getName(), plugin);
          try {
            island.load();
          }
          catch (SQLException ex) {
            plugin.write(null, "admin.sql.ex-load", "severe", player.getName());
          }
          if (island.exists) {

            try {
              island.reset();
            }
            catch (Exception ex) {
              plugin.write(null, "admin.sql.ex-reset", "severe", player.getName());
            }

            plugin.write(player, "plugin.ondeath.finished", "info");
          }
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void PlayerRespawn(PlayerRespawnEvent e) {
    if (plugin.hardcore) {
      //get the Player
      Player player = (Player) e.getPlayer();

      //System.out.println("onRespawn");
      if (player.getWorld() == plugin.skyworld) {
        Island island = new Island(player.getName(), plugin);
        try {
          island.load();
        }
        catch (SQLException ex) {
          plugin.write(null, "admin.sql.ex-load", "severe", player.getName());
        }

        if (!island.exists) {
          //tp to spawn
          Location loc = new Location(plugin.skyworld, 0, plugin.getISLANDS_Y(), 0);
          e.setRespawnLocation(loc);
        }
        else {
          Location loc = new Location(plugin.skyworld, island.x, plugin.getISLANDS_Y(), island.z);
          e.setRespawnLocation(loc);
        }
      }
    }
  }
  
  
  @EventHandler(priority = EventPriority.MONITOR)
  public void PlayerQuit(PlayerQuitEvent e){
    //delete player data from island hashmap
    plugin.playerIslands.remove(e.getPlayer().getName());
  }
}