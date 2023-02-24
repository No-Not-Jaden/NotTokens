package me.jadenp.nottokens;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.UUID;

public class TokenPlayer {
    private final OfflinePlayer offlinePlayer;
    private final long tokens;
    public TokenPlayer(UUID uuid, long tokens){
        this.offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        this.tokens = tokens;
    }

    public long getTokens() {
        return tokens;
    }

    public OfflinePlayer getOfflinePlayer() {
        return offlinePlayer;
    }

    public boolean isInList(List<TokenPlayer> list){
        for (TokenPlayer tokenPlayer : list){
            if (tokenPlayer.getOfflinePlayer().getUniqueId().equals(offlinePlayer.getUniqueId())){
                return true;
            }
        }
        return false;
    }
}
