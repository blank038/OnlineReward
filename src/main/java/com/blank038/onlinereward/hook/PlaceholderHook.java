package com.blank038.onlinereward.hook;

import com.blank038.onlinereward.Main;
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
        switch (params) {
            case "day":
                return String.valueOf(Main.getApi().getPlayerDayTime(p.getName()) / 60);
            case "all":
                return String.valueOf(Main.getApi().getPlayerOnlineTime(p.getName()) / 60);
            case "format_day":
                return this.format(Main.getApi().getPlayerDayTime(p.getName()));
            case "format_all":
                return this.format(Main.getApi().getPlayerOnlineTime(p.getName()));
            default:
                break;
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
        return Main.getInstance().getDescription().getVersion();
    }

    private String format(int time) {
        int day = time / 86400, hour = (time - (day * 86400)) / 3600,
                minute = (time - (day * 86400) - (hour * 3600)) / 60,
                second = (time - (day * 86400) - (hour * 3600) - (minute * 60));
        return Main.getInstance().getConfig().getString("papi-format").replace("%d%", String.valueOf(day))
                .replace("%h%", String.valueOf(hour)).replace("%m%", String.valueOf(minute)).replace("%s%", String.valueOf(second));
    }

    public static String format(Player player, String line) {
        if (instance == null) {
            return ChatColor.translateAlternateColorCodes('&', line);
        }
        return ChatColor.translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders(player, line));
    }
}
