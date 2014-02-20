package eu.mcminers.drali;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 *
 * @author MTN
 */
public class StringHandler {

  private static YamlConfiguration yml;
  private static YamlConfiguration defaultYml;
  private static String languageCode;
  private static File languageFile;
  private static HashMap<String, Object> defaultStrings = new HashMap<String, Object>();
  private final SimpleSkyblock plugin;

  public StringHandler(SimpleSkyblock plugin) {
    plugin.getLogger().info("Starting StringHandler");
    this.plugin = plugin;
    File languageFolder = new File(plugin.getDataFolder() + File.separator + "language");
    File defaultFile = new File(plugin.getDataFolder() + File.separator + "language" + File.separator + "language_default.yml");
    yml = new YamlConfiguration();
    defaultYml = new YamlConfiguration();

    //create the language file
    if (!languageFolder.exists()){
      languageFolder.mkdir();
    }
    
    try {
      //delete the old file
      if(defaultFile.exists()){
        defaultFile.delete();
      }
      //create a new one
      defaultFile.createNewFile();
      plugin.debug("Generating language file", "info");
    }
    catch (IOException e3) {
      //this is not an issue, since this occurs on first startup only and is solved by itself
      //TODO: I should maybe solve this later...
      e3.printStackTrace();
    }


    try {
      defaultYml.load(defaultFile);
    }
    catch (FileNotFoundException e2) {
      plugin.getLogger().info("Oh no (2)!");
      // TODO Auto-generated catch block
      e2.printStackTrace();
    }
    catch (IOException e2) {
      // TODO Auto-generated catch block
      e2.printStackTrace();
    }
    catch (InvalidConfigurationException e2) {
      // TODO Auto-generated catch block
      e2.printStackTrace();
    }
    defaultYml.options().copyDefaults(true);
    yml.options().copyDefaults(true);

    setDefaults();

    yml.addDefaults(defaultStrings);
    defaultYml.addDefaults(defaultStrings);

    languageCode = plugin.language;

    languageFile = new File(plugin.getDataFolder() + File.separator + "language" + File.separator + "language_" + languageCode + ".yml");

    if (!languageFile.exists()) {
      plugin.getLogger().info("Custom language " + languageCode + " was not found, switching to default!");
      languageCode = "default";
      languageFile = defaultFile;
    }

    try {
      yml.load(languageFile);
      plugin.getLogger().info(
              "Language language_" + languageCode + ".yml loaded!");
    }
    catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    catch (IOException | InvalidConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    try {
      yml.save(defaultFile);
    }
    catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
  }

  public String[] loadList(String key) {

    List<String> list = yml.getStringList(key);
    int i = 0;
    String output[] = new String[list.size()];

    for (String line : list) {
      output[i] = replaceChatColor(line);
      i++;
    }

    return output;
  }

  /**
   * Joins the input array into one String with \n - so output methods can work
   * with it.
   *
   * @param strings the array to work with
   * @return one string
   */
  public String shorter(String[] strings) {
    String result = "";
    for (int i = 0; i < strings.length; i++) {
      result += strings[i] + "\n";
    }
    return result;
  }

  /**
   * get the string for a key
   *
   * @param key the key in the language file you are referring to
   * @return
   */
  public String get(String key) {
    if (!yml.contains(key)) {
      plugin.getLogger().warning("Missing language key '" + key + "', inform the developer");
    }
    String output;
    if (yml.isList(key)) {
      String[] outList;
      outList = this.loadList(key);
      output = this.shorter(outList);
    }
    else {
      output = yml.getString(key);
    }
    return this.replaceChatColor(output);
  }

  /**
   * will replace %s to strings given in arguments in string lists loaded from
   * the language file, see String.format(). Never use "<$separator$>" in
   * strings!
   *
   * @param messageKey path to the string in the language file
   * @param args
   * @return
   */
  public String format(String messageKey, Object... args) {
    String messages = get(messageKey);
    String formattedMessage = String.format(messages, args);
    return this.replaceChatColor(formattedMessage);
  }

  /**
   * replace color codes in strings, see
   * [url]http://www.minecraftwiki.net/wiki/Formatting_codes[/url] use
   * "&FORMATCODE" or use {BASE}/{HIGHLIGHT} or {NOTICE} for default colors
   *
   * @param s
   * @return
   */
  public String replaceChatColor(String s) {
    s = s.replace("&r", "&r{BASE}");
    s = s.replace("{BASE}", plugin.languageBase);
    s = s.replace("{HIGHLIGHT}", plugin.languageHighlight);
    s = s.replace("{NOTICE}", plugin.languageNotice);

    s = ChatColor.translateAlternateColorCodes('&', s);
    return s;
  }

  private static void setDefaults() {
    // strings which allow more than one line will be a list
    defaultStrings.put("admin.config.general", "Check and fix your config.yml to load SimpleSkyblock succesfully");
    defaultStrings.put("admin.config.loading", "Loading config...");
    defaultStrings.put("admin.config.sql.ip", "MySQL IP not set in config.");
    defaultStrings.put("admin.config.sql.port", "MySQL port not set in config.");
    defaultStrings.put("admin.config.sql.database", "MySQL database not set in config.");
    defaultStrings.put("admin.config.sql.user", "MySQL user not set in config.");
    defaultStrings.put("admin.config.sql.password", "MySQL password not set in config.message");
    defaultStrings.put("admin.config.island.height", "Island height has unreachable value. Choose a number between 0 and 250");
    defaultStrings.put("admin.config.island.size", "Island size can not be smaller than 0 and it has to be divisible by 2");
    defaultStrings.put("admin.config.island.spacing", "Island spacing can not be smaller than 0");
    defaultStrings.put("admin.sql.connecting", "Connecting to database...");
    defaultStrings.put("admin.sql.connected", "Database connected...");
    defaultStrings.put("admin.sql.ex", "Could not connect to MySQL server! Reason: %s");
    defaultStrings.put("admin.sql.exx", "JDBC Driver not found!");
    defaultStrings.put("admin.sql.fail", "MySQL connection couldn't be made.");
    defaultStrings.put("admin.sql.disconnected", "Database was disconnected");
    defaultStrings.put("admin.sql.disconnectedfail", "Error when disconnecting database");
    defaultStrings.put("admin.sql.query", "SQL: %s");
    defaultStrings.put("admin.sql.queryrows", "SQL: %s");
    defaultStrings.put("admin.world.loading", "Loading skyworld...");
    defaultStrings.put("admin.world.fail", "The world \"%s\" doesn't exist.");
    defaultStrings.put("admin.spawn.create", "Center region not found, creating one for you!");
    defaultStrings.put("admin.noconsole", "Only players can use this command");

    defaultStrings.put("plugin.headline", "   &c-&e-&c-&e-&c-&e- &c[&e&o%s&c] &e-&c-&e-&c-&e-&c-&e");
    defaultStrings.put("plugin.help.name", "Skyblock Help");
    defaultStrings.put("plugin.help.content",
            Arrays.asList("Skyblock is a plugin that gives each player an island to play on",
            "All islands are in protected region",
            "Command usage:",
            "/sky help: displays this help",
            "/sky new: generates a new island"));
    defaultStrings.put("plugin.region.enter", "You are entering a protected island area. (%s)");
    defaultStrings.put("plugin.region.leave", "You are leaving a protected island area. (%s)");
    defaultStrings.put("plugin.region.saving", "Saving regions...");
    defaultStrings.put("plugin.region.deleting", "Deleting region: %s");


    defaultStrings.put("command.error.noparameters", "No parameters given");
    defaultStrings.put("command.error.unknown", "Unknown command. Use /sb help to see the help");
    defaultStrings.put("command.error.unknownparameter", "Parameter not found");
    defaultStrings.put("command.error.noisland", "You don't have an island. To create one, use /sb new");

    defaultStrings.put("command.tphome.noisland", "You don't have an island");
    defaultStrings.put("command.tphome.inactive", "Your island is inactive now. Activate it with /sb active");
    defaultStrings.put("command.tphome.teleporting", "Teleporting to your island");
    defaultStrings.put("command.tphome.perms", "You don't have permission to teleport to your island.");

    defaultStrings.put("command.tpfriend.noisland", "The island you are trying to teleport to doesn't exist.");
    defaultStrings.put("command.tpfriend.invalidnick", "Invalid nick!");
    defaultStrings.put("command.tpfriend.inactive", "Sorry, the island you are trying to teleport to is inactive now.");
    defaultStrings.put("command.tpfriend.teleporting", "Teleporting to %s's island");
    defaultStrings.put("command.tpfriend.denied", "You aren't a friend of %s, can't teleport you to his island.");
    defaultStrings.put("command.tpfriend.perms", "You don't have permission to teleport to other islands.");

    defaultStrings.put("command.info.noisland", "You don't have an island");
    defaultStrings.put("command.info.out",
            Arrays.asList("Island owner: %s",
            "Island ID: %s",
            "Coordinates: [%s,%s]",
            "Active: %s"));
    defaultStrings.put("command.info.perms", "You don't have permission to view your island information.");

    defaultStrings.put("command.new.exists", "You already have an island! Use /sb home to get there.");
    defaultStrings.put("command.new.starting", "Preparing your island...");
    defaultStrings.put("command.new.finished", "You were teleported to your new island!");
    defaultStrings.put("command.new.perms", "You don't have permission to create a new island.");

    defaultStrings.put("command.reset.noisland", "You don't have an island! To create it, use /sb new.");
    defaultStrings.put("command.reset.inactive", "Your island is inactive. Use /sb active to activate it.");
    defaultStrings.put("command.reset.starting", "Reseting your island...");
    defaultStrings.put("command.reset.finished", "You were teleported to your new island!");
    defaultStrings.put("command.reset.perms", "You don't have permission to reset your island.");

    defaultStrings.put("command.delete.noisland", "You don't have an island! To create it, use /sb new.");
    defaultStrings.put("command.delete.inactive", "Your island is already inactive. Use /sb active to activate it.");
    defaultStrings.put("command.delete.starting", "Reseting your island...");
    defaultStrings.put("command.delete.finished", "Your island is now inactive. Type /sb active to re-activate.");
    defaultStrings.put("command.delete.perms", "You don't have permission to delete your island.");

    defaultStrings.put("command.active.noisland", "You don't have an island! To create it, use /sb new.");
    defaultStrings.put("command.active.active", "Your island is already active. Use /sb home to get there");
    defaultStrings.put("command.active.finished", "Your island was now activated");
    defaultStrings.put("command.active.perms", "You don't have permission to activate your island.");

    defaultStrings.put("command.addfriend.nonick", "No nick found! To add a friend to your island, use /sb friend add <nick>.");
    defaultStrings.put("command.addfriend.invalidnick", "Invalid nick!");
    defaultStrings.put("command.addfriend.self", "You can't add yourself to your island!");
    defaultStrings.put("command.addfriend.starting", "Adding %s as your friend!");
    defaultStrings.put("command.addfriend.alreadyadded", "%s is your friend already!");
    defaultStrings.put("command.addfriend.perms", "You don't have permission to add players as your friends");
    defaultStrings.put("command.addfriend.friend", "You are now a friend of %s");

    defaultStrings.put("command.removefriend.nonick", "No nick found! To remove someone from your island, use /sb friend remove <nick>.");
    defaultStrings.put("command.removefriend.invalidnick", "Invalid nick!");
    defaultStrings.put("command.removefriend.self", "You can't remove yourself from your island!");
    defaultStrings.put("command.removefriend.starting", "Remove %s from your friends!");
    defaultStrings.put("command.removefriend.alreadyremoved", "%s isn't your friend!");
    defaultStrings.put("command.removefriend.perms", "You don't have permission to remove players as your friends");
    defaultStrings.put("command.removefriend.nolongerfriend", "You are no longer a friend of %s");

    defaultStrings.put("command.listfriend.own", "Following players have permission on you island:");
    defaultStrings.put("command.listfriend.nobodyown", "Nobody has permission on your island");
    defaultStrings.put("command.listfriend.other", "You have permission on following islands: ");
    defaultStrings.put("command.listfriend.nobodyother", "You don't have permission on any island");
    defaultStrings.put("command.listfriend.perms", "You don't have permission to list your friends");
    
    defaultStrings.put("command.clearfriends.perms", "You don't have permission to list your friends");
    defaultStrings.put("command.clearfriends.one", "One friend removed");
    defaultStrings.put("command.clearfriends.zero", "You had no friends");
    defaultStrings.put("command.clearfriends.twothree", "%s friends removed");
    defaultStrings.put("command.clearfriends.many", "%s friends removed");
    
    
    defaultStrings.put("exception.SQL", "Ooops, something went wrong with the database. Please contact server administrator.");
    defaultStrings.put("welcome", "%s, welcome to Skyblock!");
    /*
     defaultStrings.put("node", "message");
     defaultStrings.put("node", "message");
     defaultStrings.put("node", "message");
     defaultStrings.put("node", "message");
     defaultStrings.put("node", "message");
     defaultStrings.put("node", "message");
     defaultStrings.put("node", "message");
     defaultStrings.put("node", "message");
     defaultStrings.put("node", "message");
     defaultStrings.put("node", "message");
     defaultStrings.put("node", "message");
     defaultStrings.put("node", "message");
     defaultStrings.put("node", "message");
     defaultStrings.put("node", "message");
     defaultStrings.put("node", "message");
     defaultStrings.put("node", "message");
     defaultStrings.put("node", "message");
     */

    /*
     /**
     * get the string (array) for a key
     *
     * @param key the key in the language file you are referring to
     * @return
     *//*
     public String[] get(String key) {
     return get(key, true);
     }

     /**
     * get the string (array) for a key
     *
     * @param key the key in the language file you are referring to
     * @param setBaseColor should {BASE} be added as a prefix?
     * @return
     *//*
     public String[] get(String key, boolean setBaseColor) {
     if (!yml.contains(key)) {
     plugin.getLogger().warning("Missing language key '" + key + "', inform the developer");
     }
     String[] output;
     if (yml.isList(key)) {
     output = this.loadList(key);
     /*
     List<String> list = yml.getStringList(key);
     int i = 0;
     String output[] = new String[list.size()];

     String base = "";
     if (setBaseColor) {
     base = "{BASE}";
     }

     for (String line : list) {
     output[i] = replaceChatColor(base + line);
     i++;
     }*//*
     }
     else {
     output = new String[1];
     output[0] = replaceChatColor(yml.getString(key));
     }

     /*
     for (int j = 0; j < list.size(); j++) {
     output[j] = replaceChatColor(base + list.get(j));
     plugin.debug(list.get(j), "info");
     }*/
    /*
     for (String line : list) {
     output = replaceChatColor(base + line); // replace current line with colored line
     i++;
     }*//*
     return output;
     }*/
    /*
  
     /**
     * send a message from the language file to a player
     *
     * @param player
     * @param messageKey the key in the language file you are referring to
     */
    /*
     public void sendMessage(Player player, String messageKey) {
     player.sendMessage(get(messageKey));
     }
     /**
     * send a formated message from the language file, will replace e.g. %s with
     * the arg (String). Never use "<$separator$>" in strings, but who would...?
     *
     * @param player
     * @param messageKey
     * @param args arguments for String.format
     */
    /*
     public void sendMessage(Player player, String messageKey,
     Object... args) {
     player.sendMessage(format(messageKey, args));
     }

     /**
     * send a message from the language file to a Command Sender
     *
     * @param player
     * @param messageKey the key in the language file you are referring to
     */
    /*
     public String[] sendMessage(String messageKey) {
     //sender.sendMessage(get(messageKey));
     return get(messageKey);
     }

     /**
     * send a formated message from the language file to a CommandSender, will
     * replace e.g. %s with the arg (String). Never use "<$separator$>" in
     * strings, but who would...?
     *
     * @param player
     * @param messageKey
     * @param args arguments for String.format
     */
    /*
     public String[] sendMessage(String messageKey,
     Object... args) {
     return format(messageKey, args);
     }

     /*
     /**
     * send a message from the language file to a Command Sender
     *
     * @param player
     * @param messageKey the key in the language file you are referring to
     *//*
     public String[] sendMessage(CommandSender sender, String messageKey) {
     //sender.sendMessage(get(messageKey));
     return get(messageKey);
     }

     /**
     * send a formated message from the language file to a CommandSender, will
     * replace e.g. %s with the arg (String). Never use "<$separator$>" in
     * strings, but who would...?
     *
     * @param player
     * @param messageKey
     * @param args arguments for String.format
     *//*
     public void sendMessage(CommandSender sender, String messageKey,
     Object... args) {
     sender.sendMessage(format(messageKey, args));
     }  /**
     * will replace things like %s to strings given in arguments in string lists
     * loaded from the language file, see String.format(). Never use
     * "<$separator$>" in strings!
     *
     * @param messageKey path to the string in the language file
     * @param args
     * @return
     *//*
     public String[] format(String messageKey, Object... args) {
     String[] messages = get(messageKey);
     String mergedMessages = ""; // merge to be able to use format() without
     // replacing the same value more than one
     int i = 0;
     for (String message : messages) {
     if (i != 0) {
     mergedMessages += "<§separator§>"; // I know... nothing to be proud of
     }
     mergedMessages += replaceChatColor("{BASE}" + message);
     i++;
     }
     String formattedMessage = String.format(mergedMessages, args);
     String[] output = formattedMessage.split("<§separator§>");

     return output;
     }*/
    /*
     /**
     * get the string for a key as a list of strings
     *
     * @param key the key in the language file you are referring to
     * @return
     *//*
     public List<String> getList(String key) {
     String[] array = get(key);
     List<String> list = new ArrayList<String>();
     for (String string : array) {
     list.add(string);
     }
     return list;
     }*/
  }
}