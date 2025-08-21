package io.papermc.blocksmith.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.PreparedStatement;

public class DatabaseManager {
  private Connection mConnection;
  private static final String DatabaseURL = "jdbc:sqlite:plugins/BlocksmithShowcase/blocksmith.db?";
  private static final World BuildWorld = Bukkit.getWorld("superflat-world");
  private static DatabaseManager instance = null;

  private DatabaseManager() {
    connect();
  }

  public static DatabaseManager getInstance() {
      if (instance == null) {
          instance = new DatabaseManager();
      }
      return instance;  
  }

  public void connect() {
    try {
      Class.forName("org.sqlite.JDBC");
      this.mConnection = DriverManager.getConnection(DatabaseURL);

    } catch (ClassNotFoundException e) {
      System.err.println("class not found");
      e.printStackTrace();
    } catch (SQLException e) {
      System.err.println("cannot access database!");
      e.printStackTrace();
    }
  }

  public void createTables() {
    final String[] sqlStatements = {
        """
        create table if not exists teams (
            id integer primary key AUTOINCREMENT,
            name text not null
        );
      """,
          """
          create table if not exists players (
              id integer primary key AUTOINCREMENT,
              name text,
              minecraft_username text not null
          );
          """,
        """
            create table if not exists players_to_teams (
              id integer primary key AUTOINCREMENT,
              player_id integer not null,
              team_id integer not null,
          foreign key (player_id) references players (id),
          foreign key (team_id) references teams (id)
        );
      """,
        """
        create table if not exists players (
            id integer primary key AUTOINCREMENT,
            name text,
            minecraft_username text not null
        ); """,
            
            """
        create table if not exists plots (
            id integer primary key AUTOINCREMENT,
            team_id integer,
            center_x integer not null,
            center_y integer not null,
            center_z integer not null,
            foreign key (team_id) references teams(id)
        );
        """};

      try {
        for (String sql : sqlStatements) {
          mConnection.createStatement().execute(sql);
        }
      } catch (SQLException e) {
        System.err.println("error creating database tables");
        e.printStackTrace();
      }
  }

  public void dropAllTables() {
    String sql = """
        drop table if exists players_to_teams;
        drop table if exists plots;
        drop table if exists players;
        drop table if exists teams;
        """;

    try {
      mConnection.createStatement().execute(sql);
    } catch (SQLException e) {
      System.err.println("error dropping database tables");
      e.printStackTrace();
    }
  }

  public void setPlot(int plotNumber, int spawnX, int spawnY, int spawnZ) throws SQLException {
    String query = "select * from plots where id = ?";

    try (PreparedStatement stmt = mConnection.prepareStatement(query)) {
      stmt.setInt(1, plotNumber);
      if (stmt.executeQuery().next()) {
        // Plot already exists, update it
        String sql = "update plots set center_x = ?, center_y = ?, center_z = ? where id = ?";
        try (PreparedStatement updateStmt = mConnection.prepareStatement(sql)) {
          updateStmt.setInt(1, spawnX);
          updateStmt.setInt(2, spawnY);
          updateStmt.setInt(3, spawnZ);
          updateStmt.setInt(4, plotNumber);
          updateStmt.executeUpdate();
        }
      } else {
        // Plot does not exist, insert it
        addPlot(plotNumber, spawnX, spawnY, spawnZ);
      }
    }
  }

  public Location getSpawnLocationOfPlot(int plotNumber) throws SQLException {
    String sql = "select center_x, center_y, center_z from plots where id = ?";
    try (PreparedStatement stmt = mConnection.prepareStatement(sql)) {
      stmt.setInt(1, plotNumber);
      ResultSet rs = stmt.executeQuery();
      if (rs.next()) {
        int x = rs.getInt("center_x");
        int y = rs.getInt("center_y");
        int z = rs.getInt("center_z");
        return new Location(BuildWorld, x, y, z);
      }
    }
    return null;
  }

  private void addPlot(int plotNumber, int spawnX, int spawnY, int spawnZ) throws SQLException {
    String sql = """
        insert into plots (id, center_x, center_y, center_z) values (?, ?, ?, ?);
        """;

    try (PreparedStatement stmt = mConnection.prepareStatement(sql)) {
      stmt.setInt(1, plotNumber);
      stmt.setInt(2, spawnX);
      stmt.setInt(3, spawnY);
      stmt.setInt(4, spawnZ);
      stmt.executeUpdate();
    }
  }

  public void close() throws SQLException {
    if (mConnection == null) {
      throw new IllegalStateException("database connection is not established");
    }
      mConnection.close();
  }
}
