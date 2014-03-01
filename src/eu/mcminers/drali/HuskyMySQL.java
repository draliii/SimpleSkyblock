package eu.mcminers.drali;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Connects to and uses a MySQL database
 *
 * @author -_Husky_-
 * @author tips48
 */
public class HuskyMySQL extends HuskyDatabase {

  private final String user;
  private final String database;
  private final String password;
  private final String port;
  private final String hostname;
  //private final SimpleSkyblock plugin;
  private Connection connection;

  /**
   * Creates a new MySQL instance
   *
   * @param plugin Plugin instance
   * @param hostname Name of the host
   * @param port Port number
   * @param database Database name
   * @param username Username
   * @param password Password
   */
  public HuskyMySQL(SimpleSkyblock plugin, String hostname, String port, String database, String username, String password) {
    super(plugin);
    this.plugin = plugin;
    this.hostname = hostname;
    this.port = port;
    this.database = database;
    this.user = username;
    this.password = password;
    this.connection = null;
  }

  @Override
  public Connection openConnection() {
    try {
      Class.forName("com.mysql.jdbc.Driver");
      DriverManager.setLoginTimeout(plugin.resetCooldown);
      //TODO: add a while cycle to try to connect to the database (in a separate thread)
      connection = DriverManager.getConnection("jdbc:mysql://" + this.hostname + ":" + this.port + "/" + this.database, this.user, this.password);

      plugin.print("admin.sql.connected", true, "info");
    }
    catch (SQLException e) {
      plugin.print("admin.sql.ex", false, "severe");
    }
    catch (ClassNotFoundException e) {
      plugin.print("admin.sql.exx", false, "severe");
    }
    return connection;
  }

  @Override
  public boolean checkConnection() {
    return connection != null;
  }

  @Override
  public Connection getConnection() {
    return connection;
  }

  @Override
  public void closeConnection() {
    if (connection != null) {
      try {
        connection.close();
        plugin.print("admin.sql.disconnected", true, "info");
      }
      catch (SQLException e) {
        plugin.print("admin.sql.disconnectedfail", false, "severe");
        //e.printStackTrace();
      }
    }
  }

  public ResultSet querySQL(String query) {
    /*
    //plugin.print("admin.sql.query", true, "info", query);
    Connection c = null;

    //plugin.debug("checking connection", "info");
    if (checkConnection()) {
      //plugin.debug("connection found", "info");
      c = getConnection();
      if (c == null) {
        //plugin.debug("connection is null, opening a new one", "info");
        c = openConnection();
      }
    }
    else {
      //plugin.debug("connection not found, opening a new one", "info");
      c = openConnection();
    }*/

    Statement s = null;
    ResultSet ret = null;

    try {
      s = openConnection().createStatement();
      ret = s.executeQuery(query);
    }
    catch (SQLException e1) {
      e1.printStackTrace();
    }
    closeConnection();
    return ret;
  }

  public int updateSQL(String sql) {
    /*
    plugin.print("admin.sql.query", true, "info", sql);
    Connection c = null;

    if (checkConnection()) {
      c = getConnection();
      if (c == null) {
        c = openConnection();
      }
    }
    else {
      c = openConnection();
    }*/

    Statement s = null;
    int result = -1;

    try {
      s = openConnection().createStatement();
      result = s.executeUpdate(sql);
    }
    catch (SQLException e1) {
      e1.printStackTrace();
    }

    //plugin.print("admin.sql.queryrows", true, "info", result);
    closeConnection();
    return result;
  }
}
