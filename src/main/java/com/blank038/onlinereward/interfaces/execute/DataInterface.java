package com.blank038.onlinereward.interfaces.execute;

import com.blank038.onlinereward.data.PlayerData;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.Statement;

public abstract class DataInterface {

    public abstract void save(PlayerData data, boolean locked);

    public abstract PlayerData get(String name);

    public void connect(ExecuteModel executeModel) {
    }

    public void close(Connection connection, Statement statement) {
    }

    public void setLocked(Player player, boolean locked) {
    }

    public boolean isLocked(Player player) {
        return false;
    }
}