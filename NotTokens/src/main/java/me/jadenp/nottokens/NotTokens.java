package me.jadenp.nottokens;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.units.qual.A;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public final class NotTokens extends JavaPlugin implements CommandExecutor, Listener {

    File tokensHolder = new File(this.getDataFolder() + File.separator + "tokensHolder.yml");
    public Map<String, Integer> tokens = new HashMap<>();
    public String prefix;
    public ArrayList<String> language = new ArrayList<>();
    File records = new File(this.getDataFolder() + File.separator + "records");
    Date now = new Date();
    SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
    SimpleDateFormat formatExact = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    File today = new File(records + File.separator + format.format(now) + ".txt");
    Map<EntityType, Integer> tokenRewards = new HashMap<>();
    public ArrayList<String> transactions = new ArrayList<>();
    public int condenseSpam;
    Map<String, Long> lastTokenMessage = new HashMap<>();
    Map<String, List<Integer>> tokenChangeQueue = new HashMap<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        Objects.requireNonNull(this.getCommand("token")).setExecutor(this);
        Bukkit.getServer().getPluginManager().registerEvents(this,this);
        this.saveDefaultConfig();
        if (!tokensHolder.exists()){
            try {
                tokensHolder.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
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
                        List<Integer> changes;
                        if (tokenChangeQueue.containsKey(p.getUniqueId().toString())) {
                            changes = tokenChangeQueue.get(p.getUniqueId().toString());
                        } else {
                            break;
                        }
                        int amount = 0;
                        for (Integer i : changes) {
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

    public void loadConfig(){
        this.reloadConfig();
        language.clear();
        if (this.getConfig().getString("prefix") != null)
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

        if (this.getConfig().getConfigurationSection("").getKeys(false).contains("condense-spam")){
            condenseSpam = this.getConfig().getInt("condense-spam");
        } else {
            condenseSpam = -1;
            Bukkit.getLogger().warning("No option found in the config for \"condense-spam\"! Outdated config?");
        }

        tokenRewards.clear();
        int i = 1;
        while (this.getConfig().getString("kill-rewards." + i + ".name") != null){
            String key = this.getConfig().getString("kill-rewards." + i + ".name");
            int tokenAmount = this.getConfig().getInt("kill-rewards." + i + ".amount");
            EntityType entityType = getEntityByName(key);
            if (entityType != null) {
                tokenRewards.put(entityType, tokenAmount);
            } else {
                Bukkit.getLogger().warning("Could not find entity type for \"" + key + "\".");
            }
            i++;
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
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("token")){
            if (sender.hasPermission("nottokens.admin")){
                if (args.length == 0){
                    String text = language.get(0);
                    if (text.contains("{tokens}"))
                        text = text.replace("{tokens}", getTokens((Player) sender) + "");
                    sender.sendMessage(prefix + color(text));
                } else if (args[0].equalsIgnoreCase("reload")){
                    loadConfig();
                    try {
                        saveTokens();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    sender.sendMessage(prefix + ChatColor.GREEN + "Reloaded NotTokens version " + this.getDescription().getVersion() + ".");
                } else if (args[0].equalsIgnoreCase("help")){
                    sender.sendMessage(prefix + ChatColor.GREEN + "Token admin commands:");
                    sender.sendMessage(ChatColor.YELLOW + "/token help" + ChatColor.GOLD + " Displays this");
                    sender.sendMessage(ChatColor.YELLOW + "/token reload" + ChatColor.GOLD + " Reloads this plugin");
                    sender.sendMessage(ChatColor.YELLOW + "/token give (player) (amount)" + ChatColor.GOLD + " Gives a player tokens");
                    sender.sendMessage(ChatColor.YELLOW + "/token remove (player) (amount)" + ChatColor.GOLD + " Removes a player's tokens");
                    sender.sendMessage(ChatColor.YELLOW + "/token set (player) (amount)" + ChatColor.GOLD + " Sets a player's tokens");
                    sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "----------------------------------------");
                } else if (args[0].equalsIgnoreCase("give")){
                    if (args.length >= 2){
                        Player p = Bukkit.getPlayer(args[1]);
                        if (p == null){
                            sender.sendMessage(prefix + color(language.get(6)));
                        } else {
                            if (args.length >= 3){
                                int number = -1;
                                try
                                {
                                        number = Integer.parseInt(args[2]);
                                }
                                catch (NumberFormatException e)
                                {
                                    sender.sendMessage(prefix + ChatColor.GREEN + "Usage: ");
                                    sender.sendMessage(ChatColor.YELLOW + "/token give (player) (amount)" + ChatColor.GOLD + " Gives a player tokens");
                                }
                                if (number != -1){
                                    addTokens(Objects.requireNonNull(p), number);
                                    transactions.add("[" +formatExact.format(now) + "] " + p.getName() + " has received " + number + " tokens from " + sender.getName() + ". Total:" + getTokens(p));
                                    String text = language.get(1);
                                    if (text.contains("{player}")){
                                        text =  text.replace("{player}", Objects.requireNonNull(Bukkit.getPlayer(args[1])).getName());
                                    }
                                    if (text.contains("{tokens")){
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
                } else if (args[0].equalsIgnoreCase("remove")){
                    if (args.length >= 2){
                        Player p = Bukkit.getPlayer(args[1]);
                        if (p == null){
                            sender.sendMessage(prefix + color(language.get(6)));
                        } else {
                            if (args.length >= 3){
                                int number = -1;
                                try
                                {
                                    number = Integer.parseInt(args[2]);
                                }
                                catch (NumberFormatException e)
                                {
                                    sender.sendMessage(prefix + ChatColor.GREEN + "Usage: ");
                                    sender.sendMessage(ChatColor.YELLOW + "/token remove (player) (amount)" + ChatColor.GOLD + " Removes a player's tokens");
                                }
                                if (number != -1){
                                    removeTokens(Objects.requireNonNull(Bukkit.getPlayer(args[1])), number);
                                    transactions.add("[" +formatExact.format(now) + "] " + p.getName() + " has had " + number + " tokens removed from " + sender.getName() + ". Total:" + getTokens(p));
                                    String text = language.get(3);
                                    if (text.contains("{player}")){
                                        text =  text.replace("{player}", Objects.requireNonNull(Bukkit.getPlayer(args[1])).getName());
                                    }
                                    if (text.contains("{tokens")){
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
                } else if (args[0].equalsIgnoreCase("set")){
                    if (args.length >= 2){
                        Player p = Bukkit.getPlayer(args[1]);
                        if (p == null){
                            sender.sendMessage(prefix + color(language.get(6)));
                        } else {
                            if (args.length >= 3){
                                int number = -1;
                                try
                                {
                                    number = Integer.parseInt(args[2]);
                                }
                                catch (NumberFormatException e)
                                {
                                    sender.sendMessage(prefix + ChatColor.GREEN + "Usage: ");
                                    sender.sendMessage(ChatColor.YELLOW + "/token set (player) (amount)" + ChatColor.GOLD + " Sets a player's tokens");
                                }
                                if (number != -1){
                                    setTokens(Objects.requireNonNull(Bukkit.getPlayer(args[1])), number);
                                    transactions.add("[" +formatExact.format(now) + "] " + p.getName() + " has had " + number + " tokens set by " + sender.getName() + ". Total:" + getTokens(p));
                                    String text = language.get(7);
                                    if (text.contains("{player}")){
                                        text =  text.replace("{player}", Objects.requireNonNull(Bukkit.getPlayer(args[1])).getName());
                                    }
                                    if (text.contains("{tokens")){
                                        text = text.replace("{tokens}", number + "");
                                    }
                                    sender.sendMessage(prefix + color(text));
                                    String text2 = language.get(8);
                                    if (text2.contains("{tokens")){
                                        text2 = text2.replace("{tokens}", number + "");
                                    }
                                    Objects.requireNonNull(Bukkit.getPlayer(args[1])).sendMessage(prefix + color(text2));
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
                }
                else {
                    sender.sendMessage(prefix + color(language.get(5)));
                }
            } else {
                String text = language.get(0);
                if (text.contains("{tokens}"))
                    text = text.replace("{tokens}", getTokens((Player) sender) + "");
                sender.sendMessage(prefix + color(text));
            }
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("token")) {
            if (sender.hasPermission("nottokens.admin")) {
                List<String> autoComplete = new ArrayList<>();
                if (args.length == 1) {
                    autoComplete.add("reload");
                    autoComplete.add("help");
                    autoComplete.add("give");
                    autoComplete.add("remove");
                    autoComplete.add("set");
                    return autoComplete;
                }
            }
        }
        return null;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event){
        if (tokenRewards.containsKey(event.getEntity().getType())){
            int tokens = tokenRewards.get(event.getEntity().getType());
            if (event.getEntity().getKiller() != null){
                addTokens(event.getEntity().getKiller(), tokens);
                transactions.add("[" +formatExact.format(now) + "] " + event.getEntity().getKiller().getName() + " has received " + tokens + " tokens from killing " + event.getEntity().getName() + ". Total:" + getTokens(event.getEntity().getKiller()));
            }
        }
    }

    public void addTokens(Player p, int amount){
        tokens.replace(p.getUniqueId().toString(), tokens.get(p.getUniqueId().toString()) + amount);
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
                    List<Integer> changes;
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

    public void setTokens(Player p, int amount){
        tokens.replace(p.getUniqueId().toString(), amount);
    }

    public void removeTokens(Player p, int amount){
        tokens.replace(p.getUniqueId().toString(), tokens.get(p.getUniqueId().toString()) - amount);
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
                    List<Integer> changes;
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

    public int getTokens(Player p){
        return tokens.get(p.getUniqueId().toString());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!tokens.containsKey(event.getPlayer().getUniqueId().toString())){
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(tokensHolder);
            if (configuration.get(event.getPlayer().getUniqueId().toString()) == null){
                tokens.put(event.getPlayer().getUniqueId().toString(), 0);
            } else {
                tokens.put(event.getPlayer().getUniqueId().toString(), configuration.getInt(event.getPlayer().getUniqueId().toString()));
            }
        }
    }

    public void saveTokens() throws IOException {
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(tokensHolder);
        for (Map.Entry<String, Integer> entry : tokens.entrySet()) {
            configuration.set(entry.getKey(), entry.getValue());
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
