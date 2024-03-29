package me.jadenp.nottokens;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;

import static me.jadenp.nottokens.ConfigOptions.*;

public class TokenManager {
    public static ArrayList<String> transactions = new ArrayList<>();
    public static Map<String, Long> lastTokenMessage = new HashMap<>();
    public static Map<String, List<Long>> tokenChangeQueue = new HashMap<>();
    public static Map<String, Long> tokens = new HashMap<>();
    public static List<TokenRewards> tokenRewards = new ArrayList<>();

    public static void addTokens(OfflinePlayer p, long amount){
        if (SQL.isConnected()){
            data.addTokens(p.getUniqueId(), amount);
        } else {
            if (tokens.containsKey(p.getUniqueId().toString())) {
                tokens.replace(p.getUniqueId().toString(), getTokens(p.getUniqueId()) + amount);
            } else {
                tokens.put(p.getUniqueId().toString(), amount);
            }
        }

        if (!p.isOnline())
            return;
        Player player = p.getPlayer();
        assert player != null;
        if (condenseSpam == -1){
            String text2 = language.get(2);
            if (text2.contains("{tokens}")) {
                text2 = text2.replace("{tokens}", amount + "");
            }
            player.sendMessage(prefix + color(text2, p));
        } else {
            if (lastTokenMessage.containsKey(p.getUniqueId().toString())) {
                if (((System.currentTimeMillis() - lastTokenMessage.get(p.getUniqueId().toString())) > condenseSpam * 1000L)) {
                    String text2 = language.get(2);
                    if (text2.contains("{tokens}")) {
                        text2 = text2.replace("{tokens}", amount + "");
                    }
                    player.sendMessage(prefix + color(text2, p));
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
                player.sendMessage(prefix + color(text2, p));
                lastTokenMessage.put(p.getUniqueId().toString(), System.currentTimeMillis());
            }
        }
    }

    public static void setTokens(OfflinePlayer p, long amount){
        if (SQL.isConnected()){
            data.setToken(p.getUniqueId(), amount);
        } else {
            if (tokens.containsKey(p.getUniqueId().toString())) {
                tokens.replace(p.getUniqueId().toString(), amount);
            } else {
                tokens.put(p.getUniqueId().toString(), amount);
            }
        }

        if (p.isOnline()) {
            String text2 = language.get(8);
            if (text2.contains("{tokens")) {
                text2 = text2.replace("{tokens}", amount + "");
            }
            Objects.requireNonNull(p.getPlayer()).sendMessage(prefix + color(text2, p));
        }
    }



    public static boolean removeTokens(OfflinePlayer p, long amount){
        if (!negativeTokens && getTokens(p.getUniqueId()) < amount) {
            if (p.isOnline())
                Objects.requireNonNull(p.getPlayer()).sendMessage(prefix + color(language.get(12), p));
            return false;
        }
        if (SQL.isConnected()){
            data.removeTokens(p.getUniqueId(), amount);
        } else {
            if (tokens.containsKey(p.getUniqueId().toString())) {
                tokens.replace(p.getUniqueId().toString(), getTokens(p.getUniqueId()) - amount);
            } else {
                tokens.put(p.getUniqueId().toString(), -amount);
            }
        }
        if (!p.isOnline())
            return true;
        Player player = p.getPlayer();
        assert player != null;
        if (condenseSpam == -1){
            String text2 = language.get(4);
            if (text2.contains("{tokens}")) {
                text2 = text2.replace("{tokens}", amount + "");
            }
            player.sendMessage(prefix + color(text2, p));
        } else {
            if (lastTokenMessage.containsKey(p.getUniqueId().toString())) {
                if (((System.currentTimeMillis() - lastTokenMessage.get(p.getUniqueId().toString())) > condenseSpam * 1000L)) {
                    String text2 = language.get(4);
                    if (text2.contains("{tokens}")) {
                        text2 = text2.replace("{tokens}", amount + "");
                    }
                    player.sendMessage(prefix + color(text2, p));
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
                player.sendMessage(prefix + color(text2, p));
                lastTokenMessage.put(p.getUniqueId().toString(), System.currentTimeMillis());
            }
        }
        return true;
    }

    public static long getTokens(UUID uuid){
        if (SQL.isConnected()){
            return data.getTokens(uuid);
        }
        if (tokens.containsKey(uuid.toString())){
            return tokens.get(uuid.toString());
        }
        return 0L;
    }

}
