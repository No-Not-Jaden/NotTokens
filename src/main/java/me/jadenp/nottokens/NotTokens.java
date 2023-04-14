package me.jadenp.nottokens;

import me.jadenp.nottokens.sql.MySQL;
import me.jadenp.nottokens.sql.SQLGetter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

public final class NotTokens extends JavaPlugin implements CommandExecutor, Listener {

    /**
     * auto-fixing config x
     * enabled for kill rewards in config x
     * leaderboard exclusion x
     * tab autocompletes better x
     * optimizing storing tokens x
     *
     * offline set tokens - x
     * /token (player) to get tokens - x
     * added to autocomplete if there is nothing else to autocomplete - x
     * add sql with tokens top - x
     * update database with async & not after every server kill - TEST OUT RN TO SEE IF IT UPDATES x
     *
     * Dont need? vvvv
     * probably use tokens and every so often (like min or so) create a copy, make a connection in async, copy all tokens over,close, and subtract the copy from the main jic there were changes
     * OR you could create a new async every time someone updates their tokens - config option?
     *            ^^^^
     *
     * REMOVE TOKENS IN DB IF 0 -
     * REMOVE ADDING TO DB ON JOIN -
     *
     * papi now supports offline players x
     *
     * add papi placeholders for top 10 nottokens_top_<x>
     *
     * kill rewards for all mobs
     * add a chance for mobs to drop
     */

    File tokensHolder = new File(this.getDataFolder() + File.separator + "tokensHolder.yml");
    public Map<String, Long> tokens = new HashMap<>();
    public String prefix;
    public ArrayList<String> language = new ArrayList<>();
    File records = new File(this.getDataFolder() + File.separator + "records");
    Date now = new Date();
    SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
    SimpleDateFormat formatExact = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    File today = new File(records + File.separator + format.format(now) + ".txt");
    public List<TokenRewards> tokenRewards = new ArrayList<>();
    public int allMobReward;
    public double allMobRate;
    public boolean allMobRewardEnabled;
    public ArrayList<String> transactions = new ArrayList<>();
    public int condenseSpam;
    Map<String, Long> lastTokenMessage = new HashMap<>();
    Map<String, List<Long>> tokenChangeQueue = new HashMap<>();
    public List<String> excludedNames = new ArrayList<>();
    public boolean migrateLocalData;
    public Map<String, String> loggedPlayers = new HashMap<>();

    public MySQL SQL;
    public SQLGetter data;
    private static NotTokens instance;
    public boolean autoConnect;
    private BukkitTask autoConnectTask = null;
    boolean firstStart = true;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;


        Objects.requireNonNull(this.getCommand("token")).setExecutor(this);
        Bukkit.getServer().getPluginManager().registerEvents(this,this);
        this.saveDefaultConfig();

        if (!tokensHolder.exists()){
            try {
                tokensHolder.createNewFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        } else {
            // load tokens from config
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(tokensHolder);
            for (String token : configuration.getKeys(false)){
                if (!token.equals("logged-names"))
                tokens.put(token, configuration.getLong(token));
            }
            // load logged player names
            int i = 0;
            while (configuration.isSet("logged-names." + i + ".uuid")){
                loggedPlayers.put(configuration.getString("logged-names." + i + ".name"), configuration.getString("logged-names." + i + ".uuid"));
                i++;
            }
        }

        if (!records.exists()){
            records.mkdir();
        }
        if (!today.exists()){
            try {
                today.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                Scanner scanner = new Scanner(today);
                while (scanner.hasNextLine()){
                    String data = scanner.nextLine();
                    transactions.add(data);
                }
                scanner.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null){
            new Tokens(this).register();
        }

        loadConfig();

        this.SQL = new MySQL(this);
        this.data = new SQLGetter(this);

        if (!tryToConnect()){
            Bukkit.getLogger().info("[NotTokens] Database not connected, using internal storage");
        }


        new BukkitRunnable(){
            @Override
            public void run() {
                try {
                    saveTokens();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }.runTaskTimerAsynchronously(this,5000,5000);



        new BukkitRunnable(){
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (condenseSpam != -1)
                    if (lastTokenMessage.containsKey(p.getUniqueId().toString()))
                    if ((System.currentTimeMillis() - lastTokenMessage.get(p.getUniqueId().toString())) > condenseSpam * 1000L) {
                        List<Long> changes;
                        if (tokenChangeQueue.containsKey(p.getUniqueId().toString())) {
                            changes = tokenChangeQueue.get(p.getUniqueId().toString());
                        } else {
                            break;
                        }
                        int amount = 0;
                        for (Long i : changes) {
                            amount += i;
                        }
                        if (amount == 0)
                            break;
                        String text = language.get(9);
                        if (text.contains("{tokens}")) {
                            text = text.replace("{tokens}", amount + "");
                        }
                        if (text.contains("{time}")) {
                            text = text.replace("{time}", condenseSpam + "");
                        }
                        p.sendMessage(prefix + color(text));
                        tokenChangeQueue.replace(p.getUniqueId().toString(), new ArrayList<>());
                        lastTokenMessage.replace(p.getUniqueId().toString(), System.currentTimeMillis());
                    }
                }
            }
        }.runTaskTimer(this,40L,20L);
    }

    public static NotTokens getInstance() {
        return instance;
    }

    public boolean tryToConnect(){
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
                if (tokens.size() > 0 && migrateLocalData) {
                    Bukkit.getLogger().info("Migrating local storage to database");
                    // add entries to database
                    for (Map.Entry<String, Long> entry : tokens.entrySet()) {
                        if (entry.getValue() != 0L)
                            data.addTokens(UUID.fromString(entry.getKey()), entry.getValue());
                    }
                    tokens.clear();
                    YamlConfiguration configuration = new YamlConfiguration();
                    try {
                        configuration.save(tokensHolder);
                    } catch (IOException e) {
                        e.printStackTrace();
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


    public void loadConfig(){
        this.reloadConfig();

        // adding parts to config if they are missing
        if (this.getConfig().get("kill-rewards.enabled") == null){
            this.getConfig().set("kill-rewards.enabled", false);
        }
        if (!this.getConfig().isSet("condense-spam")){
            this.getConfig().set("condense-spam", -1);
        }
        if (!this.getConfig().isSet("leaderboard-exclusion")){
            this.getConfig().set("leaderboard-exclusion", Collections.emptyList());
        }
        if (!this.getConfig().isSet("prefix")){
            this.getConfig().set("prefix", "&7[&b&lNot&d&lTokens&7] &8➟ &r");
        }
        if (!this.getConfig().isSet("balance")){
            this.getConfig().set("balance", "&aYou have {tokens} tokens.");
        }
        if (!this.getConfig().isSet("admin-add")){
            this.getConfig().set("admin-add", "&aYou have given {player} {tokens} tokens.");
        }
        if (!this.getConfig().isSet("player-receive")){
            this.getConfig().set("player-receive", "&aYou have received {tokens} tokens.");
        }
        if (!this.getConfig().isSet("admin-remove")){
            this.getConfig().set("admin-remove", "&aYou have removed {tokens} tokens from {player}.");
        }
        if (!this.getConfig().isSet("player-take")){
            this.getConfig().set("player-take", "&a{tokens} tokens have been removed from your account.");
        }
        if (!this.getConfig().isSet("admin-set")){
            this.getConfig().set("admin-set", "&a{player} now has {tokens} tokens.");
        }
        if (!this.getConfig().isSet("player-set")){
            this.getConfig().set("player-set", "&aYour tokens were set to {tokens}.");
        }
        if (!this.getConfig().isSet("unknown-command")){
            this.getConfig().set("unknown-command", "&cUnknown Command! /token help");
        }
        if (!this.getConfig().isSet("unknown-player")){
            this.getConfig().set("unknown-player", "&cUnknown Player!");
        }
        if (!this.getConfig().isSet("reduced-message")){
            this.getConfig().set("reduced-message", "&aYour tokens have changed by {tokens} in the last {time} seconds.");
        }
        if (!this.getConfig().isSet("other-tokens")){
            this.getConfig().set("other-tokens", "&a{player} has {tokens} tokens.");
        }
        if (!this.getConfig().isSet("database.host")){
            this.getConfig().set("database.host", "localhost");
        }
        if (!this.getConfig().isSet("database.port")){
            this.getConfig().set("database.port", "3306");
        }
        if (!this.getConfig().isSet("database.database")){
            this.getConfig().set("database.database", "db");
        }
        if (!this.getConfig().isSet("database.user")){
            this.getConfig().set("database.user", "username");
        }
        if (!this.getConfig().isSet("database.password")){
            this.getConfig().set("database.password", "");
        }
        if (!this.getConfig().isSet("database.use-ssl")){
            this.getConfig().set("database.use-ssl", false);
        }
        if (!this.getConfig().isSet("database.migrate-local-data")){
            this.getConfig().set("database.migrate-local-data", true);
        }
        if (!this.getConfig().isSet("admin-give-all")){
            this.getConfig().set("admin-give-all", "&aYou have given {amount} players {tokens} tokens.");
        }
        if (!this.getConfig().isSet("database.auto-connect")){
            this.getConfig().set("database.auto-connect", false);
        }
        if (!this.getConfig().isSet("kill-rewards.all-mobs.enabled")){
            this.getConfig().set("kill-rewards.all-mobs.enabled", false);
        }
        if (!this.getConfig().isSet("kill-rewards.all-mobs.amount")){
            this.getConfig().set("kill-rewards.all-mobs.amount", 1);
        }
        if (!this.getConfig().isSet("kill-rewards.all-mobs.rate")){
            this.getConfig().set("kill-rewards.all-mobs.rate", 0.1);
        }
        this.saveConfig();

        allMobRewardEnabled = this.getConfig().getBoolean("kill-rewards.all-mobs.enabled");
        allMobReward = this.getConfig().getInt("kill-rewards.all-mobs.amount");
        allMobRate = this.getConfig().getDouble("kill-rewards.all-mobs.rate");

        language.clear();

        prefix = color(this.getConfig().getString("prefix"));
        //0 balance
        language.add(this.getConfig().getString("balance"));
        //1 admin-add
        language.add(this.getConfig().getString("admin-add"));
        //2 player-receive
        language.add(this.getConfig().getString("player-receive"));
        //3 admin-remove
        language.add(this.getConfig().getString("admin-remove"));
        //4 player-take
        language.add(this.getConfig().getString("player-take"));
        //5 unknown-command
        language.add(this.getConfig().getString("unknown-command"));
        //6 unknown-player
        language.add(this.getConfig().getString("unknown-player"));
        //7 admin-set
        language.add(this.getConfig().getString("admin-set"));
        //8 player-set
        language.add(this.getConfig().getString("player-set"));
        //9 reduced-message
        language.add(this.getConfig().getString("reduced-message"));
        //10 other-tokens
        language.add(this.getConfig().getString("other-tokens"));
        //11 admin-give-all
        language.add(this.getConfig().getString("admin-give-all"));

        if (this.getConfig().getConfigurationSection("").getKeys(false).contains("condense-spam")){
            condenseSpam = this.getConfig().getInt("condense-spam");
        } else {
            condenseSpam = -1;
            Bukkit.getLogger().warning("No option found in the config for \"condense-spam\"! Outdated config?");
        }

        excludedNames = this.getConfig().getStringList("leaderboard-exclusion");
        excludedNames.replaceAll(str -> str.toUpperCase(Locale.ROOT));

        tokenRewards.clear();
        if (this.getConfig().getBoolean("kill-rewards.enabled")) {
            int i = 1;
            while (this.getConfig().getString("kill-rewards." + i + ".name") != null) {
                String key = this.getConfig().getString("kill-rewards." + i + ".name");
                int tokenAmount = this.getConfig().getInt("kill-rewards." + i + ".amount");
                double rate = 1;
                if (this.getConfig().isSet("kill-rewards." + i + ".rate")){
                    rate = this.getConfig().getDouble("kill-rewards." + i + ".rate");
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

        migrateLocalData = this.getConfig().getBoolean("database.migrate-local-data");
        autoConnect = this.getConfig().getBoolean("database.auto-connect");
        if (autoConnectTask != null){
            autoConnectTask.cancel();
        }
        if (autoConnect){
            autoConnectTask = new BukkitRunnable(){
                @Override
                public void run() {
                    tryToConnect();
                }
            }.runTaskTimer(this, 600, 600);
        }


    }

    public EntityType getEntityByName(String name) {
        for (EntityType type : EntityType.values()) {
            if(type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }

    public String color(String s){
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        try {
            saveTokens();
        } catch (IOException e) {
            e.printStackTrace();
        }

        SQL.disconnect();

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
                        saveTokens();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    sender.sendMessage(prefix + ChatColor.GREEN + "Reloaded NotTokens version " + this.getDescription().getVersion() + ".");
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
                    if (args.length == 1){
                        // migrate notTokens data to sql

                    } else {
                        if (args[1].equalsIgnoreCase("tokenManager")){
                            if (!Bukkit.getPluginManager().isPluginEnabled("TokenManager")){
                                // plugin not found
                                sender.sendMessage(prefix + "Plugin not found!");
                                return true;
                            }

                            FileConfiguration config = Bukkit.getPluginManager().getPlugin("TokenManager").getConfig();
                        }
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

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event){
        for (TokenRewards rewards : tokenRewards){
            if (rewards.getType().equals(event.getEntity().getType())){
                if (rewards.drop())
                    if (event.getEntity().getKiller() != null){
                        addTokens(event.getEntity().getKiller(), rewards.getAmount());
                        transactions.add("[" +formatExact.format(now) + "] " + event.getEntity().getKiller().getName() + " has received " + rewards.getAmount() + " tokens from killing " + event.getEntity().getName() + ". Total:" + getTokens(event.getEntity().getKiller().getUniqueId()));
                    }
                return;
            }
        }
        if (allMobRewardEnabled){
            if (Math.random() <= allMobRate){
                if (event.getEntity().getKiller() != null){
                    addTokens(event.getEntity().getKiller(), allMobReward);
                    transactions.add("[" +formatExact.format(now) + "] " + event.getEntity().getKiller().getName() + " has received " + allMobReward + " tokens from killing " + event.getEntity().getName() + ". Total:" + getTokens(event.getEntity().getKiller().getUniqueId()));
                }
            }
        }
    }

    public void addTokens(OfflinePlayer p, long amount){
        if (SQL.isConnected()){
            data.addTokens(p.getUniqueId(), amount);
        } else {
            if (tokens.containsKey(p.getUniqueId().toString())) {
                tokens.replace(p.getUniqueId().toString(), getTokens(p.getUniqueId()) + amount);
            } else {
                tokens.put(p.getUniqueId().toString(), amount);
            }
        }
    }

    public void addTokens(Player p, long amount){
        if (SQL.isConnected()){
            data.addTokens(p.getUniqueId(), amount);
        } else {
            if (tokens.containsKey(p.getUniqueId().toString())) {
                tokens.replace(p.getUniqueId().toString(), getTokens(p.getUniqueId()) + amount);
            } else {
                tokens.put(p.getUniqueId().toString(), amount);
            }
        }

        if (condenseSpam == -1){
            String text2 = language.get(2);
            if (text2.contains("{tokens}")) {
                text2 = text2.replace("{tokens}", amount + "");
            }
            p.sendMessage(prefix + color(text2));
        } else {
            if (lastTokenMessage.containsKey(p.getUniqueId().toString())) {
                if (((System.currentTimeMillis() - lastTokenMessage.get(p.getUniqueId().toString())) > condenseSpam * 1000L)) {
                    String text2 = language.get(2);
                    if (text2.contains("{tokens}")) {
                        text2 = text2.replace("{tokens}", amount + "");
                    }
                    p.sendMessage(prefix + color(text2));
                    lastTokenMessage.replace(p.getUniqueId().toString(), System.currentTimeMillis());
                } else {
                    List<Long> changes;
                    if (tokenChangeQueue.containsKey(p.getUniqueId().toString())) {
                        changes = tokenChangeQueue.get(p.getUniqueId().toString());
                        changes.add(amount);
                        tokenChangeQueue.replace(p.getUniqueId().toString(), changes);
                    } else {
                        changes = new ArrayList<>();
                        changes.add(amount);
                        tokenChangeQueue.put(p.getUniqueId().toString(), changes);
                    }
                }
            } else {
                String text2 = language.get(2);
                if (text2.contains("{tokens}")) {
                    text2 = text2.replace("{tokens}", amount + "");
                }
                p.sendMessage(prefix + color(text2));
                lastTokenMessage.put(p.getUniqueId().toString(), System.currentTimeMillis());
            }
        }
    }

    public void setTokens(OfflinePlayer p, long amount){
        if (SQL.isConnected()){
            data.setToken(p.getUniqueId(), amount);
        } else {
            if (tokens.containsKey(p.getUniqueId().toString())) {
                tokens.replace(p.getUniqueId().toString(), amount);
            } else {
                tokens.put(p.getUniqueId().toString(), amount);
            }
        }
    }

    public void setTokens(Player p, long amount){
        if (SQL.isConnected()){
            data.setToken(p.getUniqueId(), amount);
        } else {
            if (tokens.containsKey(p.getUniqueId().toString())) {
                tokens.replace(p.getUniqueId().toString(), amount);
            } else {
                tokens.put(p.getUniqueId().toString(), amount);
            }
        }

        String text2 = language.get(8);
        if (text2.contains("{tokens")) {
            text2 = text2.replace("{tokens}", amount + "");
        }
        p.sendMessage(prefix + color(text2));
    }

    public void removeTokens(OfflinePlayer p, long amount){
        if (SQL.isConnected()){
            data.removeTokens(p.getUniqueId(), amount);
        } else {
            if (tokens.containsKey(p.getUniqueId().toString())) {
                tokens.replace(p.getUniqueId().toString(), getTokens(p.getUniqueId()) - amount);
            } else {
                tokens.put(p.getUniqueId().toString(), -amount);
            }
        }
    }

    public void removeTokens(Player p, long amount){
        if (SQL.isConnected()){
            data.removeTokens(p.getUniqueId(), amount);
        } else {
            if (tokens.containsKey(p.getUniqueId().toString())) {
                tokens.replace(p.getUniqueId().toString(), getTokens(p.getUniqueId()) - amount);
            } else {
                tokens.put(p.getUniqueId().toString(), -amount);
            }
        }
        if (condenseSpam == -1){
            String text2 = language.get(4);
            if (text2.contains("{tokens}")) {
                text2 = text2.replace("{tokens}", amount + "");
            }
            p.sendMessage(prefix + color(text2));
        } else {
            if (lastTokenMessage.containsKey(p.getUniqueId().toString())) {
                if (((System.currentTimeMillis() - lastTokenMessage.get(p.getUniqueId().toString())) > condenseSpam * 1000L)) {
                    String text2 = language.get(4);
                    if (text2.contains("{tokens}")) {
                        text2 = text2.replace("{tokens}", amount + "");
                    }
                    p.sendMessage(prefix + color(text2));
                    lastTokenMessage.replace(p.getUniqueId().toString(), System.currentTimeMillis());
                } else {
                    List<Long> changes;
                    if (tokenChangeQueue.containsKey(p.getUniqueId().toString())) {
                        changes = tokenChangeQueue.get(p.getUniqueId().toString());
                        changes.add(-amount);
                        tokenChangeQueue.replace(p.getUniqueId().toString(), changes);
                    } else {
                        changes = new ArrayList<>();
                        changes.add(-amount);
                        tokenChangeQueue.put(p.getUniqueId().toString(), changes);
                    }
                }
            } else {
                String text2 = language.get(4);
                if (text2.contains("{tokens}")) {
                    text2 = text2.replace("{tokens}", amount + "");
                }
                p.sendMessage(prefix + color(text2));
                lastTokenMessage.put(p.getUniqueId().toString(), System.currentTimeMillis());
            }
        }
    }

    public long getTokens(UUID uuid){
        if (SQL.isConnected()){
            return data.getTokens(uuid);
        }
        if (tokens.containsKey(uuid.toString())){
            return tokens.get(uuid.toString());
        }
        return 0L;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!SQL.isConnected()){
            if (!tokens.containsKey(event.getPlayer().getUniqueId().toString())){
                tokens.put(event.getPlayer().getUniqueId().toString(), 0L);
            }
        }
        // check if they are logged yet
        if (!loggedPlayers.containsValue(event.getPlayer().getUniqueId().toString())){
            // if not, add them
            loggedPlayers.put(event.getPlayer().getName().toLowerCase(Locale.ROOT), event.getPlayer().getUniqueId().toString());
        } else {
            // if they are, check if their username has changed, and update it
            if (!loggedPlayers.containsKey(event.getPlayer().getName().toLowerCase(Locale.ROOT))){
                String nameToRemove = "";
                for (Map.Entry<String, String> entry : loggedPlayers.entrySet()) {
                    if (entry.getValue().equals(event.getPlayer().getUniqueId().toString())){
                        nameToRemove = entry.getKey();
                    }
                }
                if (!Objects.equals(nameToRemove, "")) {
                    loggedPlayers.remove(nameToRemove);
                }
                loggedPlayers.put(event.getPlayer().getName().toLowerCase(Locale.ROOT), event.getPlayer().getUniqueId().toString());
            }
        }

    }

    public void saveTokens() throws IOException {
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(tokensHolder);
        if (!SQL.isConnected()) {
            for (Map.Entry<String, Long> entry : tokens.entrySet()) {
                configuration.set(entry.getKey(), entry.getValue());
            }
        }
        int i = 0;
        for (Map.Entry<String, String> entry : loggedPlayers.entrySet()) {
            configuration.set("logged-names." + i + ".name" , entry.getKey());
            configuration.set("logged-names." + i + ".uuid" , entry.getValue());
            i++;
        }
        configuration.save(tokensHolder);
        try{
            PrintWriter writer = new PrintWriter(today.getPath(), "UTF-8");
            for (String s : transactions){
                writer.println(s);
            }
            writer.close();
        } catch (IOException e) {
            // do something
        }

    }
}
