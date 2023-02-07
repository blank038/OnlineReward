package com.blank038.onlinereward.command;

import com.blank038.onlinereward.Main;
import com.blank038.onlinereward.data.CommonData;
import com.blank038.onlinereward.gui.RewardGui;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

/**
 * @author Blank038
 */
public class OnlineRewardCommand implements CommandExecutor {
    private final Main instance = Main.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof ConsoleCommandSender || CommonData.DATA_MAP.containsKey(sender.getName())) {
            if (args.length == 0) {
                if (!(sender instanceof Player)) {
                    return false;
                }
                if (CommonData.DATA_MAP.containsKey(sender.getName())) {
                    for (String line : this.instance.getConfig().getStringList("message.reward_info")) {
                        if (line.contains("%reward%")) {
                            if (this.instance.getConfig().getKeys(false).size() == 0 || !CommonData.DATA_MAP.containsKey(sender.getName())) {
                                sender.sendMessage(Main.getString("message.reward_status.no_reward"));
                                continue;
                            }
                            int online = CommonData.DATA_MAP.get(sender.getName()).getOnlineTime();
                            for (String key : this.instance.getConfig().getConfigurationSection("rewards").getKeys(false)) {
                                int need = this.instance.getConfig().getInt("rewards." + key + ".time") - online;
                                int day = need / 86400;
                                int hour = (need - (day * 86400)) / 3600;
                                int minute = (need - (day * 86400) - (hour * 3600)) / 60;
                                int second = (need - (day * 86400) - (hour * 3600) - (minute * 60));
                                String waitStatus = Main.getString("message.reward_status.wait").replace("%day%", String.valueOf(day))
                                        .replace("%hour%", String.valueOf(hour)).replace("%minute%", String.valueOf(minute))
                                        .replace("%second%", String.valueOf(second)),
                                        gottonStatus = Main.getString("message.reward_status.gotten"),
                                        name = this.instance.getConfig().getString("rewards." + key + ".name").replace("&", "§");
                                sender.sendMessage(Main.getString("message.reward_line").replace("%name%", name)
                                        .replace("%status%", (CommonData.DATA_MAP.get(sender.getName()).has(key) ? gottonStatus : waitStatus)));
                            }
                            continue;
                        }
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
                    }
                } else {
                    sender.sendMessage(Main.getString("message.on-sync", true));
                }
            } else if ("reload".equalsIgnoreCase(args[0]) && sender.hasPermission("onlinereward.admin")) {
                this.instance.loadConfig();
                sender.sendMessage(Main.getString("message.reload", true));
            } else if ("open".equalsIgnoreCase(args[0])) {
                RewardGui.open((Player) sender);
            }
        } else {
            sender.sendMessage(Main.getString("message.pls_wait_sync", true));
        }
        return false;
    }
}
