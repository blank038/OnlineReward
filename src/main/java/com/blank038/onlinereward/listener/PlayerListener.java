package com.blank038.onlinereward.listener;

import com.aystudio.core.bukkit.thread.BlankThread;
import com.aystudio.core.bukkit.thread.ThreadProcessor;
import com.blank038.onlinereward.OnlineReward;
import com.blank038.onlinereward.data.cache.CommonData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * @author Blank038
 * @since 1.3.9-SNAPSHOT
 */
public class PlayerListener implements Listener {
    private final OnlineReward main;

    public PlayerListener(OnlineReward main) {
        this.main = main;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        if (CommonData.DATA_MAP.containsKey(e.getPlayer().getName())) {
            Bukkit.getScheduler().runTaskAsynchronously(this.main, () -> CommonData.DATA_MAP.remove(e.getPlayer().getName()).save(false));
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (main.getConfig().getBoolean("save-option.pull-notify")) {
            e.getPlayer().sendMessage(OnlineReward.getString("message.sync_pull", true));
        }
        // 创建线程
        ThreadProcessor.crateTask(main, new BlankThread(10) {
            private int count;

            @Override
            public void run() {
                Player player = e.getPlayer();
                if (!main.getDataInterface().isLocked(player)) {
                    loadData(player);
                    this.cancel();
                } else {
                    count++;
                    if (count > main.getConfig().getInt("save-option.time-out")) {
                        loadData(player);
                        this.cancel();
                    }
                }
            }
        });
    }

    private void loadData(Player player) {
        main.getDataInterface().setLocked(player, true);
        CommonData.DATA_MAP.put(player.getName(), main.getDataInterface().get(player.getName()));
        if (main.getConfig().getBoolean("save-option.pull-notify")) {
            player.sendMessage(OnlineReward.getString("message.sync_finish", true));
        }
    }
}
