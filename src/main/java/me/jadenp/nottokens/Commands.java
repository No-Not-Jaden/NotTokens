package me.jadenp.nottokens;

import me.jadenp.nottokens.sql.MySQL;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

import static me.jadenp.nottokens.ConfigOptions.*;
import static me.jadenp.nottokens.ConfigOptions.excludedNames;
import static me.jadenp.nottokens.TokenManager.*;

public class Commands implements CommandExecutor, TabCompleter {
    File records = new File(NotTokens.getInstance().getDataFolder() + File.separator + "records");
    File today = new File(records + File.separator + format.format(now) + ".txt");

    enum MigrateType {
        DELETE,
        MAINTAIN,
        UPDATE
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("token")) {
            if (sender.hasPermission("nottokens.admin")) {
                if (args.length == 0) {
                    String text = language.get(0);
                    if (text.contains("{tokens}"))
                        text = text.replace("{tokens}", getTokens(((Player) sender).getUniqueId()) + "");
                    sender.sendMessage(prefix + color(text));
                } else if (args[0].equalsIgnoreCase("reload")) {
                    loadConfig();
                    try {
                        NotTokens.getInstance().saveTokens();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    sender.sendMessage(prefix + ChatColor.GREEN + "Reloaded NotTokens version " + NotTokens.getInstance().getDescription().getVersion() + ".");
                } else if (args[0].equalsIgnoreCase("help")) {
                    sender.sendMessage(prefix + ChatColor.GREEN + "Token admin commands:");
                    sender.sendMessage(ChatColor.YELLOW + "/token " + ChatColor.GOLD + " Shows your current token balance");
                    sender.sendMessage(ChatColor.YELLOW + "/token help" + ChatColor.GOLD + " Displays this");
                    sender.sendMessage(ChatColor.YELLOW + "/token reload" + ChatColor.GOLD + " Reloads this plugin");
                    sender.sendMessage(ChatColor.YELLOW + "/token (player)" + ChatColor.GOLD + " Shows a player's current token balance");
                    sender.sendMessage(ChatColor.YELLOW + "/token give (player) (amount)" + ChatColor.GOLD + " Gives a player tokens");
                    sender.sendMessage(ChatColor.YELLOW + "/token giveall (amount) (online/offline)" + ChatColor.GOLD + " Gives all offline or online players tokens");
                    sender.sendMessage(ChatColor.YELLOW + "/token remove (player) (amount)" + ChatColor.GOLD + " Removes a player's tokens");
                    sender.sendMessage(ChatColor.YELLOW + "/token set (player) (amount)" + ChatColor.GOLD + " Sets a player's tokens");
                    sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "                                              ");
                } else if (args[0].equalsIgnoreCase("give")) {
                    if (args.length >= 2) {
                        Player p = Bukkit.getPlayer(args[1]);
                        if (p == null) {
                            // check if it is in logged players
                            if (loggedPlayers.containsKey(args[1].toLowerCase(Locale.ROOT))){
                                OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(loggedPlayers.get(args[1].toLowerCase(Locale.ROOT))));
                                String name = player.getName() == null ? args[1] : player.getName();
                                if (args.length >= 3) {
                                    long number = -1;
                                    try {
                                        number = Long.parseLong(args[2]);
                                    } catch (NumberFormatException e) {
                                        sender.sendMessage(prefix + ChatColor.GREEN + "Usage: ");
                                        sender.sendMessage(ChatColor.YELLOW + "/token give (player) (amount)" + ChatColor.GOLD + " Gives a player tokens");
                                    }
                                    if (number != -1) {
                                        addTokens(player, number);
                                        transactions.add("[" + formatExact.format(now) + "] " + name + " has received " + number + " tokens from " + sender.getName() + ". Total:" + getTokens(player.getUniqueId()));
                                        String text = language.get(1);
                                        if (text.contains("{player}")) {
                                            text = text.replace("{player}", name);
                                        }
                                        if (text.contains("{tokens")) {
                                            text = text.replace("{tokens}", number + "");
                                        }
                                        sender.sendMessage(prefix + color(text));

                                    }
                                } else {
                                    sender.sendMessage(prefix + ChatColor.GREEN + "Usage: ");
                                    sender.sendMessage(ChatColor.YELLOW + "/token give (player) (amount)" + ChatColor.GOLD + " Gives a player tokens");
                                }
                            } else {
                                sender.sendMessage(prefix + color(language.get(6)));
                            }
                        } else {
                            if (args.length >= 3) {
                                long number = -1;
                                try {
                                    number = Long.parseLong(args[2]);
                                } catch (NumberFormatException e) {
                                    sender.sendMessage(prefix + ChatColor.GREEN + "Usage: ");
                                    sender.sendMessage(ChatColor.YELLOW + "/token give (player) (amount)" + ChatColor.GOLD + " Gives a player tokens");
                                }
                                if (number != -1) {
                                    addTokens(p, number);
                                    transactions.add("[" + formatExact.format(now) + "] " + p.getName() + " has received " + number + " tokens from " + sender.getName() + ". Total:" + getTokens(p.getUniqueId()));
                                    String text = language.get(1);
                                    if (text.contains("{player}")) {
                                        text = text.replace("{player}", p.getName());
                                    }
                                    if (text.contains("{tokens")) {
                                        text = text.replace("{tokens}", number + "");
                                    }
                                    sender.sendMessage(prefix + color(text));

                                }
                            } else {
                                sender.sendMessage(prefix + ChatColor.GREEN + "Usage: ");
                                sender.sendMessage(ChatColor.YELLOW + "/token give (player) (amount)" + ChatColor.GOLD + " Gives a player tokens");
                            }
                        }
                    } else {
                        sender.sendMessage(prefix + ChatColor.GREEN + "Usage: ");
                        sender.sendMessage(ChatColor.YELLOW + "/token give (player) (amount)" + ChatColor.GOLD + " Gives a player tokens");
                    }
                } else if (args[0].equalsIgnoreCase("giveall")) {
                    if (args.length >= 2) {
                        if (args.length == 3){
                            if (args[2].equalsIgnoreCase("offline")){
                                long number = -1;
                                try {
                                    number = Long.parseLong(args[1]);
                                } catch (NumberFormatException e) {
                                    sender.sendMessage(prefix + ChatColor.GREEN + "Usage: ");
                                    sender.sendMessage(ChatColor.YELLOW + "/token giveall (amount) (online/offline)" + ChatColor.GOLD + " Gives all offline or online players tokens");
                                }
                                if (number != -1) {
                                    List<String> usedUUIDS = new ArrayList<>();
                                    for (Map.Entry<String, String> entry : loggedPlayers.entrySet()){
                                        if (!usedUUIDS.contains(entry.getValue())){
                                            OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(entry.getValue()));
                                            if (player.isOnline()) {
                                                addTokens(player.getPlayer(), number);
                                            } else {
                                                addTokens(player, number);
                                            }
                                            usedUUIDS.add(entry.getValue());
                                        }
                                    }

                                    transactions.add("[" + formatExact.format(now) + "] " + usedUUIDS.size() + " players have received " + number + " tokens from " + sender.getName());

                                    String text = language.get(11);
                                    if (text.contains("{amount}")) {
                                        text = text.replace("{amount}", usedUUIDS.size() + "");
                                    }
                                    if (text.contains("{tokens}")) {
                                        text = text.replace("{tokens}", number + "");
                                    }
                                    sender.sendMessage(prefix + color(text));
                                    return true;
                                }
                            }
                        }
                        long number = -1;
                        try {
                            number = Long.parseLong(args[1]);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(prefix + ChatColor.GREEN + "Usage: ");
                            sender.sendMessage(ChatColor.YELLOW + "/token giveall (amount) (online/offline)" + ChatColor.GOLD + " Gives all offline or online players tokens");
                        }
                        if (number != -1) {
                            for (Player player : Bukkit.getOnlinePlayers()){
                                addTokens(player, number);
                            }

                            transactions.add("[" + formatExact.format(now) + "] " + Bukkit.getOnlinePlayers().size() + " players have received " + number + " tokens from " + sender.getName());

                            String text = language.get(11);
                            if (text.contains("{amount}")) {
                                text = text.replace("{amount}", Bukkit.getOnlinePlayers().size() + "");
                            }
                            if (text.contains("{tokens}")) {
                                text = text.replace("{tokens}", number + "");
                            }
                            sender.sendMessage(prefix + color(text));
                            return true;
                        }
                    } else {
                        sender.sendMessage(prefix + ChatColor.GREEN + "Usage: ");
                        sender.sendMessage(ChatColor.YELLOW + "/token giveall (amount) (online/offline)" + ChatColor.GOLD + " Gives all offline or online players tokens");
                    }
                } else if (args[0].equalsIgnoreCase("remove")) {
                    if (args.length >= 2) {
                        Player p = Bukkit.getPlayer(args[1]);
                        if (p == null) {
                            if (loggedPlayers.containsKey(args[1].toLowerCase(Locale.ROOT))) {
                                OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(loggedPlayers.get(args[1].toLowerCase(Locale.ROOT))));
                                String name = player.getName() == null ? args[1] : player.getName();
                                if (args.length >= 3) {
                                    long number = -1;
                                    try {
                                        number = Long.parseLong(args[2]);
                                    } catch (NumberFormatException e) {
                                        sender.sendMessage(prefix + ChatColor.GREEN + "Usage: ");
                                        sender.sendMessage(ChatColor.YELLOW + "/token remove (player) (amount)" + ChatColor.GOLD + " Removes a player's tokens");
                                    }
                                    if (number != -1) {
                                        removeTokens(player, number);
                                        transactions.add("[" + formatExact.format(now) + "] " + name + " has had " + number + " tokens removed from " + sender.getName() + ". Total:" + getTokens(player.getUniqueId()));
                                        String text = language.get(3);
                                        if (text.contains("{player}")) {
                                            text = text.replace("{player}", name);
                                        }
                                        if (text.contains("{tokens}")) {
                                            text = text.replace("{tokens}", number + "");
                                        }
                                        sender.sendMessage(prefix + color(text));
                                    }
                                } else {
                                    sender.sendMessage(prefix + ChatColor.GREEN + "Usage: ");
                                    sender.sendMessage(ChatColor.YELLOW + "/token remove (player) (amount)" + ChatColor.GOLD + " Removes a player's tokens");
                                }
                            } else {
                                sender.sendMessage(prefix + color(language.get(6)));
                            }

                        } else {
                            if (args.length >= 3) {
                                long number = -1;
                                try {
                                    number = Long.parseLong(args[2]);
                                } catch (NumberFormatException e) {
                                    sender.sendMessage(prefix + ChatColor.GREEN + "Usage: ");
                                    sender.sendMessage(ChatColor.YELLOW + "/token remove (player) (amount)" + ChatColor.GOLD + " Removes a player's tokens");
                                }
                                if (number != -1) {
                                    removeTokens(p, number);
                                    transactions.add("[" + formatExact.format(now) + "] " + p.getName() + " has had " + number + " tokens removed from " + sender.getName() + ". Total:" + getTokens(p.getUniqueId()));
                                    String text = language.get(3);
                                    if (text.contains("{player}")) {
                                        text = text.replace("{player}", p.getName());
                                    }
                                    if (text.contains("{tokens}")) {
                                        text = text.replace("{tokens}", number + "");
                                    }
                                    sender.sendMessage(prefix + color(text));
                                }
                            } else {
                                sender.sendMessage(prefix + ChatColor.GREEN + "Usage: ");
                                sender.sendMessage(ChatColor.YELLOW + "/token remove (player) (amount)" + ChatColor.GOLD + " Removes a player's tokens");
                            }
                        }
                    } else {
                        sender.sendMessage(prefix + ChatColor.GREEN + "Usage: ");
                        sender.sendMessage(ChatColor.YELLOW + "/token remove (player) (amount)" + ChatColor.GOLD + " Removes a player's tokens");
                    }
                } else if (args[0].equalsIgnoreCase("set")) {
                    if (args.length >= 2) {
                        Player p = Bukkit.getPlayer(args[1]);
                        if (p == null) {
                            if (loggedPlayers.containsKey(args[1].toLowerCase(Locale.ROOT))) {
                                OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(loggedPlayers.get(args[1].toLowerCase(Locale.ROOT))));
                                String name = player.getName() == null ? args[1] : player.getName();
                                if (args.length >= 3) {
                                    long number = -1;
                                    try {
                                        number = Long.parseLong(args[2]);
                                    } catch (NumberFormatException e) {
                                        sender.sendMessage(prefix + ChatColor.GREEN + "Usage: ");
                                        sender.sendMessage(ChatColor.YELLOW + "/token set (player) (amount)" + ChatColor.GOLD + " Sets a player's tokens");
                                    }
                                    if (number != -1) {
                                        setTokens(player, number);
                                        transactions.add("[" + formatExact.format(now) + "] " + name + " has had " + number + " tokens set by " + sender.getName() + ". Total:" + getTokens(player.getUniqueId()));
                                        String text = language.get(7);
                                        if (text.contains("{player}")) {
                                            text = text.replace("{player}", name);
                                        }
                                        if (text.contains("{tokens}")) {
                                            text = text.replace("{tokens}", number + "");
                                        }
                                        sender.sendMessage(prefix + color(text));

                                    }
                                } else {
                                    sender.sendMessage(prefix + ChatColor.GREEN + "Usage: ");
                                    sender.sendMessage(ChatColor.YELLOW + "/token set (player) (amount)" + ChatColor.GOLD + " Sets a player's tokens");
                                }
                            } else {
                                sender.sendMessage(prefix + color(language.get(6)));
                            }
                        } else {
                            if (args.length >= 3) {
                                long number = -1;
                                try {
                                    number = Long.parseLong(args[2]);
                                } catch (NumberFormatException e) {
                                    sender.sendMessage(prefix + ChatColor.GREEN + "Usage: ");
                                    sender.sendMessage(ChatColor.YELLOW + "/token set (player) (amount)" + ChatColor.GOLD + " Sets a player's tokens");
                                }
                                if (number != -1) {
                                    setTokens(p, number);
                                    transactions.add("[" + formatExact.format(now) + "] " + p.getName() + " has had " + number + " tokens set by " + sender.getName() + ". Total:" + getTokens(p.getUniqueId()));
                                    String text = language.get(7);
                                    if (text.contains("{player}")) {
                                        text = text.replace("{player}", p.getName());
                                    }
                                    if (text.contains("{tokens}")) {
                                        text = text.replace("{tokens}", number + "");
                                    }
                                    sender.sendMessage(prefix + color(text));

                                }
                            } else {
                                sender.sendMessage(prefix + ChatColor.GREEN + "Usage: ");
                                sender.sendMessage(ChatColor.YELLOW + "/token set (player) (amount)" + ChatColor.GOLD + " Sets a player's tokens");
                            }
                        }
                    } else {
                        sender.sendMessage(prefix + ChatColor.GREEN + "Usage: ");
                        sender.sendMessage(ChatColor.YELLOW + "/token set (player) (amount)" + ChatColor.GOLD + " Sets a player's tokens");
                    }
                } else if (args[0].equalsIgnoreCase("top")) {
                    displayTopTokens(sender);
                    return true;
                } else if (args[0].equalsIgnoreCase("migrate")){
                    // maybe add an "are you sure?"
                    if (args.length == 1){
                        // migrate notTokens data to sql

                    } else if (args.length == 3){
                        // 0 - add all tokens into NotTokens and delete other plugin values
                        // 1 - add all tokens into NotTokens but keep other plugin values
                        // 2 - add only players that are not already in NotTokens
                        MigrateType migrateType;
                        try {
                            migrateType = MigrateType.valueOf(args[2].toUpperCase());
                        } catch (IllegalArgumentException e){
                            // usage
                            sender.sendMessage(prefix + ChatColor.AQUA + "/tokens migrate " + args[1] + " (DELETE/MAINTAIN/UPDATE)");
                            return true;
                        }

                        if (args[1].equalsIgnoreCase("tokenManager")){
                            Plugin tokenManager = Bukkit.getPluginManager().getPlugin("TokenManager");
                            if (tokenManager == null || !tokenManager.isEnabled()){
                                // plugin not found
                                sender.sendMessage(prefix + "Plugin not found!");
                                return true;
                            }
                            File data = new File(tokenManager.getDataFolder() + File.separator + "data.yml");
                            // migrate local data
                            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(data);
                            for (String key : configuration.getConfigurationSection("Players").getKeys(false)) {
                                UUID player = UUID.fromString(key);
                                int amount = configuration.getInt("Players." + key);
                                switch (migrateType){
                                    case DELETE:
                                        configuration.set(key, null);
                                    case MAINTAIN:
                                        addTokens(Bukkit.getOfflinePlayer(player), amount);
                                        break;
                                    case UPDATE:
                                        if (getTokens(player) == 0)
                                            addTokens(Bukkit.getOfflinePlayer(player), amount);
                                        break;
                                }
                                if (migrateType == MigrateType.DELETE){

                                    try {
                                        configuration.save(data);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            }
                            // migrate sql data
                            FileConfiguration config = tokenManager.getConfig();
                            if (!config.getBoolean("data.mysql.enabled"))
                                return true;
                            boolean useSSL = config.isSet("data.mysql.usessl") && config.getBoolean("data.mysql.usessl");
                            MySQL tokenManagerSQL = new MySQL(config.getString("data.mysql.hostname"), config.getString("data.mysql.port"), config.getString("data.mysql.database"), config.getString("data.mysql.username"), config.getString("data.mysql.password"), useSSL);
                            String tableName = config.getString("data.mysql.table");
                            try {
                                PreparedStatement ps = tokenManagerSQL.getConnection().prepareStatement("SELECT * FROM ?;");
                                ps.setString(1, tableName);
                                ResultSet rs = ps.executeQuery();
                                if (rs.next()){
                                    UUID uuid = ;
                                    int amount = ;
                                    switch (migrateType){
                                        case DELETE:
                                        case MAINTAIN:
                                            addTokens(Bukkit.getOfflinePlayer(uuid), amount);
                                            break;
                                        case UPDATE:
                                            if (getTokens(uuid) == 0)
                                                addTokens(Bukkit.getOfflinePlayer(uuid), amount);
                                    }
                                }
                                // delete old data
                                if (migrateType == MigrateType.DELETE) {
                                    ps = tokenManagerSQL.getConnection().prepareStatement("DELETE FROM ?;");
                                    ps.setString(1, tableName);
                                    ps.executeQuery();
                                }
                            } catch (SQLException e){
                                sender.sendMessage(prefix + ChatColor.RED + "SQL Error.");
                                //e.printStackTrace();
                            }
                        }
                    } else {
                        // usage
                        sender.sendMessage(prefix + ChatColor.AQUA + "/tokens migrate (plugin) (DELETE/MAINTAIN/UPDATE)");
                    }
                }
                else {
                    Player p = Bukkit.getPlayer(args[0]);
                    if (p == null){
                        if (loggedPlayers.containsKey(args[0].toLowerCase(Locale.ROOT))) {
                            OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(loggedPlayers.get(args[0].toLowerCase(Locale.ROOT))));
                            String name = player.getName() == null ? args[1] : player.getName();
                            String text = language.get(10);
                            if (text.contains("{player}")) {
                                text = text.replace("{player}", name);
                            }
                            if (text.contains("{tokens")) {
                                text = text.replace("{tokens}", getTokens(player.getUniqueId()) + "");
                            }
                            sender.sendMessage(prefix + color(text));
                        } else {
                            sender.sendMessage(prefix + color(language.get(6)));
                        }
                    } else {
                        String text = language.get(10);
                        if (text.contains("{player}")) {
                            text = text.replace("{player}", Objects.requireNonNull(p.getName()));
                        }
                        if (text.contains("{tokens")) {
                            text = text.replace("{tokens}", getTokens(p.getUniqueId()) + "");
                        }
                        sender.sendMessage(prefix + color(text));
                    }
                }
            } else {
                if (sender.hasPermission("nottokens.top")) {
                    if (args.length > 0) {
                        if (args[0].equalsIgnoreCase("top")) {
                            displayTopTokens(sender);
                            return true;
                        }
                    }
                }
                String text = language.get(0);
                if (text.contains("{tokens}"))
                    text = text.replace("{tokens}", getTokens(((Player) sender).getUniqueId()) + "");
                sender.sendMessage(prefix + color(text));
            }
        }

        return true;
    }

    public void displayTopTokens(CommandSender sender){
        sender.sendMessage(prefix + ChatColor.GREEN + "Token Leaderboards:");
        if (SQL.isConnected()) {
            List<TokenPlayer> topTokens;
            List<TokenPlayer> actualTop = new LinkedList<>();
            int j = 0;
            while (actualTop.size() < 10){
                topTokens = data.getTopTokens(10 + j);
                for (TokenPlayer topToken : topTokens) {
                    if (topToken == null)
                        continue;
                    if (topToken.getOfflinePlayer() == null)
                        continue;
                    if (topToken.getOfflinePlayer().getName() == null) {
                        String name = null;
                        if (loggedPlayers.containsValue(topToken.getOfflinePlayer().getUniqueId().toString())) {
                            for (Map.Entry<String, String> entry : loggedPlayers.entrySet()){
                                if (entry.getValue().equals(topToken.getOfflinePlayer().getUniqueId().toString())){
                                    name = entry.getKey();
                                }
                            }
                        }
                        if (name != null) {
                            if (excludedNames.contains(name.toUpperCase(Locale.ROOT))) {
                                continue;
                            }
                        } else {
                            continue;
                        }
                    } else {
                        if (excludedNames.contains(topToken.getOfflinePlayer().getName().toUpperCase(Locale.ROOT)))
                            continue;
                    }

                    if (!topToken.isInList(actualTop)) {
                        actualTop.add(topToken);
                    }
                }
                if (topTokens.size() != 10 + j){
                    break;
                }
                j++;
            }

            for (int i = 0; i < actualTop.size(); i++){
                String name = "???";
                OfflinePlayer player = actualTop.get(i).getOfflinePlayer();
                if (player.getName() == null){
                    if (loggedPlayers.containsValue(player.getUniqueId().toString())){
                        for (Map.Entry<String, String> entry : loggedPlayers.entrySet()){
                            if (entry.getValue().equals(player.getUniqueId().toString())){
                                name = entry.getKey();
                            }
                        }
                    }
                } else {
                    name = player.getName();
                }
                sender.sendMessage(ChatColor.GOLD + "" + (i + 1) + ". " + ChatColor.AQUA + name + ChatColor.DARK_GRAY + " > " + ChatColor.LIGHT_PURPLE + actualTop.get(i).getTokens());
            }

        } else {
            Map<String, Long> topTokens = sortByValue(tokens);
            int display = 10;
            for (Map.Entry<String, Long> entry : topTokens.entrySet()) {
                if (display > 0) {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(entry.getKey()));
                    String name = null;
                    if (player.getName() == null){
                        if (loggedPlayers.containsValue(entry.getKey())){
                            for (Map.Entry<String, String> entry1 : loggedPlayers.entrySet()){
                                if (entry1.getValue().equals(entry.getKey())){
                                    name = entry1.getKey();
                                    break;
                                }
                            }

                        }
                        if (name == null){
                            continue;
                        }
                    } else {
                        name = player.getName();
                    }

                    if (excludedNames.contains(name.toUpperCase(Locale.ROOT)))
                        continue;
                    sender.sendMessage(ChatColor.GOLD + "" + (11 - display) + ". " + ChatColor.AQUA + name + ChatColor.DARK_GRAY + " > " + ChatColor.LIGHT_PURPLE + entry.getValue());
                    display--;
                } else {
                    break;
                }
            }
        }
        sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "                                              ");
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("token")) {
            List<String> autoComplete = new ArrayList<>();
            if (args.length == 1) {
                if (sender.hasPermission("nottokens.admin")) {
                    autoComplete.add("reload");
                    autoComplete.add("help");
                    autoComplete.add("give");
                    autoComplete.add("giveall");
                    autoComplete.add("remove");
                    autoComplete.add("set");
                }
                if (sender.hasPermission("nottokens.top")) {
                    autoComplete.add("top");
                }
            } else if (args.length == 2){
                if (sender.hasPermission("nottokens.admin")){
                    if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("remove")){
                        for (Player player : Bukkit.getOnlinePlayers()){
                            autoComplete.add(player.getName());
                        }
                    }
                    if (args[0].equalsIgnoreCase("giveall")){
                        autoComplete.add("online");
                        autoComplete.add("offline");
                    }
                }
            }
            String typed = args[args.length-1];
            autoComplete.removeIf(test -> test.toLowerCase(Locale.ROOT).indexOf(typed.toLowerCase(Locale.ROOT)) != 0);
            if (autoComplete.isEmpty()) {
                if (args.length == 1) {
                    if (sender.hasPermission("nottokens.admin")) {
                        for (Map.Entry<String, String> entry : loggedPlayers.entrySet()) {
                            autoComplete.add(entry.getKey());
                        }
                    }
                } else if (args.length == 2) {
                    if (sender.hasPermission("nottokens.admin")) {
                        if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("remove")) {
                            for (Map.Entry<String, String> entry : loggedPlayers.entrySet()) {
                                autoComplete.add(entry.getKey());
                            }
                        }
                    }
                }
            }
            autoComplete.removeIf(test -> test.toLowerCase(Locale.ROOT).indexOf(typed.toLowerCase(Locale.ROOT)) != 0);
            Collections.sort(autoComplete);
            return autoComplete;
        }
        return null;
    }

    // this isn't my code > https://www.geeksforgeeks.org/sorting-a-hashmap-according-to-values/
    // function to sort hashmap by values
    public static Map<String, Long> sortByValue(Map<String, Long> hm)
    {
        // Create a list from elements of HashMap
        List<Map.Entry<String, Long> > list =
                new LinkedList<Map.Entry<String, Long> >(hm.entrySet());

        // Sort the list
        list.sort(new Comparator<Map.Entry<String, Long>>() {
            public int compare(Map.Entry<String, Long> o1,
                               Map.Entry<String, Long> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });

        // put data from sorted list to hashmap
        HashMap<String, Long> temp = new LinkedHashMap<String, Long>();
        for (Map.Entry<String, Long> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }
}
