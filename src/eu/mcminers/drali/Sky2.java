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

    //deny access to non-player senders
    if (!(sender instanceof Player)) {
      plugin.print("", false, "info");
      return true;
    }

    Player player = (Player) sender;

    if (args.length == 0) {
      player.sendMessage(plugin.out.get("command.error.unknown"));
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

          case "getpos":
            Coordinates last = plugin.getLastIsland();

            IslandTools iTools = new IslandTools(plugin);
            //look for coordinates of the crds island to be generated
            Coordinates crds = iTools.nextIslandLocation(last.x, last.z);
            /*
             while (!iTools.isEmpty(crds)) {
             crds = iTools.nextIslandLocation(crds.x, crds.z);
             }*/
            player.sendMessage("x: " + crds.x + ", z: " + crds.z);
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
              player.sendMessage(plugin.out.get("command.error.unknown"));
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
                player.sendMessage(plugin.out.get("command.error.unknown"));
                break;
            }
            break;

          default:
            player.sendMessage(plugin.out.get("command.error.unknown"));
            break;
        }
        //after the switch

        return true;
      }
      catch (SQLException | ProtectedRegion.CircularInheritanceException | InvalidFlagFormat | ProtectionDatabaseException ex) {
        player.sendMessage(plugin.out.get("command.error.exception"));
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
        player.sendMessage(plugin.out.get("command.error.sqlexception"));
        e.printStackTrace();
        return;
      }
      //if the database query found nothing about the player
      if (!island.exists) {
        player.sendMessage(plugin.out.get("command.tphome.noisland"));
      }
      //if his island is inactive
      else if (!island.active) {
        player.sendMessage(plugin.out.get("command.tphome.inactive"));
      }
      else {
        player.sendMessage(plugin.out.get("command.tphome.teleporting"));
        island.tpHome(player);
      }
    }
    //if player doesn't have permission
    else {
      player.sendMessage(plugin.out.get("command.tphome.perms"));
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
        player.sendMessage(plugin.out.get("command.tpfriend.invalidnick"));
      }
      else {
        //load island data
        Island island = new Island(visited, plugin);
        try {
          island.load();
        }
        catch (SQLException e) {
          player.sendMessage(plugin.out.get("command.error.sqlexception"));
          e.printStackTrace();
          return;
        }

        //no results found (the visited nick doesn't exist)
        if (!island.exists) {
          player.sendMessage(plugin.out.get("command.tpfriend.noisland"));
          return;
        }

        boolean op = plugin.checkPerk(player, "simpleskyblock.admin.tpall");

        if (island.isFriend(player.getName()) | op) {

          //if the island is inactive
          if (!island.active) {
            player.sendMessage(plugin.out.get("command.tpfriend.inactive"));
            if (!op) {
              return;
            }
            player.sendMessage(plugin.out.get("admin.tp.inactive"));
          }

          player.sendMessage(plugin.out.format("command.tpfriend.teleporting", visited));
          if (!island.isFriend(player.getName())) {
            player.sendMessage(plugin.out.get("admin.tp.nonfriend"));
          }
          island.tpHome(player);
        }
        else {
          player.sendMessage(plugin.out.format("command.tpfriend.denied", visited));
        }


      }
    }
    //if he doesn't have permission
    else {
      player.sendMessage("command.tpfriend.perms");
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
        player.sendMessage(plugin.out.get("command.error.sqlexception"));
        e.printStackTrace();
        return;
      }
      //if no island is found
      if (!island.exists) {
        player.sendMessage(plugin.out.get("command.info.noisland"));
      }
      //if something is found
      else {
        player.sendMessage(island.toString());
      }
    }
    else {
      player.sendMessage(plugin.out.get("command.info.perms"));
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
        player.sendMessage(plugin.out.get("command.error.sqlexception"));
        e.printStackTrace();
        return;
      }
      IslandTools iTools = new IslandTools(plugin);
      RegionTools rTools = new RegionTools(plugin);

      //check if he has an island
      if (island.exists) {
        player.sendMessage(plugin.out.get("command.new.exists"));
      }
      //if he doesn't have one yet
      else {
        player.sendMessage(plugin.out.get("command.new.starting"));

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
        player.sendMessage(plugin.out.get("command.new.finished"));
      }
    }
    else {
      player.sendMessage(plugin.out.get("command.new.perms"));
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
        player.sendMessage(plugin.out.get("command.error.sqlexception"));
        e.printStackTrace();
        return;
      }

      if (!island.exists) {
        player.sendMessage(plugin.out.get("command.reset.noisland"));
      }
      else if (!island.active) {
        player.sendMessage(plugin.out.get("command.reset.inactive"));
      }
      else {
        player.sendMessage(plugin.out.get("command.reset.starting"));

        try {
          island.reset();
        }
        catch (SQLException e) {
          player.sendMessage(plugin.out.get("command.error.sqlexception"));
          e.printStackTrace();
          return;
        }

        player.sendMessage(plugin.out.get("command.reset.finished"));

        island.tpHome(player);
        island.save();
      }
    }
    else {
      player.sendMessage(plugin.out.get("command.reset.perms"));
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
        player.sendMessage(plugin.out.get("command.error.sqlexception"));
        e.printStackTrace();
        return;
      }

      RegionTools rTools = new RegionTools(plugin);
      if (!island.exists) {
        player.sendMessage(plugin.out.get("command.delete.noisland"));
      }
      else if (!island.active) {
        player.sendMessage(plugin.out.get("command.delete.inactive"));
      }
      else {
        player.sendMessage(plugin.out.get("command.delete.starting"));

        island.deactivate();

        //remove the region
        rTools.deleteRegion(player.getName() + "island");

        player.sendMessage(plugin.out.get("command.delete.finished"));
        island.save();
      }
    }
    else {
      player.sendMessage(plugin.out.get("command.delete.perms"));
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
        player.sendMessage(plugin.out.get("command.error.sqlexception"));
        e.printStackTrace();
        return;
      }

      RegionTools rTools = new RegionTools(plugin);
      if (!island.exists) {
        player.sendMessage(plugin.out.get("command.active.noisland"));
      }
      else if (island.active) {
        player.sendMessage(plugin.out.get("command.active.active"));
      }
      else {
        //set it to active in the database
        island.activate();

        //make a new region and restore the previous player settings
        rTools.restorePerms(rTools.createRegion(island.x, island.z, player), island.id);

        //teleport to home
        island.tpHome(player);

        player.sendMessage(plugin.out.get("command.active.finished"));
        island.save();
      }

    }
    else {
      player.sendMessage(plugin.out.get("command.active.perms"));
    }
  }

  public void cmdSpawn(Player player) {
    plugin.skyTp(0, 0, player);
  }

  public void cmdFriendAdd(Player player, String friend) throws
          SQLException {
    if (plugin.checkPerk(player, "simpleskyblock.sb.friend.add")) {
      if (friend == null) {
        player.sendMessage(plugin.out.get("command.addfriend.nonick"));
      }
      else if (!(friend.matches("[0-9a-zA-Z@_!\\-]{2,14}"))) {
        player.sendMessage(plugin.out.get("command.addfriend.invalidnick"));
      }
      else if (friend.equalsIgnoreCase(player.getName())) {
        player.sendMessage(plugin.out.get("command.addfriend.self"));
      }
      else {

        //load island data
        Island island = new Island(player.getName(), plugin);
        try {
          island.load();
        }
        catch (SQLException e) {
          player.sendMessage(plugin.out.get("command.error.sqlexception"));
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
            player.sendMessage(plugin.out.format("command.addfriend.alreadyadded", friend));
          }
          //add him to the database
          else {
            player.sendMessage(plugin.out.format("command.addfriend.starting", friend));
            island.addFriend(friend);

            //inform the player that he was added
            for (Player p : plugin.getServer().getOnlinePlayers()) {
              //check nick
              if (p.getName().equalsIgnoreCase(friend)) {
                p.sendMessage(plugin.out.format("command.addfriend.friend", player.getName()));
              }
            }
            
            island.save();
          }
        }
        else {
          player.sendMessage(plugin.out.get("command.addfriend.noisland"));
        }
      }
    }
    else {
      player.sendMessage(plugin.out.format("command.addfriend.perms"));
    }
  }

  public void cmdFriendRemove(Player player, String friend) throws SQLException {
    if (plugin.checkPerk(player, "simpleskyblock.sb.friend.remove")) {
      if (friend == null) {
        player.sendMessage(plugin.out.get("command.removefriend.nonick"));
      }
      else if (!(friend.matches("[0-9a-zA-Z@_!\\-]{2,14}"))) {
        player.sendMessage(plugin.out.get("command.removefriend.invalidnick"));
      }
      else if (friend.equalsIgnoreCase(player.getName())) {
        player.sendMessage(plugin.out.get("command.removefriend.self"));
      }
      else {
        //load island data
        Island island = new Island(player.getName(), plugin);
        try {
          island.load();
        }
        catch (SQLException e) {
          player.sendMessage(plugin.out.get("command.error.sqlexception"));
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
                p.sendMessage(plugin.out.format("command.removefriend.nolongerfriend", player.getName()));
                //tp him out if he is on the island
                if(p.getLocation().getX() > island.x - 50
                        && p.getLocation().getX() < island.x + 50
                        && p.getLocation().getZ() > island.z - 50
                        && p.getLocation().getZ() < island.z + 50){
                  plugin.skyTp(0, 0, p);
                }
              }
            }
            player.sendMessage(plugin.out.format("command.removefriend.starting", friend));
            island.save();
          }
          else {
            player.sendMessage(plugin.out.format("command.removefriend.alreadyremoved", friend));
          }

        }
        else {
          player.sendMessage(plugin.out.get("command.removefriend.noisland"));
        }
      }
    }
    else {
      player.sendMessage(plugin.out.get("command.removefriend.perms"));
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
        player.sendMessage(plugin.out.get("command.error.sqlexception"));
        e.printStackTrace();
        return;
      }

      if (island.exists) {

        if (!island.friends.isEmpty()) {
          player.sendMessage(plugin.out.get("command.listfriend.own"));
          String friends = "";
          for (String s : island.friends) {
            friends += s + " ";
          }

          player.sendMessage(friends);

        }
        else {
          player.sendMessage(plugin.out.get("command.listfriend.nobodyown"));
        }

        String visitableIslands = "SELECT islands.id, islands.nick, islands.x, islands.z, members.id, members.island_id, members.member "
                + "FROM " + plugin.getMysqlPrefix() + "_islands islands "
                + "LEFT JOIN " + plugin.getMysqlPrefix() + "_members members "
                + "ON islands.id = members.island_id "
                + "WHERE LOWER(members.member) = LOWER('" + player.getName() + "');";
        ResultSet rs = plugin.database.querySQL(visitableIslands);
        if (rs.next()) {
          player.sendMessage(plugin.out.get("command.listfriend.other"));
          rs.previous();
          String friends = "";
          while (rs.next()) {
            friends += rs.getString("nick") + " ";
          }
          player.sendMessage(friends);
        }
        else {
          player.sendMessage(plugin.out.get("command.listfriend.nobodyother"));
        }
      }
      else {
        player.sendMessage(plugin.out.get("command.listfriend.noisland"));

      }
    }
    else {
      player.sendMessage(plugin.out.get("command.listfriend.perms"));
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
        player.sendMessage(plugin.out.get("command.error.sqlexception"));
        e.printStackTrace();
        return;
      }
      int deletedRows = island.removeAll();

      switch (deletedRows) {
        case 0:
          player.sendMessage(plugin.out.get("command.clearfriends.zero"));
          break;
        case 1:
          player.sendMessage(plugin.out.get("command.clearfriends.one"));
          break;
        case 2:
        case 3:
          player.sendMessage(plugin.out.format("command.clearfriends.twothree", deletedRows));
          break;
        default:
          player.sendMessage(plugin.out.format("command.clearfriends.many", deletedRows));
          break;
      }
      island.save();
    }
    else {
      player.sendMessage(plugin.out.get("command.clearfriends.perms"));

    }
  }
}
