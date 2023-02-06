package com.blank038.onlinereward;

import com.blank038.onlinereward.api.OnlineRewardAPI;
import com.blank038.onlinereward.command.OnlineRewardCommand;
import com.blank038.onlinereward.data.CommonData;
import com.blank038.onlinereward.data.PlayerData;
import com.blank038.onlinereward.hook.PlaceholderHook;
import com.blank038.onlinereward.interfaces.execute.DataInterface;
import com.blank038.onlinereward.interfaces.execute.sub.MySQLData;
import com.blank038.onlinereward.interfaces.execute.sub.YamlData;
import com.blank038.onlinereward.listener.PlayerListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashSet;
import java.util.Map;

/**
 * @author Blank038
 */
public class Main extends JavaPlugin {
    private static Main main;
    private static OnlineRewardAPI orApi;
    public DataInterface dataInterface;
    public FileConfiguration guiData;

    public static Main getInstance() {
        return main;
    }

    public static OnlineRewardAPI getApi() {
        return orApi;
    }

    public DataInterface getDataInterface() {
        return dataInterface;
    }

    @Override
    public void onEnable() {
        main = this;
        orApi = new OnlineRewardAPI();
        this.loadConfig();
        // 判断存储类型, 初始化存储对象
        dataInterface = "MYSQL".equalsIgnoreCase(getConfig().getString("save-option.type")) ? new MySQLData() : new YamlData();
        Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getInstance(), () -> {
            for (Map.Entry<String, PlayerData> entry : new HashSet<>(CommonData.DATA_MAP.entrySet())) {
                entry.getValue().addTime();
                entry.getValue().checkRewards();
                entry.getValue().checkResetDate();
            }
        }, 20L, 20L);
        Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getInstance(), () -> {
            for (Map.Entry<String, PlayerData> entry : new HashSet<>(CommonData.DATA_MAP.entrySet())) {
                entry.getValue().save(true);
            }
        }, 1200L, 1200L);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderHook().register();
        }
        // 注册命令
        this.getCommand("or").setExecutor(new OnlineRewardCommand());
    }

    @Override
    public void onDisable() {
        for (PlayerData pd : CommonData.DATA_MAP.values()) {
            pd.save(false);
        }
    }

    public void loadConfig() {
        this.saveDefaultConfig();
        File data = new File(getDataFolder(), "Data");
        if (!data.exists()) {
            data.mkdir();
        }
        if (CommonData.DATA_MAP.isEmpty()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                CommonData.DATA_MAP.put(player.getName(), new PlayerData(player.getName()));
            }
        }
        File gui = new File(getDataFolder(), "gui.yml");
        if (!gui.exists()) {
            this.saveResource("gui.yml", true);
        }
        this.guiData = YamlConfiguration.loadConfiguration(gui);
        this.reloadConfig();
    }

    public static String getString(String key, boolean... prefix) {
        return ChatColor.translateAlternateColorCodes('&',
                (prefix.length > 0 && prefix[0] ? main.getConfig().getString("message.prefix") : "")
                        + main.getConfig().getString(key, ""));
    }
}
