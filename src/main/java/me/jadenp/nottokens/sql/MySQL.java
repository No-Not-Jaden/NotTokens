package me.jadenp.nottokens.sql;

import com.mysql.cj.jdbc.MysqlConnectionPoolDataSource;
import com.mysql.cj.jdbc.MysqlDataSource;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static org.bukkit.Bukkit.getLogger;

public class MySQL {
    private final Plugin plugin;
    private Connection connection;

    private String host;
    private String port;
    private String database;
    private String username;
    private String password;
    private boolean useSSL;

    public MySQL(Plugin plugin){
        this.plugin = plugin;
        host = (plugin.getConfig().isSet("database.host") ? plugin.getConfig().getString("database.host") : "localhost");
        port = (plugin.getConfig().isSet("database.port") ? plugin.getConfig().getString("database.port") : "3306");
        database = (plugin.getConfig().isSet("database.database") ? plugin.getConfig().getString("database.database") : "db");
        username = (plugin.getConfig().isSet("database.user") ? plugin.getConfig().getString("database.user") : "user");
        password = (plugin.getConfig().isSet("database.password") ? plugin.getConfig().getString("database.password") : "");
        useSSL = (plugin.getConfig().isSet("database.use-ssl") && plugin.getConfig().getBoolean("database.use-ssl"));
        /*getLogger().info(host);
        getLogger().info(port);
        getLogger().info(database);
        getLogger().info(username);
        getLogger().info(password);
        getLogger().info(useSSL + "");
        host = "localhost";
        port = "3306";
        database = "jadenplugins";
        username = "root";
        password = "";*/
    }

    public boolean isConnected() {
        return connection != null;
    }

    public void connect() throws ClassNotFoundException, SQLException {
        if (!isConnected())
            connection = DriverManager.getConnection("jdbc:mysql://" +
                            host + ":" + port + "/" + database + "?useSSL=" + useSSL,
                    username, password);
    }

    public void disconnect(){
        if (isConnected())
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        connection = null;
    }

    public Connection getConnection() {
        return connection;
    }

    /*



    private DataSource initMySQLDataSource() throws SQLException {
        MysqlDataSource dataSource = new MysqlConnectionPoolDataSource();
        // set credentials
        dataSource.setServerName(plugin.getConfig().getString("database.host"));
        dataSource.setPortNumber(plugin.getConfig().getInt("database.port"));
        dataSource.setDatabaseName(plugin.getConfig().getString("database.database"));
        dataSource.setUser(plugin.getConfig().getString("database.user"));
        dataSource.setPassword(plugin.getConfig().getString("database.password"));
        // Test connection
        testDataSource(dataSource);
        return dataSource;
    }

    private void testDataSource(DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            if (!conn.isValid(1)) {
                throw new SQLException("Could not establish database connection.");
            }
        }
    }

    private void initDb() throws SQLException, IOException {
        String setup;
        try (InputStream in = getClassLoader().getResourceAsStream("dbsetup.sql")){
            setup = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
        } catch (IOException e){
            getLogger().log(Level.SEVERE, "Could not read db setup file.", e);
            throw e;
        }
        String[] queries = setup.split(";");
        // execute each query to the database.
        for (String query : queries) {
            // If you use the legacy way you have to check for empty queries here.
            if (query.isEmpty()) continue;
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.execute();
            }
        }
        getLogger().info("§2Database setup complete.");
    }*/
}
