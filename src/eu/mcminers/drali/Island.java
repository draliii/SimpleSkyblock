package eu.mcminers.drali;

/**
 * @author dita
 */
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

public class Island {

  private SimpleSkyblock plugin;
  public int x;
  public int z;
  public int id;
  public long date;
  public String ownerNick;
  public Player owner;
  public ArrayList<String> friends;
  public boolean active;
  public boolean exists;

  Island(String owner, SimpleSkyblock plugin) {
    this.plugin = plugin;
    this.ownerNick = owner;
  }

  private boolean loadData() {
    Island tmp = plugin.playerIslands.get(this.ownerNick);
    if (tmp == null) {
      return false;
    }
    else {
      this.x = tmp.x;
      this.z = tmp.z;
      this.id = tmp.id;
      this.date = tmp.date;
      this.active = tmp.active;
      this.exists = tmp.exists;
      this.friends = tmp.friends;

      this.owner = null;
      for (Player p : plugin.getServer().getOnlinePlayers()) {
        if (p.getName().equals(this.ownerNick)) {
          this.owner = p;
          break;
        }
      }
      return true;
    }
  }

  private void loadSQL() throws SQLException {
    String sql = "SELECT islands.id, islands.nick, islands.x, islands.z, islands.active, islands.date,"
            + "members.id, members.island_id, members.member "
            + "FROM " + plugin.getMysqlPrefix() + "_islands islands "
            + "LEFT JOIN " + plugin.getMysqlPrefix() + "_members members "
            + "ON islands.id = members.island_id "
            + "WHERE LOWER(islands.nick) = LOWER('" + this.ownerNick + "');";
    //String sql = "SELECT id, x, z, nick, date, active FROM " + plugin.mysqlPrefix + "_islands WHERE LOWER(nick) = LOWER('" + this.ownerNick + "') LIMIT 1";
    ResultSet rs = plugin.database.querySQL(sql);

    if (rs.next()) {
      this.x = rs.getInt("islands.x");
      this.z = rs.getInt("islands.z");
      this.id = rs.getInt("islands.id");
      this.date = rs.getLong("islands.date");
      this.active = rs.getBoolean("islands.active");
      this.exists = true;

      this.owner = null;
      for (Player p : plugin.getServer().getOnlinePlayers()) {
        if (p.getName().equals(this.ownerNick)) {
          this.owner = p;
          break;
        }
      }

      ArrayList<String> members = new ArrayList();
      members.add(" ");

      do {
        members.add(rs.getString("members.member"));
      }
      while (rs.next());

      this.friends = members;

      for (int i = 0; i < this.friends.size(); i++) {
        plugin.write(null, "debug.members-list", "debug", i, this.friends.get(i));
      }
    }
    else {
      this.exists = false;
    }
  }

  /**
   * Loads player data from a variable or database.
   *
   * @throws SQLException in case something goes wrong in the database
   */
  public void load() throws SQLException {
    if (!this.loadData()) {
      this.loadSQL();
      plugin.playerIslands.put(ownerNick, this);
    }
  }

  @Override
  public String toString() {
    return plugin.out.format("command.info.out", this.ownerNick, this.id, this.x, this.z, this.active);
  }

  public void tpVisitors() {
    this.tpVisitors(false);
  }
  
  public void tpVisitors(boolean includeOwner){
   
    Player[] visitors = this.getVisitors(includeOwner);
    for (int i = 0; i < visitors.length; i++) {
      //teleport players to center point
      plugin.skyTp(0, 0, visitors[i]);
      if (plugin.deleteVisitorInv) {
        plugin.clearInventory(visitors[i]);
      }
    }
  }

  public void tpHome(Player player) {
    int h = plugin.getISLANDS_Y();

    //check until two safe blocks (air) are found
    while (plugin.skyworld.getBlockAt(x, h, z).getType() != Material.AIR
            || plugin.skyworld.getBlockAt(x, h + 1, z).getType() != Material.AIR) {
      h++;
    }

    plugin.skyworld.loadChunk(x, z);

    //add 0.5 to x and z, so the player gets teleported in the middle of a block
    //do h - 0.5 so the player stands on the ground and doesn't levitate (his head might gt stuck in something)
    player.teleport(new Location(plugin.skyworld, (x + 0.5), (h), (z + 0.5)));
  }

  public Player[] getVisitors(){
    return this.getVisitors(false);
  }
  
  public Player[] getVisitors(boolean includeOwner) {
    Location loc;

    //get max and min value (area of the island with center x and z)
    //y value doesn't matter
    int maxx = this.x + (plugin.getIslandSize() / 2);
    int maxz = this.z + (plugin.getIslandSize() / 2);

    int minx = this.x - (plugin.getIslandSize() / 2);
    int minz = this.z - (plugin.getIslandSize() / 2);

    int px;
    int pz;

    ArrayList<Player> result = new ArrayList();
    for (Player player : plugin.skyworld.getPlayers()) {
      if(player.getName().equalsIgnoreCase(ownerNick) && !includeOwner){
        break;
      }
      else{
        loc = player.getLocation();
        px = loc.getBlockX();
        pz = loc.getBlockZ();
        if (px >= minx && px <= maxx && pz >= minz && pz <= maxz) {
          result.add(player);
        }
      }
    }
    return result.toArray(new Player[0]);
  }

  public boolean isFriend(String playerName) {
    if (this.friends != null) {
      for (String name : this.friends) {
        try {
          if (name.equalsIgnoreCase(playerName)) {
            return true;
          }
        }
        catch (NullPointerException e) {
          return false;
        }
      }
    }
    return false;
  }

  public void addFriend(String playerName) throws SQLException {
    String addMember = "INSERT INTO " + plugin.getMysqlPrefix() + "_members (`island_id`, `member`)"
            + "VALUES ('" + this.id + "', '" + playerName + "');";
    //remove him from the database
    plugin.database.updateSQL(addMember);

    try {
      friends.add(playerName);
    }
    catch (NullPointerException e) {
      this.loadSQL();
      friends.add(playerName);
    }
  }

  public boolean removeFriend(String playerName) throws SQLException {
    if (this.isFriend(playerName)) {
      this.friends.remove(playerName);
      String deleteMember = "DELETE FROM " + plugin.getMysqlPrefix() + "_members WHERE island_id = '" + this.id + "'"
              + "AND LOWER(member) = LOWER('" + playerName + "');";
      plugin.database.updateSQL(deleteMember);
      return true;
    }
    return false;
  }

  public int removeAll() throws SQLException {
    String deleteFriends = "DELETE FROM " + plugin.getMysqlPrefix() + "_members WHERE island_id = '" + this.id + "';";
    int deletedRows = plugin.database.updateSQL(deleteFriends);

    this.tpVisitors();
    this.friends.clear();

    return deletedRows;
  }

  /**
   * Checks for a place to generate an island (write data about island into
   * database, delete data about previous owner).
   *
   * @param player the new owner of the island
   * @throws SQLException
   */
  public boolean create() throws SQLException {
    //get an abandoned island (closest to [0, 0])
    Island empty = plugin.getEmptyIsland();
    
    //if no empty island was found
    if (empty == null) {
      
      //get x, z of the island with highest ID (the latest generated, on the edge of the grid)
      Coordinates last = plugin.getLastIsland();

      IslandTools iTools = new IslandTools(plugin);
      //look for coordinates of the crds island to be generated
      System.out.println(last.x + ", " + last.z);
      Coordinates crds = iTools.nextIslandLocation(last.x, last.z);
      //insert data about the island into database


      /*
       while(!iTools.isEmpty(crds)){
       crds = iTools.nextIslandLocation(crds.x, crds.z);
       }
       */
      
      this.exists = true;
      this.active = true;
      this.date = System.currentTimeMillis() / 1000;
      this.x = crds.x;
      this.z = crds.z;

      String insert = "INSERT INTO " + plugin.getMysqlPrefix() + "_islands (`x`, `z`, `nick`, `date`)"
              + "VALUES (" + this.x + ", " + this.z + ", '" + this.ownerNick + "', '" + this.date + "');";
      plugin.database.updateSQL(insert);
    }
    
    //if an abandoned island was found
    else {
      //delete all players that could tphome to that island before
      String deleteOldMembers = "DELETE FROM " + plugin.getMysqlPrefix() + "_members"
              + " WHERE island_id = " + empty.id + ";";
      int rowsDeleted = plugin.database.updateSQL(deleteOldMembers);
      plugin.write(null, "admin.sql.queryrows", "debug", rowsDeleted);

      //get the previous owner's nick and delete the WG region
      //NO NEED TO DO THIS - regions are deleted on island deactivation
      /*String previousPlayerSql = "SELECT `nick`, `x`, `z` FROM " + plugin.getMysqlPrefix() + "_islands WHERE id = '" + emptyIsland[0] + "';";
      ResultSet rs = plugin.database.querySQL(previousPlayerSql);
      if (rs.crds()) {//if the query found something
        //delete the region
        this.x = rs.getInt("x");
        this.z = rs.getInt("z");
        String previousPlayer = rs.getString("nick");
        RegionTools rTools = new RegionTools(plugin);
        rTools.getWorldGuard().getRegionManager(plugin.skyworld).removeRegion(previousPlayer + "Island");
        plugin.debug("Deleting region of " + previousPlayer, "info");
      }
      else {
        plugin.debug("ERROR, inactive nick wasn't found", "severe");
      }*/

      this.exists = true;
      this.active = true;
      this.date = System.currentTimeMillis() / 1000;
      this.id = empty.id;
      this.x = empty.x;
      this.z = empty.z;
              

      //clear previous data (owner, date) and replace them with new data
      String updateOldData = "UPDATE " + plugin.getMysqlPrefix() + "_islands "
              + " SET nick = '" + this.ownerNick + "', date = '" + this.date + "', active = 1 "
              + " WHERE id = " + this.id
              + " LIMIT 1;";
      int rowsUpdated = plugin.database.updateSQL(updateOldData);
      plugin.write(null, "admin.sql.queryrows", "debug", rowsUpdated);
    }
    plugin.write(null, "debug.sql-queries-done", "debug");
    return true;
  }

  public void reset() throws SQLException {
    this.date = System.currentTimeMillis() / 1000;
    //update database (reset date)
    String updateOldData = "UPDATE " + plugin.getMysqlPrefix() + "_islands "
            + " SET date = '" + this.date + "'"
            + " WHERE nick = '" + this.ownerNick + "'"
            + " LIMIT 1;";
    plugin.database.updateSQL(updateOldData);

    //teleport everybody out
    this.tpVisitors();

    //delete and rebuild the islnad blocks
    IslandTools iTools = new IslandTools(plugin);
    iTools.generateIslandBlocks(this.x, this.z, this.ownerNick);

    plugin.clearInventory(owner);
    this.deleteItems();
  }

  public void deleteItems() {
    List<Entity> entList = plugin.skyworld.getEntities();//get all entities in the world

    int maxx = this.x + (plugin.getIslandSize() / 2);
    int maxz = this.z + (plugin.getIslandSize() / 2);

    int minx = this.x - (plugin.getIslandSize() / 2);
    int minz = this.z - (plugin.getIslandSize() / 2);

    int px;
    int pz;
    for (Entity current : entList) {//loop through the list
      if (current instanceof Item) {//make sure we aren't deleting mobs/players
        Location loc = current.getLocation();
        px = loc.getBlockX();
        pz = loc.getBlockZ();
        if (px >= minx && px <= maxx && pz >= minz && pz <= maxz) {
          current.remove();//remove it
        }
      }
    }
  }

  public void deactivate() throws SQLException {
    this.tpVisitors();
    String updateInactiveSql = "UPDATE " + plugin.getMysqlPrefix() + "_islands "
            + "SET `active` = 0 WHERE `nick` = '" + this.ownerNick + "';";
    plugin.database.updateSQL(updateInactiveSql);

    plugin.clearInventory(owner);
    plugin.skyTp(0, 0, owner);

    this.deleteItems();
    this.active = false;
  }

  public void activate() throws SQLException {
    String updateInactiveSql = "UPDATE " + plugin.getMysqlPrefix() + "_islands "
            + "SET `active` = 1 WHERE `nick` = '" + this.ownerNick + "';";
    plugin.database.updateSQL(updateInactiveSql);
    this.active = true;
  }

  public void save() {
    plugin.write(null, "debug.saving-island-data", "debug", ownerNick);

    if (plugin.playerIslands.containsKey(this.ownerNick)) {
      plugin.playerIslands.remove(this.ownerNick);
    }

    plugin.playerIslands.put(this.ownerNick, this);
  }
}