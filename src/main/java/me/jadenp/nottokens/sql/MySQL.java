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


}
