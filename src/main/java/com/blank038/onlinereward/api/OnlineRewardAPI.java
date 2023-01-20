package com.blank038.onlinereward.api;

import com.blank038.onlinereward.Main;
import com.blank038.onlinereward.data.CommonData;
import com.blank038.onlinereward.data.PlayerData;

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
        return (CommonData.DATA_MAP.containsKey(name) ? CommonData.DATA_MAP.get(name).getOnlineTime() : this.getPlayerData(name, "Time"));
    }

    public int getPlayerDayTime(String name) {
        return (CommonData.DATA_MAP.containsKey(name) ? CommonData.DATA_MAP.get(name).getDailyOnline() : this.getPlayerData(name, "Day"));
    }

    public int getPlayerData(String name, String key) {
        PlayerData data = Main.getInstance().getDataInterface().get(name);
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