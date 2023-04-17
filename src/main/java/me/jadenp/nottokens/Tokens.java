package me.jadenp.nottokens;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class Tokens extends PlaceholderExpansion {
    private File tokensHolder;
    private NotTokens plugin;
    public Tokens(NotTokens plugin){
        this.plugin = plugin;
        tokensHolder = new File(plugin.getDataFolder() + File.separator + "tokensHolder.yml");
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

    @Override
    public String onRequest(OfflinePlayer player, String identifier){
        if(player == null){
            return "";
        }
        // %example_placeholder1%
        if(identifier.equals("amount")){
            return TokenManager.getTokens(player.getUniqueId()) + "";
        }

        // %example_placeholder2%


        // We return null if an invalid placeholder (f.e. %example_placeholder3%)
        // was provided
        return null;
    }
}
