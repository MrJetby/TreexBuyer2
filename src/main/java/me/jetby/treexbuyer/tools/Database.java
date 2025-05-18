package me.jetby.treexbuyer.tools;

import lombok.Getter;
import me.jetby.treexbuyer.Main;

import java.io.File;
import java.sql.*;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static me.jetby.treexbuyer.configurations.Config.CFG;
import static org.bukkit.Bukkit.getLogger;

public class Database {

    @Getter
    private Connection connection;
    private final Main plugin;
    private final boolean useMySQL;

    public Database(Main plugin) {
        this.plugin = plugin;
        this.useMySQL = CFG().getBoolean("mysql.enabled", false);
    }

    public void initDatabase() {
        try {
            if (useMySQL) {
                String host = CFG().getString("mysql.host", "localhost");
                int port = CFG().getInt("mysql.port", 3306);
                String database = CFG().getString("mysql.database", "treexbuyer");
                String username = CFG().getString("mysql.username", "root");
                String password = CFG().getString("mysql.password", "");

                String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true";
                connection = DriverManager.getConnection(url, username, password);

                if (CFG().getBoolean("logger")) {
                    plugin.getLogger().info("[TreexBuyer] Подключено к MySQL серверу: " + host);
                }
            } else {
                File dbFile = new File("plugins/TreexBuyer/data.db");
                if (!dbFile.getParentFile().exists()) {
                    dbFile.getParentFile().mkdirs();
                }
                connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

                if (CFG().getBoolean("logger")) {
                    plugin.getLogger().info("[TreexBuyer] База данных SQLite создана в: " + dbFile.getAbsolutePath());
                }
            }

            createTable();

        } catch (SQLException e) {
            e.printStackTrace();
            if (CFG().getBoolean("logger")) {
                plugin.getLogger().warning("[TreexBuyer] Ошибка подключения к базе данных: " + e.getMessage());
            }
        }
    }

    private void createTable() {
        try (Statement stmt = connection.createStatement()) {
            if (useMySQL) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS players (" +
                        "uuid VARCHAR(36) PRIMARY KEY," +
                        "scores DOUBLE DEFAULT 0" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS autobuy (" +
                        "uuid VARCHAR(36) PRIMARY KEY," +
                        "status TINYINT(1) DEFAULT 0," +
                        "items TEXT" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
            } else {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS players (" +
                        "uuid TEXT PRIMARY KEY," +
                        "scores REAL DEFAULT 0" +
                        ");");

                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS autobuy (" +
                        "uuid TEXT PRIMARY KEY," +
                        "status INTEGER DEFAULT 0," +
                        "items TEXT" +
                        ");");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            plugin.getLogger().warning("[TreexBuyer] Не удалось создать таблицы: " + e.getMessage());
        }
    }

    public <T> T get(String table, UUID uuid, String column, Function<ResultSet, T> extractor) {
        String sql = "SELECT " + column + " FROM " + table + " WHERE uuid = ?;";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return extractor.apply(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void set(String table, UUID uuid, String column, Object value) {
        String sql = "UPDATE " + table + " SET " + column + " = ? WHERE uuid = ?;";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, value);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void insertOrUpdateSync(String table, UUID uuid, Map<String, Object> data) {
        // без ExecutorService
        if (useMySQL) {
            String columns = String.join(", ", data.keySet());
            String placeholders = String.join(", ", Collections.nCopies(data.size(), "?"));
            String updates = String.join(", ", data.keySet().stream().map(col -> col + " = ?").toList());

            String sql = "INSERT INTO " + table + " (uuid, " + columns + ") VALUES (?, " + placeholders + ") " +
                    "ON DUPLICATE KEY UPDATE " + updates + ";";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                int i = 2;
                for (Object value : data.values()) {
                    stmt.setObject(i++, value);
                }
                for (Object value : data.values()) {
                    stmt.setObject(i++, value);
                }
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            String columns = String.join(", ", data.keySet());
            String placeholders = String.join(", ", Collections.nCopies(data.size(), "?"));

            String sql = "INSERT OR REPLACE INTO " + table + " (uuid, " + columns + ") VALUES (?, " + placeholders + ")";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                int i = 2;
                for (Object value : data.values()) {
                    stmt.setObject(i++, value);
                }
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    public void insertOrUpdate(String table, UUID uuid, Map<String, Object> data) {
        dbExecutor.submit(() -> {
        if (useMySQL) {
            String columns = String.join(", ", data.keySet());
            String placeholders = String.join(", ", Collections.nCopies(data.size(), "?"));
            String updates = String.join(", ", data.keySet().stream().map(col -> col + " = ?").toList());

            String sql = "INSERT INTO " + table + " (uuid, " + columns + ") VALUES (?, " + placeholders + ") " +
                    "ON DUPLICATE KEY UPDATE " + updates + ";";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                int i = 2;
                for (Object value : data.values()) {
                    stmt.setObject(i++, value);
                }
                for (Object value : data.values()) {
                    stmt.setObject(i++, value);
                }
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            String columns = String.join(", ", data.keySet());
            String placeholders = String.join(", ", Collections.nCopies(data.size(), "?"));
            String updates = String.join(", ", data.keySet().stream().map(col -> col + " = ?").toList());

            String sql = "INSERT OR REPLACE INTO " + table + " (uuid, " + columns + ") VALUES (?, " + placeholders + ")";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                int i = 2;
                for (Object value : data.values()) {
                    stmt.setObject(i++, value);
                }
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }});
    }
   @Getter private ExecutorService dbExecutor = Executors.newFixedThreadPool(4);
    public void restartExecutor() {
        if (this.dbExecutor != null && !this.dbExecutor.isShutdown()) {
            this.dbExecutor.shutdownNow();
        }
        this.dbExecutor = Executors.newFixedThreadPool(4);
    }
    public boolean playerExists(String table, UUID uuid) {
        String sql = "SELECT uuid FROM " + table + " WHERE uuid = ?;";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    public void delete(String table, UUID uuid) {
        String sql = "DELETE FROM " + table + " WHERE uuid = ?;";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                if (CFG().getBoolean("logger")) {
                    getLogger().info("[TreexBuyer] Подключение к базе данных закрыто.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}