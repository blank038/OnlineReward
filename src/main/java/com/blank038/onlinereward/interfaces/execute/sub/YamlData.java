package com.blank038.onlinereward.interfaces.execute.sub;

import com.blank038.onlinereward.Main;
import com.blank038.onlinereward.data.CommonData;
import com.blank038.onlinereward.data.PlayerData;
import com.blank038.onlinereward.interfaces.execute.DataInterface;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

/**
 * @author Blank038
 * @since 1.3.9-SNAPSHOT
 */
public class YamlData extends DataInterface {

    @Override
    public void save(PlayerData pd, boolean locked) {
        try {
            File f = new File(Main.getInstance().getDataFolder() + "/Data/", pd.getName() + ".yml");
            FileConfiguration data = YamlConfiguration.loadConfiguration(f);
            data.set("Time", pd.getOnlineTime());
            data.set("Rewards", pd.getRewards());
            data.set("Day", pd.getDayTime());
            data.set("date", pd.getResetDay());
            data.set("DayRewards", pd.getDayRewards());
            data.save(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public PlayerData get(String name) {
        return CommonData.DATA_MAP.getOrDefault(name, new PlayerData(name));
    }
}