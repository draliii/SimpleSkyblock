package eu.mcminers.drali;

/**
 * Skyblock SMP mod
 *
 * @author drali_
 * @author Noobcrew skyblock map
 * @author Qgel original code
 * @author -_Husky_- Database connection API
 * @author DJTommek MySQL queries
 */
import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import com.onarandombox.MultiverseCore.utils.WorldManager;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.managers.RegionManager;
import java.util.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;

public class SimpleSkyblock extends JavaPlugin {
  //drop protect má otevřený kód, zkontrolovat

  public static Permission perms = null;
  private FileConfiguration pluginConfig;
  public HuskyMySQL database;
  //
  public StringHandler out;
  private static final String CONF_ISLAND_Y = "island-height";
  private static final String CONF_ISLAND_SIZE = "island-size";
  private static final String CONF_ISLAND_SPACING = "island-spacing";
  private static final String CONF_GENERATE_SPAWN = "generate-spawn-island";
  private static final String CONF_GENERATE_REGION = "generate-spawn-region";
  private static final String CONF_WORLD = "world-name";
  private static final String CONF_SPAWN_X = "spawn-x";
  private static final String CONF_SPAWN_Z = "spawn-z";
  private static final String CONF_MYSQL_IP = "mysql.ip";
  private static final String CONF_MYSQL_PORT = "mysql.port";
  private static final String CONF_MYSQL_DATABASE = "mysql.database";
  private static final String CONF_MYSQL_USER = "mysql.user";
  private static final String CONF_MYSQL_PASS = "mysql.pass";
  private static final String CONF_MYSQL_PREFIX = "mysql.table-prefix";
  private static final String CONF_DEBUG = "debug";
  private static final String CONF_HARDCORE = "hardcore";
  private static final String CONF_DELETE_VISITOR_INV = "delete-visitor-inventory";
  private static final String CONF_DELETE_INV_DEACTIVATE = "delete-inventory-on-deactivate";
  private static final String CONF_LANGUAGE = "language.use";
  private static final String CONF_LANGUAGE_HIGHLIGHT = "language.highlight";
  private static final String CONF_LANGUAGE_BASE = "language.base";
  private static final String CONF_LANGUAGE_NOTICE = "language.notice";
  private static final String CONF_RESET_COOLDOWN = "island-reset-cooldown";
  public World skyworld;
  private String worldName;
  public int islandY;
  public int islandSize;
  private int islandSpacing;
  private boolean generateSpawn;
  private boolean generateRegion;
  private int spawnX;
  private int spawnZ;
  private String mysqlIp;
  private String mysqlPort;
  private String mysqlDatabase;
  private String mysqlUser;
  private String mysqlPass;
  public String mysqlPrefix;
  public int resetCooldown;
  private boolean debug;
  public boolean hardcore;
  public boolean deleteVisitorInv;
  public boolean deleteInvDeactivate;
  public String language;
  public String languageHighlight;
  public String languageBase;
  public String languageNotice;

  /**
   * Ran when the plugin is being enabled (loading all files, setting
   * permissions).
   */
  @Override
  public void onEnable() {
    setupPermissions(); //connect to Vault (permission handler)

    this.saveDefaultConfig();  //create config if there isn't one
    pluginConfig = this.getConfig();  //load config data into a variable

    this.loadConfig(); //extract values from config
    //TODO: check if island size/spacing matches data in database.
    //      If not, tell the console and disable plugin to prevent island overwriting

    //load StringHandler
    out = new StringHandler(this);

    //check if all values in config are exists and won't cause issues later
    if (!checkConfig()) {
      //checkCofig also prints the mistake and tells the user how to fix it
      this.print("admin.config.general", false, "warning");

      //disable the plugin
      Bukkit.getPluginManager().disablePlugin(this);
      //return (so the plugin doesn't enable again)
      //TODO: maybe find a better way to do this...
      return;
    }

    //write the config to disk
    //not really sure how this works
    //TODO: check javadoc
    this.getConfig().options().copyDefaults(true);
    this.getConfig().options().copyHeader(true);
    this.saveConfig();

    //prepare the database
    database = new HuskyMySQL(this, mysqlIp, mysqlPort, mysqlDatabase, mysqlUser, mysqlPass);

    //connect to it
    Connection c = null;
    this.print("admin.sql.connecting", true, "info");
    c = database.openConnection(); //the connection will be closed on the end of onDisble()
    //this might be changed later, so that database is connected on command
    //TODO: find out which database approach is recommended

    //disable plugin if database wasn't connected
    if (!database.checkConnection()) {
      this.print("admin.sql.fail", false, "severe");
      Bukkit.getPluginManager().disablePlugin(this);
      return;
    }

    //TODO: check if the skyblock tables exists
    //create it if new
    /* THIS CODE DOESN'T WORK - NEVER FINDS THE TABLE THAT EXISTS
     DatabaseMetaData dbm;
     String tableName = mysqlPrefix + "_islands".toUpperCase();
    
     try {
     dbm = c.getMetaData();
     ResultSet tables = dbm.getTables(null, null, tableName, null);
     if (tables.next()) {
     //if this code is fixed, add this debug info to StringHandler and replace debug with this.print();
     debug("table exists", "info");
     }
     else {
     //same here
     debug("table doesnt exist", "info");
     }
     }
     catch (SQLException ex) {
     ex.printStackTrace();
     }*/

    //load the skyworld
    this.print("admin.world.loading", true, "info");
    /*
    MultiverseCore mvc = this.getMvCore();
    //if Multiverse doesnt exist, load the world normally
    if(mvc == null){
      skyworld = this.getServer().getWorld(worldName);
    }
    else{
      WorldManager wm = new WorldManager(mvc);
      MultiverseWorld world = wm.getMVWorld("world_sgames");
    }*/
      skyworld = this.getServer().getWorld(worldName);
    //if the world wasn't loaded
    if (skyworld == null) {
      this.print("admin.world.fail", false, "severe", skyworld.getName());
      Bukkit.getPluginManager().disablePlugin(this);  //disable the plugin
      return;  //return (so the plugin doesn't enable again)
    }

    //make a small center ramp, so a spawn island can be built
    //also, players are teleported here when deactivating your island
    if (generateSpawn) {
      if (skyworld.getBlockAt(0, islandY - 1, 0).getType() == Material.AIR) {
        for (int i = -1; i < 2; i++) {
          for (int j = -1; j < 2; j++) {
            Block blockToSet = skyworld.getBlockAt(i, islandY, j);
            blockToSet.setType(Material.STONE);
          }
        }
      }
    }

    //check if a region is around the middle island (create one and protect it)
    if (generateRegion) {
      RegionTools rTools = new RegionTools(this);
      RegionManager rm = rTools.getWorldGuard().getRegionManager(skyworld);
      if (!(rm.hasRegion("skyspawn"))) {
        this.print("admin.spawn.create", true, "info");
        //make a region, and add it
        rm.addRegion(rTools.makeRegion("SkySpawn", 0, 0));
        try {
          rm.save();
        }
        catch (ProtectionDatabaseException ex) {
          ex.printStackTrace();
        }
      }
    }

    //Register commands
    getCommand("sbtest").setExecutor(new Sky2(this));
    getCommand("sbadmin").setExecutor(new SkyAdmin(this));

    //getServer().getPluginManager().registerEvents(this, this);
    //new PlayerDeath(this);
    getServer().getPluginManager().registerEvents(new PlayerDeath(this), this);

    PluginDescriptionFile pdfFile = this.getDescription();
    this.getLogger().info(pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!");
  }

  /**
   * Ran when the plugin is being disabled (saving all files).
   */
  @Override
  public void onDisable() {
    //save out IslandData and party to database
    if (database != null) {
      database.closeConnection();
    }
    PluginDescriptionFile pdfFile = this.getDescription();
    this.getLogger().info(pdfFile.getName() + " version " + pdfFile.getVersion() + " is now Disabled!");
  }

  /**
   * Checks if the player has an island in playerIslands.bin
   *
   * @param player the player to look for
   * @return true if player's island is in the file
   */
  public boolean hasIsland(final Player player) throws SQLException {
    String sql = "SELECT `id` FROM " + this.mysqlPrefix + "_islands WHERE LOWER(nick) = LOWER('" + player.getName() + "') LIMIT 1;";
    ResultSet res = this.database.querySQL(sql);
    if (res.next()) {
      return true;
    }
    return false;
  }

  public int[] getEmptyIsland() throws SQLException {
    //select the last line in the database (the last generated island)
    String sql = "SELECT id, x, z FROM " + this.mysqlPrefix + "_islands WHERE active = 0 ORDER BY id ASC LIMIT 1;";
    ResultSet res = this.database.querySQL(sql);
    res.last();
    int[] result = new int[3];
    if (res.getRow() == 1) {
      result[0] = res.getInt("id");
      result[1] = res.getInt("x");
      result[2] = res.getInt("z");
    }
    else {
      result[0] = 0;
      result[1] = 0;
      result[2] = 0;
    }
    return result;
  }

  public int getISLANDS_Y() {
    return pluginConfig.getInt(CONF_ISLAND_Y);
  }

  public int[] getLastIsland() throws SQLException {
    String sql = "SELECT id, x, z FROM " + this.mysqlPrefix + "_islands ORDER BY id DESC LIMIT 1;";
    ResultSet res = this.database.querySQL(sql);
    int[] result = new int[2];
    res.last(); //this line has to be here
    if (res.getRow() == 1) {
      result[0] = res.getInt("x");
      result[1] = res.getInt("z");
    }
    else {
      result[0] = 0;
      result[1] = 0;
    }
    return result;
  }

  /**
   *
   * @return the number of blocks between two island centers (spacing + size)
   */
  public int getISLAND_SPACING() {
    return (pluginConfig.getInt(CONF_ISLAND_SPACING) + pluginConfig.getInt(CONF_ISLAND_SIZE));
  }

  public void skyTp(int x, int z, Player player) {
    //maybe use this?
    //https://github.com/essentials/Essentials/blob/master/Essentials/src/net/ess3/utils/LocationUtil.java
    int h = pluginConfig.getInt(CONF_ISLAND_Y);

    //check until two safe blocks (air) are found
    while (skyworld.getBlockAt(x, h, z).getType() != Material.AIR || skyworld.getBlockAt(x, h + 1, z).getType() != Material.AIR) {
      /* TODO:
       * add other safe blocks to port to (buttons, signs, ladders...) 
       * add dangerous blocks, never teleport on them (lava, cobweb...)
       * perhaps use essentials safe tp?
       */
      h++;
    }

    skyworld.loadChunk(x, z);
    /* This seems to not be needed in the newer bukkit (1-7-2)
     * if (player.getWorld() != this.skyworld) {//teleport twice if teleporting from another world (otherwise it teleports unsafely)
     *   player.teleport(new Location(skyworld, x, h, z));
     * }
     */

    //add 0.5 to x and z, so the player gets teleported in the middle of a block
    //do h - 0.5 so the player stands on the ground and doesn't levitate (his head might gt stuck in something)
    player.teleport(new Location(skyworld, (x + 0.5), (h), (z + 0.5)));
  }

  /**
   * Gets data about a player island from the database.
   *
   * @param player the owner of the island
   * @return Island object. If there is no such island in database, return null.
   * @throws SQLException
   *//*
  public Island getPlayerData(Player player) throws SQLException {

    String sql = "SELECT id, x, z, nick, date, active FROM " + this.mysqlPrefix + "_islands WHERE LOWER(nick) = LOWER('" + player.getName() + "') LIMIT 1";
    ResultSet rs = database.querySQL(sql);

    if (rs.next()) {
      return loadIslandData(rs);
    }
    else {
      return null;
    }
  }*/
/*
  public Island loadIslandData(ResultSet rs) throws SQLException {
    Island island = new Island(this);
    island.ownerNick = rs.getString("nick");
    island.x = rs.getInt("x");
    island.z = rs.getInt("z");
    island.id = rs.getInt("id");
    island.date = rs.getLong("date");
    island.active = rs.getBoolean("active");
    return island;
  }*/

  public void addPerk(Player player, String perk) {
    perms.playerAdd((String) null, player.getName(), perk);
  }

  public void removePerk(Player player, String perk) {
    perms.playerRemove((String) null, player.getName(), perk);
  }

  public void addGroup(Player player, String perk) {
    perms.playerAddGroup((String) null, player.getName(), perk);
  }

  public boolean checkPerk(Player player, String perk) {
    if (perms.has((String) null, player.getName(), perk)) {
      return true;
    }
    else {
      return false;
    }
  }

  public boolean checkGroup(String player, String perk) {
    if (perms.playerInGroup((String) null, player, perk)) {
      return true;
    }
    else {
      return false;
    }
  }

  private boolean setupPermissions() {
    RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
    perms = rsp.getProvider();
    return perms != null;
  }

  public void clearInventory(Player player) {
    player.getInventory().clear();
    player.getInventory().setHelmet(null);
    player.getInventory().setChestplate(null);
    player.getInventory().setLeggings(null);
    player.getInventory().setBoots(null);
    /*
    List<Entity> Entities = player.getNearbyEntities(15, 15, 15);
    //15 blocks is enough, because any other blocks will fall to Void
    Iterator<Entity> ent = Entities.iterator();
    while (ent.hasNext()) {
      ent.next().remove();
    }*/
  }

  public void displayHelp(Player player) {
    player.sendMessage(out.format("plugin.headline", out.get("plugin.help.name")));
    player.sendMessage(out.get("plugin.help.content"));
  }

  public void debug(String[] strings, String type) {
    if (this.debug) {
      String result = "";
      for (int i = 0; i < strings.length; i++) {
        result += strings[i];
        System.out.println(result);
      }
      switch (type) {
        case "severe":
          this.getLogger().severe("[DEBUG] " + result);
          break;
        case "warning":
          this.getLogger().warning("[DEBUG] " + result);
          break;
        case "info":
        default:
          this.getLogger().info("[DEBUG] " + result);
          break;
      }
    }
  }

  /**
   * Prints the message specified by the key
   *
   * @param key Key to the output string in StringHandler yaml
   * @param isDebug when set to true, messages will be shown only if the debug
   * option is on
   * @param type log level (info, warning, severe etc...)
   */
  public void print(String key, boolean isDebug, String type) {
    //load strings to one string
    String message = out.get(key);
    //if this is a debug information
    if (isDebug) {
      //if debug is on
      if (this.debug) {
        //print lines with "[DEBUG]"
        this.debug(message, type);
      }
    }
    else {
      //print lines
      this.out(message, type);
    }
  }

  /**
   * Prints the message specified by the key with parameters
   *
   * @param key Key to the output string in StringHandler yaml
   * @param isDebug when set to true, messages will be shown only if the debug
   * option is on
   * @param type log level (info, warning, severe etc...)
   */
  public void print(String key, boolean isDebug, String type, Object... args) {
    //load strings to one string
    String message = out.format(key, args);
    //if this is a debug information
    if (isDebug) {
      //if debug is on
      if (this.debug) {
        //print lines with "[DEBUG]"
        this.debug(message, type);
      }
    }
    else {
      //print lines
      this.out(message, type);
    }
  }

  public void out(String message, String type) {
    switch (type) {
      case "severe":
        this.getLogger().severe(message);
        break;
      case "warning":
        this.getLogger().warning(message);
        break;
      case "info":
      default:
        this.getLogger().info(message);
        break;
    }
  }

  public void debug(String message, String type) {
    if (this.debug) {
      switch (type) {
        case "severe":
          this.getLogger().severe("[DEBUG] " + message);
          break;
        case "warning":
          this.getLogger().warning("[DEBUG] " + message);
          break;
        case "info":
        default:
          this.getLogger().info("[DEBUG] " + message);
          break;
      }
    }
  }

  void loadConfig() {
    //loading all variables
    islandY = pluginConfig.getInt(CONF_ISLAND_Y);
    islandSize = pluginConfig.getInt(CONF_ISLAND_SIZE);
    islandSpacing = pluginConfig.getInt(CONF_ISLAND_SPACING);
    resetCooldown = pluginConfig.getInt(CONF_RESET_COOLDOWN);
    generateSpawn = pluginConfig.getBoolean(CONF_GENERATE_SPAWN);
    generateRegion = pluginConfig.getBoolean(CONF_GENERATE_REGION);
    worldName = pluginConfig.getString(CONF_WORLD);
    spawnX = pluginConfig.getInt(CONF_SPAWN_X);
    spawnZ = pluginConfig.getInt(CONF_SPAWN_Z);
    mysqlIp = pluginConfig.getString(CONF_MYSQL_IP);
    mysqlPort = pluginConfig.getString(CONF_MYSQL_PORT);
    mysqlDatabase = pluginConfig.getString(CONF_MYSQL_DATABASE);
    mysqlUser = pluginConfig.getString(CONF_MYSQL_USER);
    mysqlPass = pluginConfig.getString(CONF_MYSQL_PASS);
    mysqlPrefix = pluginConfig.getString(CONF_MYSQL_PREFIX);
    debug = pluginConfig.getBoolean(CONF_DEBUG);
    hardcore = pluginConfig.getBoolean(CONF_HARDCORE);
    deleteVisitorInv = pluginConfig.getBoolean(CONF_DELETE_VISITOR_INV);
    deleteInvDeactivate = pluginConfig.getBoolean(CONF_DELETE_INV_DEACTIVATE);
    language = pluginConfig.getString(CONF_LANGUAGE);
    languageBase = pluginConfig.getString(CONF_LANGUAGE_BASE);
    languageHighlight = pluginConfig.getString(CONF_LANGUAGE_HIGHLIGHT);
    languageNotice = pluginConfig.getString(CONF_LANGUAGE_NOTICE);


    /* debug("Loading config", "info");
     * debug("island-height: " + islandY, "info");
     * debug("island-size: " + islandSize, "info");
     * debug("island-spacing: " + islandSpacing, "info");
     * debug("mysql-ip: " + mysqlIp, "info");
     * debug("mysql-port: " + mysqlPort, "info");
     * debug("mysql-database: " + mysqlDatabase, "info");
     * debug("mysql-user: " + mysqlUser, "info");
     * debug("mysql-prefix: " + mysqlPrefix, "info");
     * debug("mysql-pass: " + mysqlPass, "info");
     * debug("generate-spawn-island: " + generateSpawn, "info");
     * debug("spawn-x: " + spawnX, "info");
     * debug("spawn-z: " + spawnZ, "info");
     */
  }

  boolean checkConfig() {
    //TODO check if mysql ip is exists
    boolean configOk = true;
    if (mysqlIp.equals("")) {
      this.print("admin.config.sql.ip", false, "severe");
      configOk = false;
    }
    if (mysqlPort.equals("")) {
      this.print("admin.config.sql.port", false, "severe");
      configOk = false;
    }
    if (mysqlDatabase.equals("")) {
      this.print("admin.config.sql.database", false, "severe");
      configOk = false;
    }
    if (mysqlUser.equals("")) {
      this.print("admin.config.sql.user", false, "severe");
      configOk = false;
    }
    if (mysqlPass.equals("")) {
      this.print("admin.config.sql.password", false, "severe");
      configOk = false;
    }
    if (islandY < 0 || islandY > 250) {
      this.print("admin.config.island.height", false, "severe");
      configOk = false;
    }
    if (islandSize < 0 || (islandSize % 2 == 1)) {
      this.print("admin.config.island.size", false, "severe");
      configOk = false;
    }
    if (islandSpacing < 0) {
      this.print("admin.config.island.spacing", false, "severe");
      configOk = false;
    }

    //TODO: Add other checks
    return configOk;
  }

  public String getMysqlPrefix() {
    return this.mysqlPrefix;
  }

  /**
   * Retunrs the instance of MultiverseCore (if there is no such plugin on the server, null is returned)
   * @return MultiverseCore or null
   */
  public MultiverseCore getMvCore() {
    Plugin plugin = this.getServer().getPluginManager().getPlugin("Multiverse-Core");

    if (plugin instanceof MultiverseCore) {
      return (MultiverseCore) plugin;
    }

    else{
      return null;
    }
  }
}