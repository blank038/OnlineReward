package com.blank038.onlinereward.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Blank038
 */
public class PlayerUtil {

    public static void performCommands(Player player, List<String> commands) {
        commands.stream().filter((c) -> c.startsWith("console:"))
                .map((s) -> s.substring(8))
                .forEach((command) -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
                });
        List<String> opCommands = commands.stream().filter((s) -> !s.startsWith("console:")).collect(Collectors.toList());
        if (opCommands.isEmpty()) {
            return;
        }
        boolean isOp = player.isOp();
        try {
            player.setOp(true);
            for (String command : opCommands) {
                player.performCommand(command.replace("%player%", player.getName()));
            }
        } catch (Exception ignored) {
        } finally {
            player.setOp(isOp);
        }
    }

    public static void performConsoleCommands(Player player, List<String> commands) {
        commands.forEach((command) -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                command.replace("%player%", player.getName())));
    }
}
