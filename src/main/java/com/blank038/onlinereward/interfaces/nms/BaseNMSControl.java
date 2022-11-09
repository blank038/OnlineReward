package com.blank038.onlinereward.interfaces.nms;

import com.blank038.onlinereward.Main;
import com.blank038.onlinereward.data.CommonData;
import com.blank038.onlinereward.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Map;

/**
 * @author Blank038
 * @since 1.3.9-SNAPSHOT
 */
public abstract class BaseNMSControl {
    protected final String version;
    protected Class<?> craftItemStack, nmsItem, nbtClass;

    public BaseNMSControl() {
        this.version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        try {
            this.craftItemStack = Class.forName(String.format("org.bukkit.craftbukkit.%s.inventory.CraftItemStack", this.version));
            this.nmsItem = Class.forName(String.format("net.minecraft.server.%s.ItemStack", this.version));
            this.nbtClass = Class.forName(String.format("net.minecraft.server.%s.NBTTagCompound", this.version));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public abstract NBTData nbtToData(ItemStack itemStack);

    public abstract ItemStack addNbtData(ItemStack itemStack, String key, int amount);

    public void runTask() {
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
    }

    public String getVersion() {
        return this.version;
    }

    public abstract String getImplName();
}
