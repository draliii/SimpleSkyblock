package eu.mcminers.drali;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;
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
public class Sky implements CommandExecutor {

  private final SimpleSkyblock plugin;

  public Sky(SimpleSkyblock plugin) {
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
      player.sendMessage(plugin.out.get("command.error.noparameters"));
      plugin.displayHelp(player);
      return true;
    }
    else if (args[0].equalsIgnoreCase("help")) {
      plugin.displayHelp(player);
      return true;
    }
    else {
      Island island = null;
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
              player.sendMessage(plugin.out.get("command.error.noparameters"));
              plugin.displayHelp(player);
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
                player.sendMessage(plugin.out.get("command.error.unknownparameter"));
                plugin.displayHelp(player);
                break;
            }
            break;

          default:
            player.sendMessage(plugin.out.get("command.error.unknownparameter"));
            plugin.displayHelp(player);
            break;
        }
        //after the switch
        return true;
      }
      catch (SQLException ex) {
        ex.printStackTrace();
        player.sendMessage("SQL Error. Please contact server administrator.");
        //plugin.getLogger().info("SQL Error when executing command of " + player.getName()
        //+ " at [" + island.x + ", " + plugin.getISLANDS_Y() + ", " + island.x + "]\n" + ex);
        ex.printStackTrace();
      }
      catch (ProtectedRegion.CircularInheritanceException ex) {
        player.sendMessage("Error when creating region for you. Please contact server administrator.");
        plugin.getLogger().info("WorldGuard error when executing command of " + player.getName()
                + " at [" + island.x + ", " + plugin.getISLANDS_Y() + ", " + island.x + "]\n" + ex);
      }
      catch (InvalidFlagFormat ex) {
        ex.printStackTrace();
        player.sendMessage("Error when creating region for you. Please contact server administrator.");
        plugin.getLogger().info("WorldGuard error when executing command of  " + player.getName()
                + " at [" + island.x + ", " + plugin.getISLANDS_Y() + ", " + island.x + "]\n" + ex);
      }
      catch (ProtectionDatabaseException ex) {
        ex.printStackTrace();
        Logger.getLogger(Sky.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    return false;
  }

  public void cmdHomeSelf(Player player) throws SQLException {

    //check permission
    if (plugin.checkPerk(player, "simpleskyblock.sb.home.self")) {
      //load island data
      Island island = plugin.getPlayerData(player);
      //if the database query found nothing about the player
      if (island == null) {
        player.sendMessage(plugin.out.get("command.tphome.noisland"));
      }
      //if his island is inactive
      else if (island.active == false) {
        player.sendMessage(plugin.out.get("command.tphome.inactive"));
      }
      else {
        player.sendMessage(plugin.out.get("command.tphome.teleporting"));
        plugin.skyTp(island.x, island.z, player);
      }
    }
    //if player doesn't have permission
    else {
      player.sendMessage(plugin.out.get("command.tphome.perms"));
    }
  }

  public void cmdHomeSelf(Island island, Player player) throws SQLException {
    //check permission
    if (plugin.checkPerk(player, "simpleskyblock.sb.home.self")) {
      //if the database query found nothing about the player
      if (island == null) {
        player.sendMessage(plugin.out.get("command.tphome.noisland"));
      }
      //if his island is inactive
      else if (island.active == false) {
        player.sendMessage(plugin.out.get("command.tphome.inactive"));
      }
      else {
        player.sendMessage(plugin.out.get("command.tphome.teleporting"));
        plugin.skyTp(island.x, island.z, player);
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

        String sql = "SELECT islands.id, islands.nick, islands.x, islands.z, islands.active, islands.date, members.id, members.island_id, members.member "
                + "FROM " + plugin.getMysqlPrefix() + "_islands islands "
                + "LEFT JOIN " + plugin.getMysqlPrefix() + "_members members "
                + "ON islands.id = members.island_id "
                + "WHERE LOWER(islands.nick) = LOWER('" + visited + "');";
        ResultSet rs = plugin.database.querySQL(sql);
        //no results found (the visited nick doesn't exist)
        if (!(rs.next())) {
          player.sendMessage(plugin.out.get("command.tpfriend.noisland"));
          return;
        }
        //load data from the rs
        Island island = plugin.loadIslandData(rs);

        //if the island is inactive
        if (!island.active) {
          player.sendMessage(plugin.out.get("command.tpfriend.inactive"));
          return;
        }

        do {
          //if the sender's nick is found, teleport him there
          if (rs.getString("member") != null && rs.getString("member").equalsIgnoreCase(player.getName())) {
            player.sendMessage(plugin.out.format("command.tpfriend.teleporting", visited));
            plugin.skyTp(island.x, island.z, player);
            return;
          }
        }
        while (rs.next());

        player.sendMessage(plugin.out.format("command.tpfriend.denied", visited));

        /*
         String sql = "SELECT islands.id, islands.nick, islands.x, islands.z, islands.active, islands.date, members.id, members.island_id, members.member "
         + "FROM " + plugin.getMysqlPrefix() + "_islands islands "
         + "LEFT JOIN " + plugin.getMysqlPrefix() + "_members members "
         + "ON islands.id = members.island_id "
         + "WHERE islands.nick = '" + visited + "' AND members.member = '" + player.getName() + "';";
         ResultSet rs = plugin.database.querySQL(sql);
         //no results found (the visited nick doesn't exist)
         if (!(rs.next())) {
         player.sendMessage(plugin.out.get("command.tpfriend.noisland"));
         return;
         }
         //load data from the rs
         Island island = plugin.loadIslandData(rs);

         //if the island is inactive
         if (!island.active) {
         player.sendMessage(plugin.out.get("command.tpfriend.inactive"));
         return;
         }

         //if the sender's nick is found, teleport him there
         if (rs.getString("member").equalsIgnoreCase(player.getName())) {
         player.sendMessage(plugin.out.format("command.tpfriend.teleporting", visited));
         plugin.skyTp(island.x, island.z, player);
         }
         else {
         player.sendMessage(plugin.out.format("command.tpfriend.denied", visited));
         }
         */
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
      Island island = plugin.getPlayerData(player);
      //if no island is found
      if (island == null) {
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
      Island island = null;
      island = plugin.getPlayerData(player);
      IslandTools iTools = new IslandTools(plugin);
      RegionTools rTools = new RegionTools(plugin);

      //check if he has an island
      if (island != null) {
        player.sendMessage(plugin.out.get("command.new.exists"));
      }
      //if he doesn't have one yet
      else {
        player.sendMessage(plugin.out.get("command.new.starting"));

        iTools.createIslandSQL(player); //find coordinates, write into database
        //player.sendMessage("Your island is now in the database");

        island = plugin.getPlayerData(player); //do the query again to have the new island data

        iTools.generateIslandBlocks(island.x, island.z, island.ownerNick); //clear the area and generate new blocks (both is in the method)
        //player.sendMessage("Your island was now spawned");

        rTools.createRegion(island.x, island.z, player); //create the region and protect it
        //player.sendMessage("Your island was protected by a region");

        plugin.skyTp(island.x, island.z, player); //teleport the player to his island
        plugin.clearInventory(player);
        player.sendMessage(plugin.out.get("command.new.finished"));
      }
    }
    else {
      player.sendMessage(plugin.out.get("command.new.perms"));
    }
  }

  public void cmdReset(Player player) throws SQLException {
    if (plugin.checkPerk(player, "simpleskyblock.sb.reset")) {

      //load island data
      Island island = plugin.getPlayerData(player);
      IslandTools iTools = new IslandTools(plugin);
      if (island == null) {
        player.sendMessage(plugin.out.get("command.reset.noisland"));
      }
      else if (!island.active) {
        player.sendMessage(plugin.out.get("command.reset.inactive"));
      }
      else {
        player.sendMessage(plugin.out.get("command.reset.starting"));
        //update database (reset date)
        String updateOldData = "UPDATE " + plugin.getMysqlPrefix() + "_islands "
                + " SET date = '" + System.currentTimeMillis() / 1000 + "'"
                + " WHERE nick = '" + player.getName() + "'"
                + " LIMIT 1;";
        plugin.database.updateSQL(updateOldData);

        //teleport everybody out
        plugin.tpVisitors(island.x, island.z);

        //delete and rebuild the islnad blocks
        iTools.generateIslandBlocks(island.x, island.z, island.ownerNick);

        player.sendMessage(plugin.out.get("command.reset.finished"));

        //teleport the player back, delete his inventory and entities around him
        plugin.skyTp(island.x, island.z, player);
        plugin.clearInventory(player);
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
      Island island = plugin.getPlayerData(player);
      IslandTools iTools = new IslandTools(plugin);
      RegionTools rTools = new RegionTools(plugin);
      if (!(plugin.hasIsland(player))) {
        player.sendMessage(plugin.out.get("command.delete.noisland"));
      }
      else if (!island.active) {
        player.sendMessage(plugin.out.get("command.delete.inactive"));
      }
      else {
        player.sendMessage(plugin.out.get("command.delete.starting"));

        //teleport everybody out of the island
        plugin.tpVisitors(island.x, island.z);

        //clear his inventory
        plugin.clearInventory(player);

        //deactivate (set to inactive in the database)
        iTools.deactivateIsland(player.getName());

        //remove the region
        rTools.deleteRegion(player.getName() + "island");

        player.sendMessage(plugin.out.get("command.delete.finished"));

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
      Island island = plugin.getPlayerData(player);
      IslandTools iTools = new IslandTools(plugin);
      RegionTools rTools = new RegionTools(plugin);
      if (island == null) {
        player.sendMessage(plugin.out.get("command.active.noisland"));
      }
      else if (island.active) {
        player.sendMessage(plugin.out.get("command.active.active"));
      }
      else {
        //set it to active in the database
        iTools.activateIsland(player.getName());
        //make a new WorldGuard region, too (since it had to be deleted when setting the island to inactive)
        //find a better way to deal with inactive regions
        rTools.createRegion(island.x, island.z, player);
        //teleport to home
        island = plugin.getPlayerData(player);
        player.sendMessage(plugin.out.get("command.active.finished"));
        this.cmdHomeSelf(island, player);
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
        Island island = plugin.getPlayerData(player);
        RegionTools rTools = new RegionTools(plugin);

        //add him as region member (nothing happens if he is added already)
        ProtectedRegion region = rTools.getWorldGuard().getRegionManager(plugin.skyworld).getRegion(player.getName() + "island");
        DefaultDomain members = region.getMembers();
        members.addPlayer(friend);
        region.setMembers(members);

        //check if the player isn't added already
        String checkMember = "SELECT member FROM " + plugin.getMysqlPrefix() + "_members WHERE island_id = '" + island.id + "' AND LOWER(member) = LOWER('" + friend + "');";
        ResultSet rs = plugin.database.querySQL(checkMember);

        if (rs.next()) {
          player.sendMessage(plugin.out.format("command.addfriend.alreadyadded", friend));
        }
        //add him to the database
        else {
          player.sendMessage(plugin.out.format("command.addfriend.starting", friend));
          //player.sendMessage("Database:");
          String addMember = "INSERT INTO " + plugin.getMysqlPrefix() + "_members (`island_id`, `member`) VALUES ('" + island.id + "', '" + friend + "');";
          //remove him from the database
          int queriesCount = plugin.database.updateSQL(addMember);
          //player.sendMessage("QueriesCount:" + queriesCount);

          //inform the player that he was added
          for (Player p : plugin.getServer().getOnlinePlayers()) {
            //check nick
            if (p.getName().equalsIgnoreCase(friend)) {
              p.sendMessage(plugin.out.format("command.addfriend.friend", player.getName()));
            }
          }
        }
      }
    }
    else {
      player.sendMessage(plugin.out.format("command.addfriend.perms"));
    }
  }

  public void cmdFriendRemove(Player player, String friend) throws SQLException {
    if (plugin.checkPerk(player, "simpleskyblock.sb.friend.add")) {
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
        Island island = plugin.getPlayerData(player);
        RegionTools rTools = new RegionTools(plugin);

        //remove him form region members (nothing happens if he isnt't there)
        ProtectedRegion region = rTools.getWorldGuard().getRegionManager(plugin.skyworld).getRegion(player.getName() + "island");
        DefaultDomain members = region.getMembers();
        members.removePlayer(friend);
        region.setMembers(members);

        //check if the player is added already
        //remove him from the database (nothing happens if he isn't there)
        String deleteMember = "DELETE FROM " + plugin.getMysqlPrefix() + "_members WHERE island_id = '" + island.id + "' AND LOWER(member) = LOWER('" + friend + "');";
        int deletedRows = plugin.database.updateSQL(deleteMember);

        if (deletedRows != 0) {
          //check if removed player is on the island
          Player[] visitors = plugin.getPlayersOnIsland(island.x, island.z);
          //look in the array for his nick
          for (int i = 0; i < visitors.length; i++) {
            //if he is found, teleport him out
            if (visitors[i].getName().equalsIgnoreCase(friend)) {
              plugin.skyTp(0, 0, visitors[i]);
              plugin.skyTp(0, 0, visitors[i]);
            }
          }
          player.sendMessage(plugin.out.format("command.removefriend.starting", friend));

          //inform the player that he was removed
          for (Player p : plugin.getServer().getOnlinePlayers()) {
            //check nick
            if (p.getName().equalsIgnoreCase(friend)) {
              p.sendMessage(plugin.out.format("command.removefriend.nolongerfriend", player.getName()));
            }
          }
        }
        else {
          player.sendMessage(plugin.out.format("command.removefriend.alreadyremoved", friend));
        }

      }
    }
    else {
      player.sendMessage(plugin.out.get("command.removefriend.perms"));
    }

  }

  public void cmdFriendList(Player player) throws SQLException {
    if (plugin.checkPerk(player, "simpleskyblock.sb.friend.add")) {

      //load island data
      Island island = plugin.getPlayerData(player);
      String membersList = "SELECT member FROM " + plugin.getMysqlPrefix() + "_members WHERE island_id = '" + island.id + "';";
      ResultSet result = plugin.database.querySQL(membersList);

      if (result.next()) {
        result.previous();
        player.sendMessage(plugin.out.get("command.listfriend.own"));
        String friends = "";
        while (result.next()) {
          friends += result.getString("member") + " ";
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
      player.sendMessage(plugin.out.get("command.listfriend.perms"));
    }
  }

  public void cmdFriendClear(Player player) throws SQLException {
    if (plugin.checkPerk(player, "simpleskyblock.sb.friend.add")) {
      //load island data
      Island island = plugin.getPlayerData(player);
      String deleteFriends = "DELETE FROM " + plugin.getMysqlPrefix() + "_members WHERE island_id = '" + island.id + "';";
      int deletedRows = plugin.database.updateSQL(deleteFriends);
      Player[] visitors = plugin.getPlayersOnIsland(island.x, island.z);
      //look in the array for his nick
      for (int i = 0; i < visitors.length; i++) {
        //if he is found, teleport him out
        if (!visitors[i].getName().equalsIgnoreCase(player.getName())) {
          plugin.skyTp(0, 0, visitors[i]);
          plugin.skyTp(0, 0, visitors[i]);
        }
      }
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
    }
    else {
      player.sendMessage(plugin.out.get("command.clearfriends.perms"));

    }
  }
}
