package com.blank038.onlinereward.command;

import com.blank038.onlinereward.OnlineReward;
import com.blank038.onlinereward.api.event.PlayerGetRewardEvent;
import com.blank038.onlinereward.data.DataContainer;
import com.blank038.onlinereward.data.cache.PlayerData;
import com.blank038.onlinereward.data.cache.RewardData;
import com.blank038.onlinereward.gui.RewardGui;
import com.blank038.onlinereward.util.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

/**
 * @author Blank038
 */
public class OnlineRewardCommand implements CommandExecutor {
    private final OnlineReward instance = OnlineReward.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof ConsoleCommandSender || DataContainer.DATA_MAP.containsKey(sender.getName())) {
            if (args.length == 0) {
                this.gottenRewards(sender);
                return false;
            }
            switch (args[0].toLowerCase()) {
                case "reload":
                    this.reloadConfig(sender);
                    break;
                case "open":
                    this.open(sender, args);
                    break;
                case "get":
                    this.get(sender, args);
                    break;
                case "setdaily":
                    this.setDaily(sender, args);
                    break;
                default:
                    break;
            }
        } else {
            sender.sendMessage(OnlineReward.getString("message.pls_wait_sync", true));
        }
        return false;
    }

    private void gottenRewards(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return;
        }
        if (!DataContainer.DATA_MAP.containsKey(sender.getName())) {
            sender.sendMessage(OnlineReward.getString("message.on-sync", true));
            return;
        }
        for (String line : this.instance.getConfig().getStringList("message.reward_info")) {
            if (!line.contains("%reward%")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
                continue;
            }
            if (this.instance.getConfig().getKeys(false).isEmpty() || !DataContainer.DATA_MAP.containsKey(sender.getName())) {
                sender.sendMessage(OnlineReward.getString("message.reward_status.no_reward"));
                continue;
            }
            int online = DataContainer.DATA_MAP.get(sender.getName()).getOnlineTime();
            for (String key : this.instance.getConfig().getConfigurationSection("rewards").getKeys(false)) {
                int need = this.instance.getConfig().getInt("rewards." + key + ".time") - online;
                int day = need / 86400;
                int hour = (need - (day * 86400)) / 3600;
                int minute = (need - (day * 86400) - (hour * 3600)) / 60;
                int second = (need - (day * 86400) - (hour * 3600) - (minute * 60));
                String waitStatus = OnlineReward.getString("message.reward_status.wait").replace("%day%", String.valueOf(day))
                        .replace("%hour%", String.valueOf(hour)).replace("%minute%", String.valueOf(minute))
                        .replace("%second%", String.valueOf(second)),
                        gottonStatus = OnlineReward.getString("message.reward_status.gotten"),
                        name = this.instance.getConfig().getString("rewards." + key + ".name").replace("&", "§");
                sender.sendMessage(OnlineReward.getString("message.reward_line").replace("%name%", name)
                        .replace("%status%", (DataContainer.DATA_MAP.get(sender.getName()).has(key) ? gottonStatus : waitStatus)));
            }
        }
    }

    private void reloadConfig(CommandSender sender) {
        if (sender.hasPermission("onlinereward.admin")) {
            this.instance.loadConfig();
            sender.sendMessage(OnlineReward.getString("message.reload", true));
        } else {
            sender.sendMessage(OnlineReward.getString("message.not-have-perms", true));
        }
    }

    private void open(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            return;
        }
        if (sender.hasPermission("onlinereward.open")) {
            String gui = args.length > 1 ? args[1] : this.instance.getConfig().getString("default-gui");
            new RewardGui((Player) sender, gui);
        } else {
            sender.sendMessage(OnlineReward.getString("message.not-have-perms", true));
        }
    }

    private void get(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) || !DataContainer.DATA_MAP.containsKey(sender.getName())) {
            return;
        }
        if (!sender.hasPermission("onlinereward.open")) {
            sender.sendMessage(OnlineReward.getString("message.not-have-perms", true));
            return;
        }
        if (args.length == 1) {
            sender.sendMessage(OnlineReward.getString("message.pls_enter_reward_key", true));
            return;
        }
        RewardData rewardData = DataContainer.REWARD_DATA_MAP.get(args[1]);
        if (rewardData == null) {
            sender.sendMessage(OnlineReward.getString("message.not_found_reward", true));
            return;
        }
        Player player = (Player) sender;
        PlayerData playerData = DataContainer.DATA_MAP.get(sender.getName());
        int onlineTime = OnlineReward.getApi().getPlayerDayTime(sender.getName()) / 60;
        if (onlineTime >= rewardData.getOnline() && !playerData.hasDayReward(args[1])) {
            int count = (int) Arrays.stream(player.getInventory().getContents())
                    .filter((s) -> s == null || s.getType() == Material.AIR)
                    .count();
            if (count < rewardData.getNeedEmptySlots()) {
                player.sendMessage(OnlineReward.getString("message.need_empty_slots")
                        .replace("%count%", String.valueOf(count)));
                return;
            }
            PlayerGetRewardEvent event = new PlayerGetRewardEvent(player, args[1]);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }
            playerData.addReward(args[1]);
            PlayerUtil.performCommands(player, RewardGui.getCommands(rewardData, player));
            sender.sendMessage(OnlineReward.getString("message.gotten_reward", true));
        } else {
            sender.sendMessage(OnlineReward.getString("message.ara", true));
        }
    }

    private void setDaily(CommandSender sender, String[] args) {
        if (!sender.hasPermission("onlinereward.admin")) {
            return;
        }
        if (args.length == 1) {
            sender.sendMessage(OnlineReward.getString("message.pls-enter-player-name", true));
            return;
        }
        if (args.length == 2 || !args[2].matches("\\d+")) {
            sender.sendMessage(OnlineReward.getString("message.pls-enter-online-value", true));
            return;
        }
        Player player = Bukkit.getPlayerExact(args[1]);
        if (player == null || !player.isOnline() || !DataContainer.DATA_MAP.containsKey(player.getName())) {
            sender.sendMessage(OnlineReward.getString("message.player-offline", true));
            return;
        }
        PlayerData playerData = DataContainer.DATA_MAP.get(player.getName());
        playerData.setDailyOnline(Integer.parseInt(args[2]));
        sender.sendMessage(OnlineReward.getString("message.set-daily", true));
    }
}
