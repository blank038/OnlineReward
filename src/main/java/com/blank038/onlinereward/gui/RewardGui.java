package com.blank038.onlinereward.gui;

import com.aystudio.core.bukkit.util.common.CommonUtil;
import com.aystudio.core.bukkit.util.inventory.GuiModel;
import com.blank038.onlinereward.OnlineReward;
import com.blank038.onlinereward.api.event.PlayerGetRewardEvent;
import com.blank038.onlinereward.data.DataContainer;
import com.blank038.onlinereward.data.cache.PlayerData;
import com.blank038.onlinereward.data.cache.RewardData;
import com.blank038.onlinereward.hook.PlaceholderHook;
import com.blank038.onlinereward.util.PlayerUtil;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.utils.MinecraftVersion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Blank038
 */
public class RewardGui {

    public static void open(Player player, String guiFile) {
        if (!DataContainer.DATA_MAP.containsKey(player.getName())) {
            return;
        }
        // 打开面板
        File file = new File(OnlineReward.getInstance().getDataFolder() + "/gui", guiFile + ".yml");
        if (!file.exists()) {
            return;
        }
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);
        // 获取玩家数据
        PlayerData playerData = DataContainer.DATA_MAP.get(player.getName());
        int onlineMinute = playerData.getDailyOnline() / 60;
        GuiModel model = new GuiModel(data.getString("Inventory.title"), data.getInt("Inventory.size"));
        model.registerListener(OnlineReward.getInstance());
        model.setCloseRemove(true);
        List<String> rewards = new ArrayList<>();
        if (data.getKeys(false).contains("Items")) {
            for (String key : data.getConfigurationSection("Items").getKeys(false)) {
                // 获取配置节点
                ConfigurationSection section = data.getConfigurationSection("Items." + key);
                ItemStack itemStack = new ItemStack(RewardGui.getMaterial(section.getString("type")), section.getInt("amount"));
                ItemMeta itemMeta = itemStack.getItemMeta();
                if (MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_13_R1)) {
                    ((Damageable) itemMeta).setDamage((short) section.getInt("data"));
                    if (section.contains("custom-data")) {
                        itemMeta.setCustomModelData(section.getInt("custom-data"));
                    }
                } else {
                    itemStack.setDurability((short) section.getInt("data"));
                }
                itemMeta.setDisplayName(OnlineReward.replaceColor(section.getString("name")));
                List<String> itemLore = new ArrayList<>();
                String rewardKey = section.getString("reward");
                RewardData rewardData = DataContainer.REWARD_DATA_MAP.get(rewardKey);
                int rewardOnlineCondition = rewardData == null ? 0 : rewardData.getOnline();
                // 获取领取状态
                String status = "";
                if (rewardOnlineCondition > 0) {
                    status = OnlineReward.getString("reward-status." + (onlineMinute >= rewardOnlineCondition
                            ? (playerData.hasDayReward(rewardKey) ? "1" : "2") : "3"));
                }
                for (String lore : section.getStringList("lore")) {
                    itemLore.add(PlaceholderHook.format(player, OnlineReward.replaceColor(lore))
                            .replace("%status%", status));
                }

                itemMeta.setLore(itemLore);
                itemStack.setItemMeta(itemMeta);
                if (rewardData != null) {
                    NBT.modify(itemStack, (nbt) -> {
                        nbt.setString("RewardKey", rewardKey);
                    });
                    rewards.add(rewardKey);
                }
                if (section.contains("action")) {
                    NBT.modify(itemStack, (nbt) -> {
                        nbt.setString("OnlineRewardAction", section.getString("action"));
                    });
                }
                getSlots(section).forEach((v) -> model.setItem(v, itemStack));
            }
        }
        model.execute((e) -> {
            e.setCancelled(true);
            if (e.getClickedInventory() == e.getInventory()) {
                ItemStack itemStack = e.getCurrentItem();
                if (itemStack == null || itemStack.getType().equals(Material.AIR)) {
                    return;
                }
                Player clicker = (Player) e.getWhoClicked();
                if (!DataContainer.DATA_MAP.containsKey(clicker.getName())) {
                    return;
                }
                String key = NBT.get(itemStack, (nbt) -> nbt.getString("RewardKey"));
                if (key != null) {

                    return;
                }
                String action = NBT.get(itemStack, (nbt) -> nbt.getString("OnlineRewardAction"));
                if (action != null) {
                    switch (action) {
                        case "gotten_all":
                            gottenAll(clicker, rewards);
                            break;
                        default:
                            break;
                    }
                }
            }
        });
        model.openInventory(player);
    }

    private static void gottenReward(Player clicker, String key) {
        RewardData rewardData = DataContainer.REWARD_DATA_MAP.get(key);
        int onlineRewardTime = rewardData.getOnline();
        int onlineTime = OnlineReward.getApi().getPlayerDayTime(clicker.getName()) / 60;
        String permission = rewardData.getPermission();
        if (permission != null && !permission.isEmpty() && !clicker.hasPermission(permission)) {
            clicker.sendMessage(OnlineReward.getString("message.permission-denied", true));
            return;
        }
        if (onlineTime >= onlineRewardTime && !DataContainer.DATA_MAP.get(clicker.getName()).hasDayReward(key)) {
            int count = (int) Arrays.stream(clicker.getInventory().getContents())
                    .filter((s) -> s == null || s.getType() == Material.AIR)
                    .count();
            if (count < rewardData.getNeedEmptySlots()) {
                clicker.sendMessage(OnlineReward.getString("message.need_empty_slots")
                        .replace("%count%", String.valueOf(count)));
                return;
            }
            PlayerGetRewardEvent event = new PlayerGetRewardEvent(clicker, key);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }
            DataContainer.DATA_MAP.get(clicker.getName()).addReward(key);
            PlayerUtil.performCommands(clicker, getCommands(rewardData, clicker));
            clicker.sendMessage(OnlineReward.getString("message.gotten_reward", true));
        } else {
            clicker.closeInventory();
            clicker.sendMessage(OnlineReward.getString("message.ara", true));
        }
    }

    private static void gottenAll(Player clicker, List<String> rewardKeys) {
        rewardKeys.forEach((k) -> gottenReward(clicker, k));
    }

    public static List<String> getCommands(RewardData rewardData, Player player) {
        if (rewardData.getRewardNodes().isEmpty()) {
            return rewardData.getDefaultCommands();
        }
        List<RewardData.RewardNode> nodes = rewardData.getRewardNodes().values().stream()
                .filter((entry) -> entry.getPermission() == null || player.hasPermission(entry.getPermission()))
                .collect(Collectors.toList());
        if (nodes.isEmpty()) {
            return rewardData.getDefaultCommands();
        }
        nodes.sort((entry1, entry2) -> Integer.compare(entry2.getPriority(), entry1.getPriority()));
        return nodes.get(0).getCommands();
    }

    private static Material getMaterial(String name) {
        try {
            return Material.valueOf(name);
        } catch (Exception ignored) {
            OnlineReward.getInstance().getLogger().info("物品类型读取异常: " + name);
            return Material.STONE;
        }
    }

    private static List<Integer> getSlots(ConfigurationSection section) {
        List<Integer> result = new ArrayList<>();
        if (section.isList("slots")) {
            result.addAll(section.getIntegerList("slots"));
        } else if (section.contains("slots")) {
            result.addAll(Arrays.asList(CommonUtil.formatSlots(section.getString("slots"))));
        } else {
            result.add(section.getInt("slot"));
        }
        return result;
    }
}
