package com.blank038.onlinereward.interfaces.nms.impl;

import com.blank038.onlinereward.interfaces.nms.BaseNMSControl;
import com.blank038.onlinereward.interfaces.nms.NBTData;
import com.esotericsoftware.reflectasm.ConstructorAccess;
import com.esotericsoftware.reflectasm.MethodAccess;
import org.bukkit.inventory.ItemStack;

/**
 * @author Blank038
 * @since 2021-07-30
 */
public class CommonNmsControlImpl extends BaseNMSControl {
    private final SlowNmsControlImpl slowNmsControl = new SlowNmsControlImpl();
    private final MethodAccess cratItemAccess, nmsAccess, nbtAccess;
    private final ConstructorAccess<?> nbtConstructor;


    public CommonNmsControlImpl() {
        super();
        // 初始化反射
        this.cratItemAccess = MethodAccess.get(this.craftItemStack);
        this.nmsAccess = MethodAccess.get(this.nmsItem);
        this.nbtAccess = MethodAccess.get(nbtClass);
        // 初始化构造器
        this.nbtConstructor = ConstructorAccess.get(this.nbtClass);
    }

    @Override
    public NBTData nbtToData(ItemStack itemStack) {
        try {
            Object nmsItem = this.cratItemAccess.invoke(null, "asNMSCopy", itemStack);
            Object nbt = this.nmsAccess.invoke(nmsItem, "getTag");
            if (nbt == null) {
                return new NBTData(null, -1);
            }
            boolean result = (boolean) this.nbtAccess.invoke(nbt, "hasKey", "OnlineReward")
                    && (boolean) this.nbtAccess.invoke(nbt, "hasKey", "RewardKey");
            if (result) {
                return new NBTData((String) this.nbtAccess.invoke(nbt, "getString", "RewardKey"),
                        (Integer) this.nbtAccess.invoke(nbt, "getInt", "OnlineReward"));
            }
            return new NBTData(null, -1);
        } catch (Exception ignored) {
            return this.slowNmsControl.nbtToData(itemStack);
        }
    }

    @Override
    public ItemStack addNbtData(ItemStack itemStack, String key, int amount) {
        try {
            Object nmsItem = this.cratItemAccess.invoke(null, "asNMSCopy", itemStack);
            Object nbt = this.nmsAccess.invoke(nmsItem, "getTag");
            if (nbt == null) {
                nbt = this.nbtConstructor.newInstance();
            }

            this.nbtAccess.invoke(nbt, "setInt", "OnlineReward", amount);
            this.nbtAccess.invoke(nbt, "setString", "RewardKey", key);
            this.nmsAccess.invoke(nmsItem, "setTag", nbt);
            return (ItemStack) this.cratItemAccess.invoke(null, "asBukkitCopy", nmsItem);
        } catch (Exception ignored) {
            return this.slowNmsControl.addNbtData(itemStack, key, amount);
        }
    }

    @Override
    public String getImplName() {
        return "Common";
    }
}