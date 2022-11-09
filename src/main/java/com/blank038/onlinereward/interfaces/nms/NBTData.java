package com.blank038.onlinereward.interfaces.nms;

/**
 * @author Blank038
 */
public class NBTData {
    private final String key;
    private final int amount;

    public NBTData(String key, int amount) {
        this.key = key;
        this.amount = amount;
    }

    public String getKey() {
        return key;
    }

    public int getAmount() {
        return amount;
    }
}