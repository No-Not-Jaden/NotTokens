package me.jadenp.nottokens;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static me.jadenp.nottokens.ConfigOptions.*;
import static me.jadenp.nottokens.ConfigOptions.loggedPlayers;
import static me.jadenp.nottokens.TokenManager.*;
import static me.jadenp.nottokens.TokenManager.tokens;

public class Events implements Listener {
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        for (TokenRewards rewards : tokenRewards) {
            if (rewards.getType().equals(event.getEntity().getType())) {
                if (rewards.drop())
                    if (event.getEntity().getKiller() != null) {
                        addTokens(event.getEntity().getKiller(), rewards.getAmount());
                        transactions.add("[" + formatExact.format(now) + "] " + event.getEntity().getKiller().getName() + " has received " + rewards.getAmount() + " tokens from killing " + event.getEntity().getName() + ". Total:" + getTokens(event.getEntity().getKiller().getUniqueId()));
                    }
                return;
            }
        }
        if (allMobRewardEnabled) {
            if (Math.random() <= allMobRate) {
                if (event.getEntity().getKiller() != null) {
                    addTokens(event.getEntity().getKiller(), allMobReward);
                    transactions.add("[" + formatExact.format(now) + "] " + event.getEntity().getKiller().getName() + " has received " + allMobReward + " tokens from killing " + event.getEntity().getName() + ". Total:" + getTokens(event.getEntity().getKiller().getUniqueId()));
                }
            }
        }
    }


    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!SQL.isConnected()) {
            if (!tokens.containsKey(event.getPlayer().getUniqueId().toString())) {
                tokens.put(event.getPlayer().getUniqueId().toString(), 0L);
            }
        }
        // check if they are logged yet
        if (!loggedPlayers.containsValue(event.getPlayer().getUniqueId().toString())) {
            // if not, add them
            loggedPlayers.put(event.getPlayer().getName().toLowerCase(Locale.ROOT), event.getPlayer().getUniqueId().toString());
        } else {
            // if they are, check if their username has changed, and update it
            if (!loggedPlayers.containsKey(event.getPlayer().getName().toLowerCase(Locale.ROOT))) {
                String nameToRemove = "";
                for (Map.Entry<String, String> entry : loggedPlayers.entrySet()) {
                    if (entry.getValue().equals(event.getPlayer().getUniqueId().toString())) {
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
}
