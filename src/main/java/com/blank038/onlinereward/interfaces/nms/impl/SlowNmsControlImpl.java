package com.blank038.onlinereward.interfaces.nms.impl;

import com.blank038.onlinereward.interfaces.nms.BaseNMSControl;
import com.blank038.onlinereward.interfaces.nms.NBTData;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Blank38
 * @since 1.3.9-SNAPSHOT
 */
public class SlowNmsControlImpl extends BaseNMSControl {
    private Method refNMSCopy, refBukkitCopy, refGetTag, refHasKey, refGetStr, refGetInt,
            refSetStr, refSetInt, refSetTag;

    public SlowNmsControlImpl() {
        super();
        try {
            this.refNMSCopy = this.craftItemStack.getDeclaredMethod("asNMSCopy", ItemStack.class);
            this.refBukkitCopy = this.craftItemStack.getDeclaredMethod("asBukkitCopy", this.nmsItem);
            this.refGetTag = this.nmsItem.getMethod("getTag");
            this.refHasKey = this.nbtClass.getMethod("hasKey", String.class);
            this.refGetStr = this.nbtClass.getMethod("getString", String.class);
            this.refGetInt = this.nbtClass.getMethod("getInt", String.class);
            this.refSetStr = this.nbtClass.getMethod("setString", String.class, String.class);
            this.refSetInt = this.nbtClass.getMethod("setInt", String.class, int.class);
            this.refSetTag = this.nmsItem.getMethod("setTag", this.nbtClass);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public NBTData nbtToData(ItemStack itemStack) {
        try {
            Object nmsItem = this.refNMSCopy.invoke(null, itemStack);
            Object nbt = this.refGetTag.invoke(nmsItem);
            if (nbt == null) {
                return new NBTData(null, -1);
            }
            boolean result = (boolean) this.refHasKey.invoke(nbt, "OnlineReward")
                    && (boolean) this.refHasKey.invoke(nbt, "RewardKey");
            if (result) {
                return new NBTData((String) this.refGetStr.invoke(nbt, "RewardKey"),
                        (Integer) this.refGetInt.invoke(nbt, "OnlineReward"));
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return new NBTData(null, -1);
    }

    @Override
    public ItemStack addNbtData(ItemStack itemStack, String key, int amount) {
        try {
            Object nmsItem = this.refNMSCopy.invoke(null, itemStack);
            Object nbt = this.refGetTag.invoke(nmsItem);
            if (nbt == null) {
                nbt = this.nbtClass.newInstance();
            }
            this.refSetInt.invoke(nbt, "OnlineReward", amount);
            this.refSetStr.invoke(nbt, "RewardKey", key);
            this.refSetTag.invoke(nmsItem, nbt);
            return (ItemStack) this.refBukkitCopy.invoke(null, nmsItem);
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
        }
        return itemStack;
    }

    @Override
    public String getImplName() {
        return "Slow";
    }
}
