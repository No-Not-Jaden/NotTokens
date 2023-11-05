package me.jadenp.nottokens;

import me.clip.placeholderapi.PlaceholderAPI;
import me.jadenp.nottokens.sql.MySQL;
import me.jadenp.nottokens.sql.SQLGetter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.md_5.bungee.api.ChatColor.COLOR_CHAR;
import static me.jadenp.nottokens.TokenManager.*;

public class ConfigOptions {
    public static Date now = new Date();
    public static SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
    public static SimpleDateFormat formatExact = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    public static int allMobReward;
    public static double allMobRate;
    public static boolean allMobRewardEnabled;
    public static int condenseSpam;
    public static boolean migrateLocalData;
    public static List<String> excludedNames = new ArrayList<>();
    public static boolean autoConnect;
    private static BukkitTask autoConnectTask = null;
    public static String prefix;
    public static ArrayList<String> language = new ArrayList<>();
    private static boolean firstStart = true;
    public static MySQL SQL;
    public static SQLGetter data;
    public static boolean negativeTokens;
    public static boolean papiEnabled;


    public static Map<String, String> loggedPlayers = new HashMap<>();

    public static void loadConfig(){
        Plugin plugin = NotTokens.getInstance();

        papiEnabled = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");

        plugin.reloadConfig();

        // adding parts to config if they are missing
        if (plugin.getConfig().get("kill-rewards.enabled") == null)
            plugin.getConfig().set("kill-rewards.enabled", false);
        if (!plugin.getConfig().isSet("condense-spam")) plugin.getConfig().set("condense-spam", -1);
        if (!plugin.getConfig().isSet("leaderboard-exclusion"))
            plugin.getConfig().set("leaderboard-exclusion", Collections.emptyList());
        if (!plugin.getConfig().isSet("prefix")) plugin.getConfig().set("prefix", "&7[&b&lNot&d&lTokens&7] &8➟ &r");
        if (!plugin.getConfig().isSet("balance")) plugin.getConfig().set("balance", "&aYou have {tokens} tokens.");
        if (!plugin.getConfig().isSet("admin-add"))
            plugin.getConfig().set("admin-add", "&aYou have given {player} {tokens} tokens.");
        if (!plugin.getConfig().isSet("player-receive"))
            plugin.getConfig().set("player-receive", "&aYou have received {tokens} tokens.");
        if (!plugin.getConfig().isSet("admin-remove"))
            plugin.getConfig().set("admin-remove", "&aYou have removed {tokens} tokens from {player}.");
        if (!plugin.getConfig().isSet("player-take"))
            plugin.getConfig().set("player-take", "&a{tokens} tokens have been removed from your account.");
        if (!plugin.getConfig().isSet("admin-set"))
            plugin.getConfig().set("admin-set", "&a{player} now has {tokens} tokens.");
        if (!plugin.getConfig().isSet("player-set"))
            plugin.getConfig().set("player-set", "&aYour tokens were set to {tokens}.");
        if (!plugin.getConfig().isSet("unknown-command"))
            plugin.getConfig().set("unknown-command", "&cUnknown Command! /token help");
        if (!plugin.getConfig().isSet("unknown-player")) plugin.getConfig().set("unknown-player", "&cUnknown Player!");
        if (!plugin.getConfig().isSet("reduced-message"))
            plugin.getConfig().set("reduced-message", "&aYour tokens have changed by {tokens} in the last {time} seconds.");
        if (!plugin.getConfig().isSet("other-tokens"))
            plugin.getConfig().set("other-tokens", "&a{player} has {tokens} tokens.");
        if (!plugin.getConfig().isSet("database.host")) plugin.getConfig().set("database.host", "localhost");
        if (!plugin.getConfig().isSet("database.port")) plugin.getConfig().set("database.port", "3306");
        if (!plugin.getConfig().isSet("database.database")) plugin.getConfig().set("database.database", "db");
        if (!plugin.getConfig().isSet("database.user")) plugin.getConfig().set("database.user", "username");
        if (!plugin.getConfig().isSet("database.password")) plugin.getConfig().set("database.password", "");
        if (!plugin.getConfig().isSet("database.use-ssl")) plugin.getConfig().set("database.use-ssl", false);
        if (!plugin.getConfig().isSet("database.migrate-local-data"))
            plugin.getConfig().set("database.migrate-local-data", true);
        if (!plugin.getConfig().isSet("admin-give-all"))
            plugin.getConfig().set("admin-give-all", "&aYou have given {amount} players {tokens} tokens.");
        if (!plugin.getConfig().isSet("database.auto-connect")) plugin.getConfig().set("database.auto-connect", false);
        if (!plugin.getConfig().isSet("kill-rewards.all-mobs.enabled"))
            plugin.getConfig().set("kill-rewards.all-mobs.enabled", false);
        if (!plugin.getConfig().isSet("kill-rewards.all-mobs.amount"))
            plugin.getConfig().set("kill-rewards.all-mobs.amount", 1);
        if (!plugin.getConfig().isSet("kill-rewards.all-mobs.rate"))
            plugin.getConfig().set("kill-rewards.all-mobs.rate", 0.1);
        if (!plugin.getConfig().isSet("negative-tokens"))
            plugin.getConfig().set("negative-tokens", false);
        if (!plugin.getConfig().isSet("insufficient-tokens"))
            plugin.getConfig().set("insufficient-tokens", "&cYou do not have enough tokens for this transaction!");
        plugin.saveConfig();

        allMobRewardEnabled = plugin.getConfig().getBoolean("kill-rewards.all-mobs.enabled");
        allMobReward = plugin.getConfig().getInt("kill-rewards.all-mobs.amount");
        allMobRate = plugin.getConfig().getDouble("kill-rewards.all-mobs.rate");
        condenseSpam = plugin.getConfig().getInt("condense-spam");
        negativeTokens = plugin.getConfig().getBoolean("negative-tokens");

        language.clear();

        prefix = color(plugin.getConfig().getString("prefix"), null);
        //0 balance
        language.add(plugin.getConfig().getString("balance"));
        //1 admin-add
        language.add(plugin.getConfig().getString("admin-add"));
        //2 player-receive
        language.add(plugin.getConfig().getString("player-receive"));
        //3 admin-remove
        language.add(plugin.getConfig().getString("admin-remove"));
        //4 player-take
        language.add(plugin.getConfig().getString("player-take"));
        //5 unknown-command
        language.add(plugin.getConfig().getString("unknown-command"));
        //6 unknown-player
        language.add(plugin.getConfig().getString("unknown-player"));
        //7 admin-set
        language.add(plugin.getConfig().getString("admin-set"));
        //8 player-set
        language.add(plugin.getConfig().getString("player-set"));
        //9 reduced-message
        language.add(plugin.getConfig().getString("reduced-message"));
        //10 other-tokens
        language.add(plugin.getConfig().getString("other-tokens"));
        //11 admin-give-all
        language.add(plugin.getConfig().getString("admin-give-all"));
        //12 insufficient-tokens
        language.add(plugin.getConfig().getString("insufficient-tokens"));



        excludedNames = plugin.getConfig().getStringList("leaderboard-exclusion");
        excludedNames.replaceAll(str -> str.toUpperCase(Locale.ROOT));

        tokenRewards.clear();
        if (plugin.getConfig().getBoolean("kill-rewards.enabled")) {
            int i = 1;
            while (plugin.getConfig().getString("kill-rewards." + i + ".name") != null) {
                String key = plugin.getConfig().getString("kill-rewards." + i + ".name");
                int tokenAmount = plugin.getConfig().getInt("kill-rewards." + i + ".amount");
                double rate = 1;
                if (plugin.getConfig().isSet("kill-rewards." + i + ".rate")){
                    rate = plugin.getConfig().getDouble("kill-rewards." + i + ".rate");
                }
                EntityType entityType = getEntityByName(key);
                if (entityType != null) {
                    tokenRewards.add(new TokenRewards(entityType, tokenAmount, rate));
                } else {
                    Bukkit.getLogger().warning("Could not find entity type for \"" + key + "\".");
                }
                i++;
            }
        }

        migrateLocalData = plugin.getConfig().getBoolean("database.migrate-local-data");
        autoConnect = plugin.getConfig().getBoolean("database.auto-connect");
        if (autoConnectTask != null){
            autoConnectTask.cancel();
        }
        if (autoConnect){
            autoConnectTask = new BukkitRunnable(){
                @Override
                public void run() {
                    tryToConnect();
                }
            }.runTaskTimer(plugin, 600, 600);
        }


    }

    public static boolean tryToConnect(){
        if (!SQL.isConnected()) {
            try {
                SQL.connect();
            } catch (ClassNotFoundException | SQLException e) {
                //e.printStackTrace();
                return false;
            }

            if (SQL.isConnected()) {
                Bukkit.getLogger().info("Database is connected!");
                data.createTable();
                if (!tokens.isEmpty() && migrateLocalData) {
                    Bukkit.getLogger().info("Migrating local storage to database");
                    // add entries to database
                    for (Map.Entry<String, Long> entry : tokens.entrySet()) {
                        if (entry.getValue() != 0L)
                            data.addTokens(UUID.fromString(entry.getKey()), entry.getValue());
                    }
                    tokens.clear();
                    YamlConfiguration configuration = new YamlConfiguration();
                    try {
                        configuration.save(NotTokens.getInstance().tokensHolder);
                    } catch (IOException e) {
                        Bukkit.getLogger().warning(e.toString());
                    }
                }
                int rows = data.removeExtraData();
                if (firstStart) {
                    Bukkit.getLogger().info("Cleared up " + rows + " unused rows in the database!");
                    firstStart = false;
                }
            }
        }
        return true;
    }

    public static String color(String str, OfflinePlayer player){
        if (papiEnabled)
            str = PlaceholderAPI.setPlaceholders(player, str);
        str = ChatColor.translateAlternateColorCodes('&', str);
        return translateHexColorCodes("&#","", str);
    }
    public static String translateHexColorCodes(String startTag, String endTag, String message)
    {
        final Pattern hexPattern = Pattern.compile(startTag + "([A-Fa-f0-9]{6})" + endTag);
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);
        while (matcher.find())
        {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, COLOR_CHAR + "x"
                    + COLOR_CHAR + group.charAt(0) + COLOR_CHAR + group.charAt(1)
                    + COLOR_CHAR + group.charAt(2) + COLOR_CHAR + group.charAt(3)
                    + COLOR_CHAR + group.charAt(4) + COLOR_CHAR + group.charAt(5)
            );
        }
        return matcher.appendTail(buffer).toString();
    }

    public static EntityType getEntityByName(String name) {
        for (EntityType type : EntityType.values()) {
            if(type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
