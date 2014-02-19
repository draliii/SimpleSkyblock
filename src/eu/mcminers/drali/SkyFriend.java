package eu.mcminers.drali;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author dita
 */
public class SkyFriend implements CommandExecutor {

  private final SimpleSkyblock plugin;

  public SkyFriend(SimpleSkyblock plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    //add, remove, list
    if (!(sender instanceof Player)) {
      plugin.getLogger().info("Only players can use this command");
      return true;
    }

    Player player = (Player) sender;

    if (args.length == 0) {
      plugin.getLogger().info("No parameters given");
      player.sendMessage("No parameters given");
      return true;
    }

    Island island;
    try {
      island = plugin.getPlayerData(player);
    }
    catch (SQLException ex) {
      Logger.getLogger(SkyFriend.class.getName()).log(Level.SEVERE, null, ex);
      return false;
    }

    if (island == null) {
      player.sendMessage("You don't have an island. To create one, use /sb new");
      return true;
    }

    switch (args[0]) {
      case "add":
        if (args.length == 1) {
          player.sendMessage("No nick found! To add a friend to your island, use /sbfriend add <nick>.");
        }
        else if (args[1].equalsIgnoreCase(player.getName())) {
          player.sendMessage("You can't add yourself to your island!");
        }
        else {
          player.sendMessage("Adding " + args[1] + " as your friend!");
          //"SELECT nick FROM skys_members WHERE island_id = '" + island.id + "' AND member = '" + args[1] + "';";
          //add him as region member (nothing happens if he is added already)
          RegionTools rTools = new RegionTools(plugin);
          ProtectedRegion region = rTools.getWorldGuard().getRegionManager(plugin.skyworld).getRegion(player.getName() + "Island");
          DefaultDomain members = region.getMembers();
          members.addPlayer(args[1]);
          region.setMembers(members);

          //check if the player isn't added already
          String checkMember = "SELECT member FROM " + plugin.getMysqlPrefix() + "_members WHERE island_id = '" + island.id + "' AND member = '" + args[1] + "';";
          ResultSet rs = plugin.database.querySQL(checkMember);
          try {
            if (rs.next()) {
              player.sendMessage(args[1] + " is your friend already!");
              return true;
            }
          }
          catch (SQLException ex) {
            Logger.getLogger(SkyFriend.class.getName()).log(Level.SEVERE, null, ex);
          }
          //add him to the database
          player.sendMessage("Database:");
          String addMember = "INSERT INTO " + plugin.getMysqlPrefix() + "_members (`island_id`, `member`) VALUES ('" + island.id + "', '" + args[1] + "');";
          //remove him from the database
          int queriesCount = plugin.database.updateSQL(addMember);
          player.sendMessage("QueriesCount:" + queriesCount);
        }
        break;
      case "remove":
        if (args.length == 1) {
          player.sendMessage("No nick found! To remove a player from your friend list, use /sbfriend remove <nick>.");
        }
        else if (args[1].equalsIgnoreCase(player.getName())) {
          player.sendMessage("You can't remove yourself from your island!");
        }
        else {
          player.sendMessage("Removing " + args[1] + " from your friends!");

          //remove him form region members (nothing happens if he isnt't there)
          player.sendMessage("Region:");
          RegionTools rTools = new RegionTools(plugin);
          ProtectedRegion region = rTools.getWorldGuard().getRegionManager(plugin.skyworld).getRegion(player.getName() + "Island");
          DefaultDomain members = region.getMembers();
          members.removePlayer(args[1]);
          region.setMembers(members);
          //check if the player is added already
          //remove him from the database (nothing happens if he isn't there)
          player.sendMessage("Database:");
          String deleteMember = "DELETE FROM " + plugin.getMysqlPrefix() + "_members WHERE island_id = '" + island.id + "' AND member = '" + args[1] + "';";
          int queriesCount = plugin.database.updateSQL(deleteMember);
          player.sendMessage("QueriesCount:" + queriesCount);

        }
        break;
      case "list":
        if (!(island == null)) {
          player.sendMessage("Following players have permission on you island:");
          String membersList = "SELECT member FROM " + plugin.getMysqlPrefix() + "_members WHERE island_id = '" + island.id + "';";
          ResultSet result = plugin.database.querySQL(membersList);
          try {
            while (result.next()) {
              player.sendMessage(result.getString("member"));
            }
          }
          catch (SQLException ex) {
            Logger.getLogger(SkyFriend.class.getName()).log(Level.SEVERE, null, ex);
          }

        }
        player.sendMessage("You have permissions on the following islands:");
        String visitableIslands = "SELECT islands.id, islands.nick, islands.x, islands.z, members.id, members.island_id, members.member "
                + "FROM " + plugin.getMysqlPrefix() + "_islands islands "
                + "LEFT JOIN " + plugin.getMysqlPrefix() + "_members members "
                + "ON islands.id = members.island_id "
                + "WHERE members.member = '" + player.getName() + "';";
        ResultSet rs = plugin.database.querySQL(visitableIslands);
        /*
         */
        try {
          while (rs.next()) {
            player.sendMessage(rs.getString("nick"));
          }
        }
        catch (SQLException ex) {
          Logger.getLogger(SkyFriend.class.getName()).log(Level.SEVERE, null, ex);
        }

        break;
      default:
        player.sendMessage("");
    }
    return false;
  }
}
