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

      plugin.write(null, "admin.sql.connected", "debug");
    }
    catch (SQLException e) {
      plugin.write(null, "admin.sql.ex", "severe");
    }
    catch (ClassNotFoundException e) {
      plugin.write(null, "admin.sql.exx", "severe");
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
        plugin.write(null, "admin.sql.disconnected", "debug");
      }
      catch (SQLException e) {
        plugin.write(null, "admin.sql.disconnectedfail", "severe");
        //e.printStackTrace();
      }
    }
  }

  public ResultSet querySQL(String query) throws SQLException {

    plugin.write(null, "admin.sql.query", "debug", query);
    Statement s = null;
    ResultSet ret = null;

    s = openConnection().createStatement();
    ret = s.executeQuery(query);

    //closeConnection();
    return ret;
  }

  public int updateSQL(String sql) throws SQLException {

    Statement s = null;
    plugin.write(null, "admin.sql.query", "debug", sql);
    int result = -1;

    s = openConnection().createStatement();
    result = s.executeUpdate(sql);

    //plugin.print("admin.sql.queryrows", true, "info", result);
    //closeConnection();
    return result;
  }
}
