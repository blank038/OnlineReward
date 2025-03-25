package com.blank038.onlinereward.data.cache;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class RewardData {
    private final int online, needEmptySlots;
    private final String permission;
    private final List<String> defaultCommands;
    private final Map<String, RewardNode> rewardNodes = new HashMap<>();

    public RewardData(ConfigurationSection section) {
        this.online = section.getInt("online");
        this.needEmptySlots = section.getInt("need-empty-slots");
        this.permission = section.getString("permission");
        this.defaultCommands = section.getStringList("commands");
        if (section.contains("override")) {
            ConfigurationSection overrideSection = section.getConfigurationSection("override");
            overrideSection.getKeys(false).forEach((v) -> rewardNodes.put(v, new RewardNode(overrideSection.getConfigurationSection(v))));
        }
    }

    @Getter
    public static class RewardNode {
        private final String permission;
        private final int priority;
        private final List<String> commands;

        public RewardNode(ConfigurationSection section) {
            this.permission = section.getString("permission");
            this.priority = section.getInt("priority");
            this.commands = section.getStringList("commands");
        }
    }
}
