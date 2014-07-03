package eu.mcminers.drali;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author dita
 */
public class Sky2 implements CommandExecutor {

  private final SimpleSkyblock plugin;

  public Sky2(SimpleSkyblock plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    //new, reset, delete, home, home nick

    //do not execute the command if plugin isn't loaded correctly
    if (!plugin.checkOK) {
      //output to console
      if (!(sender instanceof Player)) {
        plugin.write(null, "admin.loading-failed", "severe", plugin.checkReason);
        return true;
      }
      //output to admin player
      if(plugin.checkPerk((Player) sender, "simpleskyblock.admin")){
        plugin.write(sender, "admin.loading-failed", "severe", plugin.checkReason);
        return true;
      }
      //output to all other players
      plugin.write(sender, "plugin.loading-failed", "severe");
      return true;
    }

    //deny access to non-player senders
    if (!(sender instanceof Player)) {
      plugin.write(null, "plugin.noconsole", "info");
      return true;
    }

    Player player = (Player) sender;

    if (args.length == 0) {
      plugin.write(player, "command.error.unknown", "info");
      return true;
    }
    else if (args[0].equalsIgnoreCase("help")) {
      plugin.displayHelp(player);
      return true;
    }
    else {
      try {

        switch (args[0]) {
          //the HOME command
          case "home":
            //if there are no parameters
            if (args.length == 1) {
              cmdHomeSelf(player);
            }
            //if there are more parameters (which means a nick)
            else {
              cmdHomeFriend(player, args[1]);
            }
            break;

          //the INFO command
          case "info":
            cmdInfo(player);
            break;

          //the NEW command
          case "new":
            cmdNew(player);
            break;

          //the RESET command
          case "reset":
            cmdReset(player);
            break;

          //the DELETE command
          case "delete":
            cmdDelete(player);
            break;
          //the ACTIVE command
          case "active":
            cmdActive(player);
            break;

          case "spawn":
            cmdSpawn(player);
            break;

          case "friend":

            //load parameter friend (for easier manipulation)
            String friend;
            if (args.length >= 3) {
              friend = args[2];
            }
            else {
              friend = null;
            }
            if (args.length == 1) {
              plugin.write(player, "command.error.unknown", "info");
              return true;
            }
            switch (args[1]) {
              case "add":
                cmdFriendAdd(player, friend);
                break;
              case "remove":
                cmdFriendRemove(player, friend);
                break;
              case "clear":
                cmdFriendClear(player);
                break;
              case "list":
                cmdFriendList(player);
                break;
              default:
                plugin.write(player, "command.error.unknown", "info");
                break;
            }
            break;

          default:
            plugin.write(player, "command.error.unknown", "info");
            break;
        }
        //after the switch

        return true;
      }
      catch (SQLException | ProtectedRegion.CircularInheritanceException | InvalidFlagFormat | ProtectionDatabaseException ex) {
        plugin.write(player, "command.error.exception", "info");
        ex.printStackTrace();
      }
    }
    return false;
  }

  public void cmdHomeSelf(Player player) throws SQLException {

    //check permission
    if (plugin.checkPerk(player, "simpleskyblock.sb.home.self")) {
      //load island data
      Island island = new Island(player.getName(), plugin);
      try {
        island.load();
      }
      catch (SQLException e) {
        plugin.write(player, "command.error.sqlexception", "info");
        e.printStackTrace();
        return;
      }
      //if the database query found nothing about the player
      if (!island.exists) {
        plugin.write(player, "command.tphome.noisland", "info");
      }
      //if his island is inactive
      else if (!island.active) {
        plugin.write(player, "command.tphome.inactive", "info");
      }
      else {
        plugin.write(player, "command.tphome.teleporting", "info");
        island.tpHome(player);
      }
    }
    //if player doesn't have permission
    else {
      plugin.write(player, "command.tphome.perms", "info");
    }
  }

  public void cmdHomeFriend(Player player, String visited) throws SQLException {
    //teleport to his own island, if he wrote his own nick
    if (player.getName().equalsIgnoreCase(visited)) {
      cmdHomeSelf(player);
    }
    //check permission
    else if (plugin.checkPerk(player, "simpleskyblock.sb.home.other")) {
      if (!visited.matches("[0-9a-zA-Z@_!\\-]{2,14}")) {
        plugin.write(player, "command.tpfriend.invalidnick", "info");
      }
      else {
        //load island data
        Island island = new Island(visited, plugin);
        try {
          island.load();
        }
        catch (SQLException e) {
          plugin.write(player, "command.error.sqlexception", "info");
          e.printStackTrace();
          return;
        }

        //no results found (the visited nick doesn't exist)
        if (!island.exists) {
          plugin.write(player, "command.tpfriend.noisland", "info");
          return;
        }

        boolean op = plugin.checkPerk(player, "simpleskyblock.admin.tpall");

        if (island.isFriend(player.getName()) | op) {

          //if the island is inactive
          if (!island.active) {
            plugin.write(player, "command.tpfriend.inactive", "info");
            if (!op) {
              return;
            }
            plugin.write(player, "admin.tp.inactive", "info");
          }

          plugin.write(player, "command.tpfriend.teleporting", "info", visited);
          if (!island.isFriend(player.getName())) {
            plugin.write(player, "admin.tp.nonfriend", "info");
          }
          island.tpHome(player);
        }
        else {
          plugin.write(player, "command.tpfriend.denied", "info", visited);
        }


      }
    }
    //if he doesn't have permission
    else {
      plugin.write(player, "command.tpfriend.perms", "info");
    }
  }

  public void cmdInfo(Player player) throws SQLException {

    //check permission
    if (plugin.checkPerk(player, "simpleskyblock.sb.info")) {

      //load island data
      Island island = new Island(player.getName(), plugin);
      try {
        island.load();
      }
      catch (SQLException e) {
        plugin.write(player, "command.error.sqlexception", "info");
        e.printStackTrace();
        return;
      }
      //if no island is found
      if (!island.exists) {
        plugin.write(player, "command.info.noisland", "info");
      }
      //if something is found
      else {
        player.sendMessage(island.toString());
      }
    }
    else {
      plugin.write(player, "command.info.perms", "info");
    }
  }

  public void cmdNew(Player player) throws SQLException,
                                           ProtectedRegion.CircularInheritanceException,
                                           InvalidFlagFormat,
                                           ProtectionDatabaseException {

    if (plugin.checkPerk(player, "simpleskyblock.sb.new")) {

      //load island data
      Island island = new Island(player.getName(), plugin);
      try {
        island.load();
      }
      catch (SQLException e) {
        plugin.write(player, "command.error.sqlexception", "info");
        e.printStackTrace();
        return;
      }
      IslandTools iTools = new IslandTools(plugin);
      RegionTools rTools = new RegionTools(plugin);

      //check if he has an island
      if (island.exists) {
        plugin.write(player, "command.new.exists", "info");
      }
      //if he doesn't have one yet
      else {
        plugin.write(player, "command.new.starting", "info");

        island.create(); //find coordinates, write into database
        //player.sendMessage("Your island is now in the database");

        iTools.generateIslandBlocks(island.x, island.z, island.ownerNick); //clear the area and generate new blocks (both is in the method)
        //player.sendMessage("Your island was now spawned");

        rTools.createRegion(island.x, island.z, player); //create the region and protect it
        //player.sendMessage("Your island was protected by a region");

        island.deleteItems();
        island.tpHome(player);
        plugin.clearInventory(player);
        island.save();
        plugin.write(player, "command.new.finished", "info");
      }
    }
    else {
      plugin.write(player, "command.new.perms", "info");
    }
  }

  public void cmdReset(Player player) {
    if (plugin.checkPerk(player, "simpleskyblock.sb.reset")) {

      //load island data
      Island island = new Island(player.getName(), plugin);
      try {
        island.load();
      }
      catch (SQLException e) {
        plugin.write(player, "command.error.sqlexception", "info");
        e.printStackTrace();
        return;
      }

      if (!island.exists) {
        plugin.write(player, "command.reset.noisland", "info");
      }
      else if (!island.active) {
        plugin.write(player, "command.reset.inactive", "info");
      }
      else {
        plugin.write(player, "command.reset.starting", "info");

        try {
          island.reset();
        }
        catch (SQLException e) {
          plugin.write(player, "command.error.sqlexception", "info");
          e.printStackTrace();
          return;
        }

        plugin.write(player, "command.reset.finished", "info");

        island.tpHome(player);
        island.save();
      }
    }
    else {
      plugin.write(player, "command.reset.perms", "info");
    }
  }

  public void cmdDelete(Player player) throws SQLException,
                                              ProtectionDatabaseException {
    if (plugin.checkPerk(player, "simpleskyblock.sb.delete")) {

      //load island data
      Island island = new Island(player.getName(), plugin);
      try {
        island.load();
      }
      catch (SQLException e) {
        plugin.write(player, "command.error.sqlexception", "info");
        e.printStackTrace();
        return;
      }

      RegionTools rTools = new RegionTools(plugin);
      if (!island.exists) {
        plugin.write(player, "command.delete.noisland", "info");
      }
      else if (!island.active) {
        plugin.write(player, "command.delete.inactive", "info");
      }
      else {
        plugin.write(player, "command.delete.starting", "info");

        island.deactivate();

        //remove the region
        rTools.deleteRegion(player.getName() + "island");

        plugin.write(player, "command.delete.finished", "info");
        island.save();
      }
    }
    else {
      plugin.write(player, "command.delete.perms", "info");
    }
  }

  public void cmdActive(Player player) throws SQLException,
                                              ProtectedRegion.CircularInheritanceException,
                                              InvalidFlagFormat,
                                              ProtectionDatabaseException {

    if (plugin.checkPerk(player, "simpleskyblock.sb.active")) {

      //load island data
      Island island = new Island(player.getName(), plugin);
      try {
        island.load();
      }
      catch (SQLException e) {
        plugin.write(player, "command.error.sqlexception", "info");
        e.printStackTrace();
        return;
      }

      RegionTools rTools = new RegionTools(plugin);
      if (!island.exists) {
        plugin.write(player, "command.active.noisland", "info");
      }
      else if (island.active) {
        plugin.write(player, "command.active.active", "info");
      }
      else {
        //set it to active in the database
        island.activate();

        //make a new region and restore the previous player settings
        rTools.restorePerms(rTools.createRegion(island.x, island.z, player), island);

        //teleport to home
        island.tpHome(player);

        plugin.write(player, "command.active.finished", "info");
        island.save();
      }

    }
    else {
      plugin.write(player, "command.active.perms", "info");
    }
  }

  public void cmdSpawn(Player player) {
    plugin.skyTp(0, 0, player);
  }

  public void cmdFriendAdd(Player player, String friend) throws
          SQLException {
    if (plugin.checkPerk(player, "simpleskyblock.sb.friend.add")) {
      if (friend == null) {
        plugin.write(player, "command.addfriend.nonick", "info");
      }
      else if (!(friend.matches("[0-9a-zA-Z@_!\\-]{2,14}"))) {
        plugin.write(player, "command.addfriend.invalidnick", "info");
      }
      else if (friend.equalsIgnoreCase(player.getName())) {
        plugin.write(player, "command.addfriend.self", "info");
      }
      else {

        //load island data
        Island island = new Island(player.getName(), plugin);
        try {
          island.load();
        }
        catch (SQLException e) {
          plugin.write(player, "command.error.sqlexception", "info");
          e.printStackTrace();
          return;
        }

        if (island.exists) {
          RegionTools rTools = new RegionTools(plugin);

          //add him as region member (nothing happens if he is added already)
          ProtectedRegion region = rTools.getWorldGuard().getRegionManager(plugin.skyworld).getRegion(player.getName() + "island");
          DefaultDomain members = region.getMembers();
          members.addPlayer(friend);
          region.setMembers(members);

          if (island.isFriend(friend)) {
            plugin.write(player, "command.addfriend.alreadyadded", "info", friend);
          }
          //add him to the database
          else {
            plugin.write(player, "command.addfriend.starting", "info", friend);
            island.addFriend(friend);

            //inform the player that he was added
            for (Player p : plugin.getServer().getOnlinePlayers()) {
              //check nick
              if (p.getName().equalsIgnoreCase(friend)) {
                plugin.write(player, "command.addfriend.friend", "info", player.getName());
              }
            }

            island.save();
          }
        }
        else {
          plugin.write(player, "command.addfriend.noisland", "info");
        }
      }
    }
    else {
      plugin.write(player, "command.addfriend.perms", "info");
    }
  }

  public void cmdFriendRemove(Player player, String friend) throws SQLException {
    if (plugin.checkPerk(player, "simpleskyblock.sb.friend.remove")) {
      if (friend == null) {
        plugin.write(player, "command.removefriend.nonick", "info");
      }
      else if (!(friend.matches("[0-9a-zA-Z@_!\\-]{2,14}"))) {
        plugin.write(player, "command.removefriend.invalidnick", "info");
      }
      else if (friend.equalsIgnoreCase(player.getName())) {
        plugin.write(player, "command.removefriend.self", "info");
      }
      else {
        //load island data
        Island island = new Island(player.getName(), plugin);
        try {
          island.load();
        }
        catch (SQLException e) {
          plugin.write(player, "command.error.sqlexception", "info");
          e.printStackTrace();
          return;
        }

        if (island.exists) {
          RegionTools rTools = new RegionTools(plugin);

          //remove him form region members (nothing happens if he isnt't there)
          ProtectedRegion region = rTools.getWorldGuard().getRegionManager(plugin.skyworld).getRegion(player.getName() + "island");
          DefaultDomain members = region.getMembers();
          members.removePlayer(friend);
          region.setMembers(members);

          if (island.removeFriend(friend)) {
            //inform the player that he was removed
            for (Player p : plugin.getServer().getOnlinePlayers()) {
              //check nick
              if (p.getName().equalsIgnoreCase(friend)) {
                plugin.write(p, "command.removefriend.nolongerfriend", "info", player.getName());
                //tp him out if he is on the island
                if (p.getLocation().getX() > island.x - 50
                        && p.getLocation().getX() < island.x + 50
                        && p.getLocation().getZ() > island.z - 50
                        && p.getLocation().getZ() < island.z + 50) {
                  plugin.skyTp(0, 0, p);
                }
              }
            }
            plugin.write(player, "command.removefriend.starting", "info", friend);
            island.save();
          }
          else {
            plugin.write(player, "command.removefriend.alreadyremoved", "info", friend);
          }

        }
        else {
          plugin.write(player, "command.removefriend.noisland", "info");
        }
      }
    }
    else {
      plugin.write(player, "command.removefriend.perms", "info");
    }

  }

  public void cmdFriendList(Player player) throws SQLException {
    if (plugin.checkPerk(player, "simpleskyblock.sb.friend.list")) {

      //load island data
      Island island = new Island(player.getName(), plugin);
      try {
        island.load();
      }
      catch (SQLException e) {
        plugin.write(player, "command.error.sqlexception", "info");
        e.printStackTrace();
        return;
      }

      if (island.exists) {

        if (!island.friends.isEmpty()) {
          plugin.write(player, "command.listfriend.own", "info");
          String friends = "";
          for (String s : island.friends) {
            friends += s + " ";
          }

          player.sendMessage(friends);

        }
        else {
          plugin.write(player, "command.listfriend.nobodyown", "info");
        }

        String visitableIslands = "SELECT islands.id, islands.nick, islands.x, islands.z, members.id, members.island_id, members.member "
                + "FROM " + plugin.getMysqlPrefix() + "_islands islands "
                + "LEFT JOIN " + plugin.getMysqlPrefix() + "_members members "
                + "ON islands.id = members.island_id "
                + "WHERE LOWER(members.member) = LOWER('" + player.getName() + "');";
        ResultSet rs = plugin.database.querySQL(visitableIslands);
        if (rs.next()) {
          plugin.write(player, "command.listfriend.other", "info");
          rs.previous();
          String friends = "";
          while (rs.next()) {
            friends += rs.getString("nick") + " ";
          }
          player.sendMessage(friends);
        }
        else {
          plugin.write(player, "command.listfriend.nobodyother", "info");
        }
      }
      else {
        plugin.write(player, "command.listfriend.noisland", "info");

      }
    }
    else {
      plugin.write(player, "command.listfriend.perms", "info");
    }
  }

  public void cmdFriendClear(Player player) throws SQLException {
    if (plugin.checkPerk(player, "simpleskyblock.sb.friend.clear")) {

      //load island data
      Island island = new Island(player.getName(), plugin);
      try {
        island.load();
      }
      catch (SQLException e) {
        plugin.write(player, "command.error.sqlexception", "info");
        e.printStackTrace();
        return;
      }
      int deletedRows = island.removeAll();

      switch (deletedRows) {
        case 0:
          plugin.write(player, "command.clearfriends.zero", "info");
          break;
        case 1:
          plugin.write(player, "command.clearfriends.one", "info");
          break;
        case 2:
        case 3:
          plugin.write(player, "command.clearfriends.twothree", "info", deletedRows);
          break;
        default:
          plugin.write(player, "command.clearfriends.many", "info", deletedRows);
          break;
      }
      island.save();
    }
    else {
      plugin.write(player, "command.clearfriends.perms", "info");

    }
  }
}
