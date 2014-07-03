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
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.managers.RegionManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

public class SimpleSkyblock extends JavaPlugin {
  //drop protect má otevřený kód, zkontrolovat

  public static Permission perms = null;
  private FileConfiguration pluginConfig;
  public HuskyMySQL database;
  public HashMap<String, Island> playerIslands = new HashMap<>();
  public StringHandler out;
  public boolean checkOK = true;
  public String checkReason = "";
  //
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
  private static final String CONF_DELETE_XP = "delete-xp";
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
  public boolean deleteXP;
  public String language;
  public String languageHighlight;
  public String languageBase;
  public String languageNotice;
  public Sky2 skyCommand;

  /**
   * Ran when the plugin is being enabled (loading all files, setting
   * permissions).
   */
  @Override
  public void onEnable() {
    setupPermissions(); //connect to Vault (permission handler)

    //register commands
    skyCommand = new Sky2(this);
    getCommand("sb").setExecutor(skyCommand);
    getCommand("sbadmin").setExecutor(new SkyAdmin(this));

    //getServer().getPluginManager().registerEvents(this, this);
    //new PlayerDeath(this);
    getServer().getPluginManager().registerEvents(new PlayerDeath(this), this);

    this.saveDefaultConfig();  //create config if there isn't one
    pluginConfig = this.getConfig();  //load config data into a variable

    this.loadConfig(); //extract values from config

    //load StringHandler
    out = new StringHandler(this);

    this.playerIslands = new HashMap<>();

    //check if all values in config exists and won't cause issues later
    if (!checkConfig()) {
      //checkCofig also prints the mistake and tells the user how to fix it
      this.write(null, "admin.config.general", "warning");
      this.checkOK = false;
      this.checkReason += "admin.config.general";
      return; //stop loading the plugin
    }
    this.write(null, "admin.config.ok", "debug");

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
    this.write(null, "admin.sql.connecting", "debug");
    c = database.openConnection();

    //disable plugin if database wasn't connected
    if (!database.checkConnection()) {
      this.write(null, "admin.sql.fail", "severe");
      this.checkOK = false;
      this.checkReason += "admin.sql.fail";
      return; //stop loading the plugin
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
    this.write(null, "admin.world.loading", "info");
    skyworld = this.getServer().getWorld(worldName);

    //if the world wasn't loaded
    if (skyworld == null) {
      this.write(null, "admin.world.fail", "severe", worldName);
      this.checkOK = false;
      this.checkReason += "admin.world.fail";
      return; //stop loading the plugin
    }

    //make a small center ramp, so a spawn island can be built
    //also, players are teleported here when deactivating your island
    if (generateSpawn) {
      if (skyworld.getBlockAt(0, islandY - 1, 0).getType() == Material.AIR) {
        this.write(null, "admin.spawn.blocks", "debug");
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
        this.write(null, "admin.spawn.region", "debug");
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

    //getServer().getPluginManager().registerEvents(this, this);
    //new PlayerDeath(this);
    getServer().getPluginManager().registerEvents(new PlayerDeath(this), this);

    this.write(null, "admin.enabled", "info");

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
    this.write(null, "admin.disabled", "info");
  }

  /**
   * Returns the id and coordinates of a deactivated island - in case no
   * deactivated isalnds are found, id 0 is returned.
   *
   * @return {id, x, z}
   * @throws SQLException
   */
  public int[] getEmptyIsland() throws SQLException {
    //select the first deactivated island in the database
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
    return this.islandY;
  }

  public Coordinates getLastIsland() throws SQLException {
    String sql = "SELECT id, x, z FROM " + this.mysqlPrefix + "_islands ORDER BY id DESC LIMIT 1;";
    ResultSet res = this.database.querySQL(sql);

    res.next();
    if (res.getRow() == 1) {
      return new Coordinates(res.getInt("x"), res.getInt("z"));
    }
    return new Coordinates(this.spawnX, this.spawnZ);
    /*
     res.last(); //this line has to be here
     Coordinates result;
     if (res.getRow() == 1) {
     return new Coordinates(res.getInt("x"), res.getInt("z"));
     }
     return new Coordinates(res.getInt(this.spawnX), res.getInt(this.spawnZ));
     */
  }

  /**
   *
   * @return the number of blocks between two island centers (spacing + size)
   */
  public int getISLAND_SPACING() {
    return (this.islandSpacing + this.islandSize);
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

    //add 0.5 to x and z, so the player gets teleported in the middle of a block
    //do h - 0.5 so the player stands on the ground and doesn't levitate (his head might gt stuck in something)
    player.teleport(new Location(skyworld, (x + 0.5), (h), (z + 0.5)));
  }

  public void addPerk(Player player, String perk) {
    perms.playerAdd((String) null, player.getName(), perk);
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
    if (this.deleteXP) {
      player.setExp(0);
      player.setTotalExperience(0);
    }
  }

  public void displayHelp(Player player) {
    player.sendMessage(out.format("plugin.headline", out.get("plugin.help.name")));
    player.sendMessage(out.get("plugin.help.content"));
  }

  private void out(String message, String type) {
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

  private void debug(String message) {
    if (this.debug) {
      long microunix = System.currentTimeMillis();
      this.getLogger().info("[DEBUG] [t" + microunix + "] " + message);
    }
  }

  /**
   * Prints 
   * 
   * @param sender the player to send the message to (use null to send to console)
   * @param key message key from StringHandler
   * @param type debug, info, warn or severe (default is info)
   * @param args args to fill to the key string in case there are any
   */
  public void write(CommandSender sender, String key, String type, Object... args) {
    String output = this.out.format(key, args);
    //console output
    if (!(sender instanceof Player)) {
      //debug message
      if(type.equals("debug")){
        this.debug(output);
      }
      //normal message
      else{
        this.out(output, type);
      }
    }
    //player output
    else{
      Player player = (Player) sender;
      player.sendMessage(output);
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
    deleteXP = pluginConfig.getBoolean(CONF_DELETE_XP);
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
      this.write(null, "admin.config.sql.ip", "severe");
      configOk = false;
    }
    if (mysqlPort.equals("")) {
      this.write(null, "admin.config.sql.port", "severe");
      configOk = false;
    }
    if (mysqlDatabase.equals("")) {
      this.write(null, "admin.config.sql.database", "severe");
      configOk = false;
    }
    if (mysqlUser.equals("")) {
      this.write(null, "admin.config.sql.user", "severe");
      configOk = false;
    }
    if (mysqlPass.equals("")) {
      this.write(null, "admin.config.sql.password", "severe");
      configOk = false;
    }
    if (islandY < 0 || islandY > 250) {
      this.write(null, "admin.config.island.height", "severe");
      configOk = false;
    }
    if (islandSize < 0 || (islandSize % 2 == 1)) {
      this.write(null, "admin.config.island.size", "severe");
      configOk = false;
    }
    if (islandSpacing < 0) {
      this.write(null, "admin.config.island.spacing", "severe");
      configOk = false;
    }

    //TODO: check if island size/spacing matches data in database.
    //      If not, tell the console and disable plugin to prevent island overwriting
    return configOk;
  }

  public String getMysqlPrefix() {
    return this.mysqlPrefix;
  }

  /**
   * Retunrs the instance of MultiverseCore (if there is no such plugin on the
   * server, null is returned)
   *
   * @return MultiverseCore or null
   */
  public MultiverseCore getMvCore() {
    Plugin plugin = this.getServer().getPluginManager().getPlugin("Multiverse-Core");

    if (plugin instanceof MultiverseCore) {
      return (MultiverseCore) plugin;
    }
    else {
      return null;
    }
  }
}