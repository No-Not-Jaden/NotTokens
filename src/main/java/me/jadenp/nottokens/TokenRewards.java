package me.jadenp.nottokens;

import org.bukkit.entity.EntityType;

public class TokenRewards {
    private final EntityType type;
    private final int amount;
    private final double rate;

    public TokenRewards(EntityType type, int amount, double rate){

        this.type = type;
        this.amount = amount;
        this.rate = rate;
    }

    public int getAmount() {
        return amount;
    }

    public double getRate() {
        return rate;
    }

    public EntityType getType() {
        return type;
    }

    public boolean drop(){
        return Math.random() <= rate;
    }
}
