package me.jadenp.nottokens.sql;

import me.jadenp.nottokens.NotTokens;
import me.jadenp.nottokens.TokenPlayer;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class SQLGetter {

    private final NotTokens plugin;
    public SQLGetter (NotTokens plugin){
        this.plugin = plugin;
    }

    public void createTable(){
        PreparedStatement ps;
        try {
            ps = plugin.SQL.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS player_tokens" +
                    "(" +
                    "    uuid CHAR(36) NOT NULL," +
                    "    tokens BIGINT DEFAULT 0 NOT NULL," +
                    "    PRIMARY KEY (uuid)" +
                    ");");
            ps.executeUpdate();
        } catch (SQLException e){
            Bukkit.getLogger().warning("Lost connection with database, will try to reconnect.");
            NotTokens.getInstance().SQL.disconnect();
            if (!plugin.tryToConnect()) {
                Bukkit.getScheduler().runTaskLater(NotTokens.getInstance(), () -> {
                    if (NotTokens.getInstance().tryToConnect()){
                        createTable();
                    }
                }, 20L);
            } else {
                createTable();
            }
            //e.printStackTrace();
        }
    }

    public void addTokens(UUID uuid, long amount){
        try {
            PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("INSERT INTO player_tokens(uuid, tokens) VALUES(?, ?) ON DUPLICATE KEY UPDATE tokens = tokens + ?;");
            ps.setString(1, uuid.toString());
            ps.setLong(2, amount);
            ps.setLong(3, amount);
            ps.executeUpdate();
        } catch (SQLException e){
            if (reconnect()){
                addTokens(uuid, amount);
            } else {
                plugin.addTokens(Bukkit.getOfflinePlayer(uuid), amount);
            }
            //e.printStackTrace();
        }
    }

    public long getTokens(UUID uuid) {
        try {
            PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("SELECT tokens FROM player_tokens WHERE uuid = ?;");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            long tokens;
            if (rs.next()){
                tokens = rs.getLong("tokens");
                return tokens;
            }
        } catch (SQLException e){
            if (reconnect()){
                return getTokens(uuid);
            }
            //e.printStackTrace();
        }
        return 0;
    }
    public void removeTokens(UUID uuid, long amount) {
        try {
            PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("UPDATE player_tokens SET tokens = tokens - ? WHERE uuid = ? AND tokens >= ?;");
            ps.setLong(1, amount);
            ps.setString(2, uuid.toString());
            ps.setLong(3, amount);
            ps.executeUpdate();
        } catch (SQLException e){
            if (reconnect()){
                removeTokens(uuid, amount);
            } else {
                plugin.removeTokens(Bukkit.getOfflinePlayer(uuid), amount);
            }
            //e.printStackTrace();
        }
    }

    public void setToken(UUID uuid, long amount) {
        try {
            PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("REPLACE player_tokens(uuid, tokens) VALUES(? ,?);");
            ps.setString(1, uuid.toString());
            ps.setLong(2, amount);
            ps.executeUpdate();
        } catch (SQLException e){
            if (reconnect()){
                setToken(uuid, amount);
            } else {
                plugin.setTokens(Bukkit.getOfflinePlayer(uuid), amount);
            }
            //e.printStackTrace();
        }
    }

    public List<TokenPlayer> getTopTokens(int amount) {
        try {
            PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("SELECT uuid, tokens FROM player_tokens ORDER BY tokens DESC LIMIT ?;");
            ps.setInt(1, amount);
            ResultSet resultSet = ps.executeQuery();
            List<TokenPlayer> tokens = new LinkedList<>();
            while (resultSet.next()) {
                if (resultSet.getString("uuid") != null) {
                    tokens.add(
                            new TokenPlayer(
                                    UUID.fromString(resultSet.getString("uuid")),
                                    resultSet.getLong("tokens"))
                    );
                }
            }
            return tokens;
        } catch (SQLException e){
            if (reconnect()){
                return getTopTokens(amount);
            }
            //e.printStackTrace();
        }
        return new LinkedList<>();

    }

    public int removeExtraData() {
        try {
            PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("DELETE FROM player_tokens WHERE tokens = ?;");
            ps.setLong(1, 0L);
            return ps.executeUpdate();
        } catch (SQLException e){
            reconnect();
            //e.printStackTrace();
        }
        return 0;
    }

    public boolean reconnect(){
        NotTokens.getInstance().SQL.disconnect();
        Bukkit.getLogger().warning("Lost connection with database, will try to reconnect.");
        if (!plugin.tryToConnect()){
            Bukkit.getScheduler().runTaskLater(NotTokens.getInstance(), () -> NotTokens.getInstance().tryToConnect(), 20L);
            return false;
        }
        return true;
    }




}
