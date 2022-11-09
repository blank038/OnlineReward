package com.blank038.onlinereward.data;

import com.blank038.onlinereward.Main;
import com.blank038.onlinereward.api.event.PlayerGetRewardEvent;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Blank038
 * @since 1.3.9-SNAPSHOT
 */
public class PlayerData {
    private final List<String> rewards, dayRewards;
    private final String name;
    private int time, day;
    private boolean isNew;
    private String resetDay;

    public PlayerData(String name) {
        File f = new File(Main.getInstance().getDataFolder() + "/Data/", name + ".yml");
        FileConfiguration data = YamlConfiguration.loadConfiguration(f);
        this.name = name;
        if (!f.exists()) {
            try {
                f.createNewFile();
                data.set("Time", 0);
                data.set("Rewards", new ArrayList<>());
                data.set("DayRewards", new ArrayList<>());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.rewards = data.getStringList("Rewards");
        this.dayRewards = data.getStringList("DayRewards");
        this.time = data.getInt("Time");
        this.day = data.getInt("Day");
        // 检测时间
        this.resetDay = data.getString("date");
        this.checkRewards();
        this.checkResetDate();
    }

    public PlayerData(String name, JsonObject jsonObject) {
        if (jsonObject.has("isNew")) {
            isNew = jsonObject.get("isNew").getAsBoolean();
        }
        this.name = name;
        this.time = jsonObject.get("time").getAsInt();
        this.day = jsonObject.get("day").getAsInt();
        this.rewards = new ArrayList<>();
        for (JsonElement object : jsonObject.getAsJsonArray("rewards")) {
            rewards.add(object.getAsString());
        }
        this.dayRewards = new ArrayList<>();
        for (JsonElement object : jsonObject.getAsJsonArray("dayRewards")) {
            dayRewards.add(object.getAsString());
        }
        this.resetDay = jsonObject.has("date") ? jsonObject.get("date").getAsString() : null;
        this.checkRewards();
        this.checkResetDate();
    }

    public String getResetDay() {
        return this.resetDay;
    }

    public void setResetDay(String resetDay) {
        this.resetDay = resetDay;
    }

    public void reset() {
        // 设置重置日期
        this.dayRewards.clear();
        this.setDailyOnline(0);
        this.setResetDay(CommonData.DATE_FORMAT.format(new Date(System.currentTimeMillis())));
    }

    public boolean isNew() {
        return isNew;
    }

    public int getDayTime() {
        return day;
    }

    public void setDailyOnline(int dailyOnline) {
        this.day = dailyOnline;
    }

    public int getOnlineTime() {
        return time;
    }

    public void setOnlineTime(int onlineTime) {
        this.time = onlineTime;
    }

    public boolean has(String key) {
        return rewards.contains(key);
    }

    public boolean hasDayReward(String key) {
        return dayRewards.contains(key);
    }

    public void addTime() {
        this.setOnlineTime(this.getOnlineTime() + 1);
        this.setDailyOnline(this.getDayTime() + 1);
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
        if (Main.getInstance().getConfig().contains("rewards")) {
            Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
                ConfigurationSection section = Main.getInstance().getConfig().getConfigurationSection("rewards");
                Player player = this.getPlayer();
                for (String key : section.getKeys(false)) {
                    if (!PlayerData.this.rewards.contains(key) && PlayerData.this.time >= section.getInt(key + ".time")) {
                        PlayerGetRewardEvent event = new PlayerGetRewardEvent(player, key);
                        Bukkit.getPluginManager().callEvent(event);
                        if (event.isCancelled()) {
                            return;
                        }
                        PlayerData.this.rewards.add(key);
                        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                            if (player != null && player.isOnline()) {
                                player.sendMessage(Main.getString("message.receive_award", true)
                                        .replace("%name%", section.getString(key + ".name").replace("&", "§")));
                            }
                            for (String command : section.getStringList(key + ".commands")) {
                                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", PlayerData.this.name));
                            }
                        });
                    }
                }
            });
        }
    }

    public void checkResetDate() {
        Date date = new Date(System.currentTimeMillis());
        if (this.resetDay == null || !CommonData.DATE_FORMAT.format(date).equals(this.resetDay)) {
            this.reset();
            this.save(true);
        }
    }

    public String getName() {
        return name;
    }

    public void save(boolean locked) {
        Main.getInstance().getDataInterface().save(this, locked);
        this.isNew = false;
    }
}
