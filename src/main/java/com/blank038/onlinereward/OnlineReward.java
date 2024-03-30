package com.blank038.onlinereward;

import com.blank038.onlinereward.api.OnlineRewardAPI;
import com.blank038.onlinereward.command.OnlineRewardCommand;
import com.blank038.onlinereward.data.DataContainer;
import com.blank038.onlinereward.data.cache.CommonData;
import com.blank038.onlinereward.data.cache.PlayerData;
import com.blank038.onlinereward.hook.PlaceholderHook;
import com.blank038.onlinereward.interfaces.execute.DataInterface;
import com.blank038.onlinereward.interfaces.execute.sub.MySQLData;
import com.blank038.onlinereward.interfaces.execute.sub.YamlData;
import com.blank038.onlinereward.listener.PlayerListener;
import de.tr7zw.nbtapi.utils.MinecraftVersion;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Blank038
 */
public class OnlineReward extends JavaPlugin {
    private static final Pattern PATTERN = Pattern.compile("#[A-f0-9]{6}");
    private static OnlineReward instance;
    private static OnlineRewardAPI orApi;
    private DataInterface dataInterface;

    public static OnlineReward getInstance() {
        return instance;
    }

    public static OnlineRewardAPI getApi() {
        return orApi;
    }

    public DataInterface getDataInterface() {
        return dataInterface;
    }

    @Override
    public void onEnable() {
        instance = this;
        orApi = new OnlineRewardAPI();
        this.loadConfig();
        // 判断存储类型, 初始化存储对象
        dataInterface = "MYSQL".equalsIgnoreCase(getConfig().getString("save-option.type")) ? new MySQLData() : new YamlData();
        Bukkit.getScheduler().runTaskTimer(OnlineReward.getInstance(), () -> {
            CommonData.DATA_MAP.forEach((key, value) -> {
                value.addTime();
                value.checkRewards();
                value.checkResetDate();
            });
        }, 20L, 20L);
        Bukkit.getScheduler().runTaskTimerAsynchronously(OnlineReward.getInstance(), () -> CommonData.DATA_MAP.forEach((k, v) -> v.save(true)), 1200L, 1200L);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderHook().register();
        }
        if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_16_R1)) {
            DataContainer.legacyVersion = false;
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
        this.reloadConfig();
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
    }

    public static String replaceColor(String message) {
        return DataContainer.legacyVersion ? ChatColor.translateAlternateColorCodes('&', message) : formatHexColor(message);
    }

    public static String getString(String key, boolean... prefix) {
        String message = instance.getConfig().getString(key, "");
        if (prefix.length > 0 && prefix[0]) {
            message = instance.getConfig().getString("message.prefix") + message;
        }
        return OnlineReward.replaceColor(message);
    }

    private static String formatHexColor(String message) {
        String copy = message;
        Matcher matcher = PATTERN.matcher(copy);
        while (matcher.find()) {
            String color = message.substring(matcher.start(), matcher.end());
            copy = copy.replace(color, String.valueOf(ChatColor.of(color)));
        }
        return ChatColor.translateAlternateColorCodes('&', copy);
    }
}
