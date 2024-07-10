package com.blank038.onlinereward.hook;

import com.blank038.onlinereward.OnlineReward;
import com.blank038.onlinereward.data.cache.CommonData;
import com.blank038.onlinereward.data.cache.PlayerData;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * @author Blank038
 * @since 1.3.9-SNAPSHOT
 */
public class PlaceholderHook extends PlaceholderExpansion {
    private static PlaceholderHook instance;

    public PlaceholderHook() {
        instance = this;
    }

    @Override
    public String onPlaceholderRequest(Player p, String params) {
        if (p == null || !CommonData.DATA_MAP.containsKey(p.getName())) {
            return "";
        }
        PlayerData playerData = CommonData.DATA_MAP.get(p.getName());
        switch (params) {
            case "day":
                return String.valueOf(playerData.getDailyOnline() / 60);
            case "all":
                return String.valueOf(playerData.getOnlineTime() / 60);
            case "format_day":
                return this.format(playerData.getDailyOnline());
            case "format_all":
                return this.format(playerData.getOnlineTime());
            default:
                break;
        }
        if (params.startsWith("is_gotten_")) {
            return OnlineReward.getString("placeholder.gotten." + playerData.hasDayReward(params.substring(10)));
        }
        return "";
    }

    @Override
    public String getIdentifier() {
        return "onlinereward";
    }

    @Override
    public String getAuthor() {
        return "Blank038";
    }

    @Override
    public String getVersion() {
        return OnlineReward.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    private String format(int time) {
        int day = time / 86400, hour = (time - (day * 86400)) / 3600,
                minute = (time - (day * 86400) - (hour * 3600)) / 60,
                second = (time - (day * 86400) - (hour * 3600) - (minute * 60));
        return OnlineReward.getInstance().getConfig().getString("papi-format").replace("%d%", String.valueOf(day))
                .replace("%h%", String.valueOf(hour)).replace("%m%", String.valueOf(minute)).replace("%s%", String.valueOf(second));
    }

    public static String format(Player player, String line) {
        if (instance == null) {
            return ChatColor.translateAlternateColorCodes('&', line);
        }
        return ChatColor.translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders(player, line));
    }
}
