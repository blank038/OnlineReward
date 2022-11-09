package com.blank038.onlinereward.gui;

import com.aystudio.core.bukkit.util.common.CommonUtil;
import com.aystudio.core.bukkit.util.inventory.GuiModel;
import com.blank038.onlinereward.Main;
import com.blank038.onlinereward.api.event.PlayerGetRewardEvent;
import com.blank038.onlinereward.data.CommonData;
import com.blank038.onlinereward.data.PlayerData;
import com.blank038.onlinereward.hook.PlaceholderHook;
import com.blank038.onlinereward.interfaces.nms.NBTData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
        // 打开面板
        FileConfiguration data = YamlConfiguration.loadConfiguration(new File(Main.getInstance().getDataFolder(), "gui.yml"));

        GuiModel model = new GuiModel(data.getString("Inventory.title"), data.getInt("Inventory.size"));
        model.registerListener(Main.getInstance());
        model.setCloseRemove(true);
        if (data.getKeys(false).contains("Items")) {
            int onlineTime = playerData.getDayTime() / 60;
            for (String key : data.getConfigurationSection("Items").getKeys(false)) {
                ItemStack itemStack = new ItemStack(RewardGui.getMaterial(data.getString("Items." + key + ".type")),
                        data.getInt("Items." + key + ".amount"), (short) data.getInt("Items." + key + ".data"));
                ItemMeta itemMeta = itemStack.getItemMeta();
                itemMeta.setDisplayName(data.getString("Items." + key + ".name").replace("&", "§"));
                List<String> itemLore = new ArrayList<>();
                int onlineKey = data.getInt("Items." + key + ".online");
                // 获取领取状态
                String status = "";
                if (onlineKey > 0) {
                    status = Main.getString("reward-status." + (onlineTime >= onlineKey
                            ? (playerData.hasDayReward(key) ? "1" : 2) : "3"));
                }
                for (String lore : data.getStringList("Items." + key + ".lore")) {
                    itemLore.add(PlaceholderHook.format(player, lore.replace("&", "§"))
                            .replace("%status%", status));
                }
                itemMeta.setLore(itemLore);
                itemStack.setItemMeta(itemMeta);
                if (onlineKey > 0) {
                    itemStack = Main.getInstance().nmsModel.addNbtData(itemStack, key, onlineKey);
                }
                if (data.isList("Items." + key + ".slots")) {
                    for (int slot : data.getIntegerList("Items." + key + ".slots")) {
                        model.setItem(slot, itemStack);
                    }
                } else if (data.contains("Items." + key + ".slots")) {
                    for (int i : CommonUtil.formatSlots(data.getString("Items." + key + ".slots"))) {
                        model.setItem(i, itemStack);
                    }
                } else {
                    model.setItem(data.getInt("Items." + key + ".slot"), itemStack);
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
                NBTData nbtTagCompound = Main.getInstance().nmsModel.nbtToData(itemStack);
                // 判断该物品是否有对应的NBT数据
                if (nbtTagCompound.getKey() != null) {
                    Player clicker = (Player) e.getWhoClicked();
                    if (CommonData.DATA_MAP.containsKey(clicker.getName())) {
                        int onlineRewardTime = nbtTagCompound.getAmount();
                        int onlineTime = Main.getApi().getPlayerDayTime(clicker.getName()) / 60;
                        String key = nbtTagCompound.getKey(), permission = Main.getInstance().guiData.getString("Items." + key + ".permission");
                        if (permission != null && !permission.isEmpty() && !player.hasPermission(permission)) {
                            clicker.sendMessage(Main.getString("message.permission-denied", true));
                            return;
                        }
                        if (onlineTime >= onlineRewardTime && !CommonData.DATA_MAP.get(clicker.getName()).hasDayReward(key)) {
                            PlayerGetRewardEvent event = new PlayerGetRewardEvent(clicker, key);
                            Bukkit.getPluginManager().callEvent(event);
                            if (event.isCancelled()) {
                                return;
                            }
                            CommonData.DATA_MAP.get(clicker.getName()).addReward(key);
                            boolean isOp = clicker.isOp();
                            try {
                                clicker.setOp(true);
                                for (String command : Main.getInstance().guiData.getStringList("Items." + key + ".commands")) {
                                    clicker.performCommand(command.replace("%player%", clicker.getName()));
                                }
                            } catch (Exception ignored) {
                            } finally {
                                clicker.setOp(isOp);
                            }
                            clicker.sendMessage(Main.getString("message.gotten_reward", true));
                        } else {
                            clicker.closeInventory();
                            clicker.sendMessage(Main.getString("message.ara", true));
                        }
                    }
                }
            }
        });
        model.openInventory(player);
    }

    private static Material getMaterial(String name) {
        try {
            return Material.valueOf(name);
        } catch (Exception ignored) {
            Main.getInstance().getLogger().info("物品类型读取异常: " + name);
            return Material.STONE;
        }
    }
}
