package com.opennms.minecraftnoc;

import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class CommandGetMetric implements CommandExecutor {
    private final MetricsClientImpl metricsClient;

    public CommandGetMetric(MinecraftNOC main) {
        FileConfiguration config = main.getConfig();
        metricsClient = new MetricsClientImpl(main);
    }

    public boolean onCommand(CommandSender cs, Command c, String label, String[] args) {
        if (cs instanceof Player) {
            Player p = (Player) cs;

            p.sendMessage(ChatHelper.format("Fetching metric value"));
            metricsClient.getMetric(args[0]);
            return true;
        } else {
            // Sender is console
            if (cs instanceof ConsoleCommandSender) {
                cs.sendMessage("This command can't be run from the console yet!");
            } else if (cs instanceof BlockCommandSender) {
                cs.sendMessage("This command can't be run from command blocks.");
            }
        }

        return false;
    }
}
