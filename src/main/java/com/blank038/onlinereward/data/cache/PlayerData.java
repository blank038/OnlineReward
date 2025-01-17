package com.blank038.onlinereward.data.cache;

import com.blank038.onlinereward.OnlineReward;
import com.blank038.onlinereward.api.event.PlayerGetRewardEvent;
import com.blank038.onlinereward.util.PlayerUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Blank038
 */
public class PlayerData {
    private final List<String> rewards, dayRewards;
    private final String name;
    private int onlineTotal, dailyOnline;
    private LocalDate resetDate;
    private boolean isNew;

    public PlayerData(String name) {
        File f = new File(OnlineReward.getInstance().getDataFolder() + "/Data/", name + ".yml");
        FileConfiguration data = YamlConfiguration.loadConfiguration(f);
        this.name = name;
        this.rewards = data.getStringList("Rewards");
        this.dayRewards = data.getStringList("DayRewards");
        this.onlineTotal = data.getInt("Time");
        this.dailyOnline = data.getInt("Day");
        // 检测时间
        this.initializeDate(data.getLong("resetDate", System.currentTimeMillis()));
        this.checkResetDate();
    }

    public PlayerData(String name, JsonObject jsonObject) {
        if (jsonObject.has("isNew")) {
            isNew = jsonObject.get("isNew").getAsBoolean();
        }
        this.name = name;
        this.onlineTotal = jsonObject.get("time").getAsInt();
        this.dailyOnline = jsonObject.get("day").getAsInt();
        this.rewards = new ArrayList<>();
        for (JsonElement object : jsonObject.getAsJsonArray("rewards")) {
            rewards.add(object.getAsString());
        }
        this.dayRewards = new ArrayList<>();
        for (JsonElement object : jsonObject.getAsJsonArray("dayRewards")) {
            dayRewards.add(object.getAsString());
        }
        this.initializeDate(jsonObject.has("resetDate") ? jsonObject.get("resetDate").getAsLong() : 0);
        this.checkResetDate();
    }

    private void initializeDate(long time) {
        Instant instant = Instant.ofEpochMilli(time);
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.systemDefault());
        this.resetDate = zonedDateTime.toLocalDate();
    }

    public long getResetDate() {
        LocalDateTime localDateTime = this.resetDate.atStartOfDay();
        Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        return instant.toEpochMilli();
    }

    public boolean isNew() {
        return isNew;
    }

    public int getDailyOnline() {
        return dailyOnline;
    }

    public void setDailyOnline(int dailyOnline) {
        this.dailyOnline = dailyOnline;
    }

    public int getOnlineTime() {
        return onlineTotal;
    }

    public void setOnlineTime(int onlineTime) {
        this.onlineTotal = onlineTime;
    }

    public boolean has(String key) {
        return rewards.contains(key);
    }

    public boolean hasDayReward(String key) {
        return dayRewards.contains(key);
    }

    public void addTime() {
        this.setOnlineTime(this.getOnlineTime() + 1);
        this.setDailyOnline(this.getDailyOnline() + 1);
    }

    public void addReward(String key) {
        dayRewards.add(key);
    }

    public List<String> getDayRewards() {
        return dayRewards;
    }

    public List<String> getRewards() {
        return rewards;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(name);
    }

    public void checkRewards() {
        if (OnlineReward.getInstance().getConfig().contains("rewards")) {
            ConfigurationSection section = OnlineReward.getInstance().getConfig().getConfigurationSection("rewards");
            Player player = this.getPlayer();
            for (String key : section.getKeys(false)) {
                if (!PlayerData.this.rewards.contains(key) && PlayerData.this.onlineTotal >= section.getInt(key + ".time")) {
                    PlayerGetRewardEvent event = new PlayerGetRewardEvent(player, key);
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        return;
                    }
                    PlayerData.this.rewards.add(key);
                    if (player != null && player.isOnline()) {
                        player.sendMessage(OnlineReward.getString("message.receive_award", true)
                                .replace("%name%", section.getString(key + ".name").replace("&", "§")));
                    }
                    PlayerUtil.performConsoleCommands(player, section.getStringList(key + ".commands"));
                }
            }
        }
    }

    public void checkResetDate() {
        LocalDate now = LocalDate.now();
        if (this.resetDate.isBefore(now)) {
            this.resetDate = now;
            CompletableFuture.supplyAsync(() -> {
                this.save(true);
                return true;
            }).thenAcceptAsync(b -> {
                if (b) {
                    this.dayRewards.clear();
                    this.setDailyOnline(0);
                    this.save(true);
                }
            });
        }
    }

    public String getName() {
        return name;
    }

    public void save(boolean locked) {
        OnlineReward.getInstance().getDataInterface().save(this, locked);
        this.isNew = false;
    }
}
