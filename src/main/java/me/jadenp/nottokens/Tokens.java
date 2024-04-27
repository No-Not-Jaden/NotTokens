package me.jadenp.nottokens;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class Tokens extends PlaceholderExpansion {
    private NotTokens plugin;
    public Tokens(NotTokens plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean persist(){
        return true;
    }

    @Override
    public boolean canRegister(){
        return true;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "nottokens";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    // %nottokens_amount%
    // %nottokens_top_[x]%
    @Override
    public String onRequest(OfflinePlayer player, String identifier){
        if(player == null){
            return "";
        }
        // %example_placeholder1%
        if(identifier.equals("amount")){
            return TokenManager.getTokens(player.getUniqueId()) + "";
        }
        if (identifier.startsWith("top")) {
            if (identifier.length() > 4) {
                try {
                    int top = Integer.parseInt(identifier.substring(4));
                    if (top > 10 || top < 1) {
                        return "Must be a number 1-10";
                    }
                    List<TokenPlayer> topTokens = TokenManager.getTopTokens();
                    if (topTokens.size() < top)
                        return "";
                    TokenPlayer spot = topTokens.get(top-1);
                    String name = spot.getOfflinePlayer().getName();
                    if (name == null && ConfigOptions.loggedPlayers.containsValue(spot.getOfflinePlayer().getUniqueId().toString())) {
                        for (Map.Entry<String, String> entry : ConfigOptions.loggedPlayers.entrySet()) {
                            if (entry.getValue().equals(spot.getOfflinePlayer().getUniqueId().toString())) {
                                name = entry.getKey();
                                break;
                            }
                        }
                    }
                    return (ChatColor.GOLD + "" + (top) + ". " + ChatColor.AQUA + name + ChatColor.DARK_GRAY + " > " + ChatColor.LIGHT_PURPLE + spot.getTokens());
                } catch (NumberFormatException e) {
                    return "Format Error";
                }
            }
        }

        // %example_placeholder2%


        // We return null if an invalid placeholder (f.e. %example_placeholder3%)
        // was provided
        return null;
    }
}
