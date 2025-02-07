package com.blank038.onlinereward.gui;

import com.aystudio.core.bukkit.util.common.CommonUtil;
import com.aystudio.core.bukkit.util.inventory.GuiModel;
import com.blank038.onlinereward.OnlineReward;
import com.blank038.onlinereward.api.event.PlayerGetRewardEvent;
import com.blank038.onlinereward.data.cache.CommonData;
import com.blank038.onlinereward.data.cache.PlayerData;
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

    public static void open(Player player) {
        if (!CommonData.DATA_MAP.containsKey(player.getName())) {
            return;
        }
        // 获取玩家数据
        PlayerData playerData = CommonData.DATA_MAP.get(player.getName());
        int onlineMinute = playerData.getDailyOnline() / 60;
        // 打开面板
        FileConfiguration data = YamlConfiguration.loadConfiguration(new File(OnlineReward.getInstance().getDataFolder(), "gui.yml"));

        GuiModel model = new GuiModel(data.getString("Inventory.title"), data.getInt("Inventory.size"));
        model.registerListener(OnlineReward.getInstance());
        model.setCloseRemove(true);
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
                int rewardOnlineCondition = section.getInt("online");
                // 获取领取状态
                String status = "";
                if (rewardOnlineCondition > 0) {
                    status = OnlineReward.getString("reward-status." + (onlineMinute >= rewardOnlineCondition
                            ? (playerData.hasDayReward(key) ? "1" : "2") : "3"));
                }
                for (String lore : section.getStringList("lore")) {
                    itemLore.add(PlaceholderHook.format(player, OnlineReward.replaceColor(lore))
                            .replace("%status%", status));
                }

                itemMeta.setLore(itemLore);
                itemStack.setItemMeta(itemMeta);
                if (rewardOnlineCondition > 0) {
                    NBT.modify(itemStack, (nbt) -> {
                        nbt.setString("RewardKey", key);
                        nbt.setInteger("OnlineReward", rewardOnlineCondition);
                    });
                }
                if (section.isList("slots")) {
                    for (int slot : section.getIntegerList("slots")) {
                        model.setItem(slot, itemStack);
                    }
                } else if (section.contains("slots")) {
                    for (int i : CommonUtil.formatSlots(section.getString("slots"))) {
                        model.setItem(i, itemStack);
                    }
                } else {
                    model.setItem(section.getInt("slot"), itemStack);
                }
            }
        }
        model.execute((e) -> {
            e.setCancelled(true);
            if (e.getClickedInventory() == e.getInventory()) {
                ItemStack itemStack = e.getCurrentItem();
                if (itemStack == null || itemStack.getType().equals(Material.AIR)) {
                    return;
                }
                String key = NBT.get(itemStack, (nbt) -> nbt.getString("RewardKey"));
                if (key == null) {
                    return;
                }
                Player clicker = (Player) e.getWhoClicked();
                if (!CommonData.DATA_MAP.containsKey(clicker.getName())) {
                    return;
                }
                int onlineRewardTime = NBT.get(itemStack, (nbt) -> nbt.getInteger("OnlineReward"));
                int onlineTime = OnlineReward.getApi().getPlayerDayTime(clicker.getName()) / 60;
                ConfigurationSection section = data.getConfigurationSection("Items." + key);
                String permission = section.getString("permission");
                if (permission != null && !permission.isEmpty() && !player.hasPermission(permission)) {
                    clicker.sendMessage(OnlineReward.getString("message.permission-denied", true));
                    return;
                }
                if (onlineTime >= onlineRewardTime && !CommonData.DATA_MAP.get(clicker.getName()).hasDayReward(key)) {
                    int count = (int) Arrays.stream(clicker.getInventory().getContents())
                            .filter((s) -> s == null || s.getType() == Material.AIR)
                            .count();
                    if (count < section.getInt("need-empty-slots")) {
                        clicker.sendMessage(OnlineReward.getString("message.need_empty_slots")
                                .replace("%count%", String.valueOf(count)));
                        return;
                    }
                    PlayerGetRewardEvent event = new PlayerGetRewardEvent(clicker, key);
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        return;
                    }
                    CommonData.DATA_MAP.get(clicker.getName()).addReward(key);
                    PlayerUtil.performCommands(clicker, getCommands(section, clicker));
                    clicker.sendMessage(OnlineReward.getString("message.gotten_reward", true));
                } else {
                    clicker.closeInventory();
                    clicker.sendMessage(OnlineReward.getString("message.ara", true));
                }
            }
        });
        model.openInventory(player);
    }

    private static List<String> getCommands(ConfigurationSection section, Player player) {
        if (section.contains("override")) {
            ConfigurationSection overrideSection = section.getConfigurationSection("override");
            List<RewardEntry> entries = overrideSection.getKeys(false).stream()
                    .map((v) -> new RewardEntry(overrideSection.getConfigurationSection(v)))
                    .filter((entry) -> entry.permission == null || player.hasPermission(entry.permission))
                    .collect(Collectors.toList());
            if (entries.isEmpty()) {
                return section.getStringList("commands");
            }
            entries.sort((entry1, entry2) -> Integer.compare(entry2.priority, entry1.priority));
            return entries.get(0).commands;
        }
        return section.getStringList("commands");
    }

    private static Material getMaterial(String name) {
        try {
            return Material.valueOf(name);
        } catch (Exception ignored) {
            OnlineReward.getInstance().getLogger().info("物品类型读取异常: " + name);
            return Material.STONE;
        }
    }

    private static class RewardEntry {
        private final int priority;
        private final String permission;
        private final List<String> commands;

        public RewardEntry(ConfigurationSection section) {
            this.priority = section.getInt("priority");
            this.permission = section.getString("permission");
            this.commands = section.getStringList("commands");
        }
    }
}
