package me.jadenp.nottokens;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;

import static me.jadenp.nottokens.Commands.sortByValue;
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

    public static List<TokenPlayer> getTopTokens() {
        List<TokenPlayer> actualTop = new LinkedList<>();
        if (SQL.isConnected()) {
            List<TokenPlayer> topTokens;
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
                    actualTop.add(new TokenPlayer(UUID.fromString(entry.getKey()), entry.getValue()));
                    display--;
                } else {
                    break;
                }
            }
        }
        return actualTop;
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
