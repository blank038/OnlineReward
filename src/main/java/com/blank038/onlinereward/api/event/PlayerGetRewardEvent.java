package com.blank038.onlinereward.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerGetRewardEvent extends Event
        implements Cancellable {
    private final static HandlerList handler = new HandlerList();
    private final Player player;
    private final String rewardKey;
    private boolean cancell;

    public PlayerGetRewardEvent(Player player, String rewardKey) {
        this.player = player;
        this.rewardKey = rewardKey;
    }

    public Player getPlayer() {
        return player;
    }

    public String getRewardKey() {
        return rewardKey;
    }

    @Override
    public HandlerList getHandlers() {
        return handler;
    }

    public HandlerList getHandlerList() {
        return handler;
    }

    @Override
    public boolean isCancelled() {
        return cancell;
    }

    @Override
    public void setCancelled(boolean b) {
        this.cancell = b;
    }
}
