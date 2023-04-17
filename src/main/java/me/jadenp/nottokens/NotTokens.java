package me.jadenp.nottokens;

import me.jadenp.nottokens.sql.MySQL;
import me.jadenp.nottokens.sql.SQLGetter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;

import static me.jadenp.nottokens.ConfigOptions.*;
import static me.jadenp.nottokens.TokenManager.*;

public final class NotTokens extends JavaPlugin {

    /**
     * auto-fixing config x
     * enabled for kill rewards in config x
     * leaderboard exclusion x
     * tab autocompletes better x
     * optimizing storing tokens x
     * <p>
     * offline set tokens - x
     * /token (player) to get tokens - x
     * added to autocomplete if there is nothing else to autocomplete - x
     * add sql with tokens top - x
     * update database with async & not after every server kill - TEST OUT RN TO SEE IF IT UPDATES x
     * <p>
     * Dont need? vvvv
     * probably use tokens and every so often (like min or so) create a copy, make a connection in async, copy all tokens over,close, and subtract the copy from the main jic there were changes
     * OR you could create a new async every time someone updates their tokens - config option?
     * ^^^^
     * <p>
     * REMOVE TOKENS IN DB IF 0 -
     * REMOVE ADDING TO DB ON JOIN -
     * <p>
     * papi now supports offline players x
     * <p>
     * add papi placeholders for top 10 nottokens_top_<x>
     * <p>
     * kill rewards for all mobs -
     * add a chance for mobs to drop -
     * <p>
     * Migrate tokens from TokenManager
     * add are you sure
     * separate java files
     * replace msgs with language
     */

    File tokensHolder = new File(this.getDataFolder() + File.separator + "tokensHolder.yml");

    File records = new File(this.getDataFolder() + File.separator + "records");

    File today = new File(records + File.separator + format.format(now) + ".txt");

    private static NotTokens instance;


    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;


        Objects.requireNonNull(this.getCommand("token")).setExecutor(new Commands());
        Bukkit.getServer().getPluginManager().registerEvents(new Events(), this);
        this.saveDefaultConfig();

        if (!tokensHolder.exists()) {
            try {
                tokensHolder.createNewFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        } else {
            // load tokens from config
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(tokensHolder);
            for (String token : configuration.getKeys(false)) {
                if (!token.equals("logged-names"))
                    tokens.put(token, configuration.getLong(token));
            }
            // load logged player names
            int i = 0;
            while (configuration.isSet("logged-names." + i + ".uuid")) {
                loggedPlayers.put(configuration.getString("logged-names." + i + ".name"), configuration.getString("logged-names." + i + ".uuid"));
                i++;
            }
        }

        if (!records.exists()) {
            records.mkdir();
        }
        if (!today.exists()) {
            try {
                today.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                Scanner scanner = new Scanner(today);
                while (scanner.hasNextLine()) {
                    String data = scanner.nextLine();
                    transactions.add(data);
                }
                scanner.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new Tokens(this).register();
        }

        loadConfig();

        SQL = new MySQL(this);
        data = new SQLGetter(this);

        if (!tryToConnect()) {
            Bukkit.getLogger().info("[NotTokens] Database not connected, using internal storage");
        }


        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    saveTokens();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }.runTaskTimerAsynchronously(this, 5000, 5000);


        new BukkitRunnable() {
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
        }.runTaskTimer(this, 40L, 20L);
    }

    public static NotTokens getInstance() {
        return instance;
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




    public void saveTokens() throws IOException {
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(tokensHolder);
        if (!SQL.isConnected()) {
            for (Map.Entry<String, Long> entry : tokens.entrySet()) {
                configuration.set(entry.getKey(), entry.getValue());
            }
        }
        int i = 0;
        for (Map.Entry<String, String> entry : loggedPlayers.entrySet()) {
            configuration.set("logged-names." + i + ".name", entry.getKey());
            configuration.set("logged-names." + i + ".uuid", entry.getValue());
            i++;
        }
        configuration.save(tokensHolder);
        try {
            PrintWriter writer = new PrintWriter(today.getPath(), "UTF-8");
            for (String s : transactions) {
                writer.println(s);
            }
            writer.close();
        } catch (IOException e) {
            // do something
        }

    }
}
