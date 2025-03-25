package com.blank038.onlinereward.data;

import com.blank038.onlinereward.OnlineReward;
import com.blank038.onlinereward.data.cache.PlayerData;
import com.blank038.onlinereward.data.cache.RewardData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Blank038
 */
public class DataContainer {
    public static final Map<String, PlayerData> DATA_MAP = new ConcurrentHashMap<>();
    public static final Map<String, RewardData> REWARD_DATA_MAP = new HashMap<>();
    public static boolean legacyVersion = true;

    public static void init() {
        REWARD_DATA_MAP.clear();
        File folder = new File(OnlineReward.getInstance().getDataFolder(), "rewards");
        if (!folder.exists()) {
            OnlineReward.getInstance().saveResource("rewards/example.yml", "rewards/example.yml");
        }
        for (File file : folder.listFiles()) {
            if (!file.getName().endsWith(".yml")) {
                continue;
            }
            FileConfiguration data = YamlConfiguration.loadConfiguration(file);
            for (String key : data.getKeys(false)) {
                REWARD_DATA_MAP.put(key, new RewardData(data.getConfigurationSection(key)));
            }
        }
    }
}
