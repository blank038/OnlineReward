package com.blank038.onlinereward.api;

import com.blank038.onlinereward.OnlineReward;
import com.blank038.onlinereward.data.DataContainer;
import com.blank038.onlinereward.data.cache.PlayerData;

/**
 * @author Blank038
 * @since 1.3.9-SNAPSHOT
 */
public class OnlineRewardAPI {
    private static OnlineRewardAPI instance;

    public OnlineRewardAPI() {
        instance = this;
    }

    public int getPlayerOnlineTime(String name) {
        return (DataContainer.DATA_MAP.containsKey(name) ? DataContainer.DATA_MAP.get(name).getOnlineTime() : this.getPlayerData(name, "Time"));
    }

    public int getPlayerDayTime(String name) {
        return (DataContainer.DATA_MAP.containsKey(name) ? DataContainer.DATA_MAP.get(name).getDailyOnline() : this.getPlayerData(name, "Day"));
    }

    public int getPlayerData(String name, String key) {
        PlayerData data = OnlineReward.getInstance().getDataInterface().get(name);
        switch (key) {
            case "Day":
                return data.getDailyOnline();
            case "Time":
                return data.getOnlineTime();
            default:
                return 0;
        }
    }

    public static OnlineRewardAPI getInstance() {
        return instance;
    }
}