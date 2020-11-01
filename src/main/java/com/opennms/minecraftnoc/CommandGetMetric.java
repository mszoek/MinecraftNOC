package com.opennms.minecraftnoc;

import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class CommandGetMetric implements CommandExecutor {
    private final MinecraftNOC plugin;

    public CommandGetMetric(MinecraftNOC main) {
        FileConfiguration config = main.getConfig();
        plugin = main;
    }

    public boolean onCommand(CommandSender cs, Command c, String label, String[] args) {
        if (cs instanceof Player) {
            Player p = (Player) cs;

            if(args.length < 3) {
                p.sendMessage(ChatHelper.format("Not enough parameters"));
                return true;
            }

            String title = args[1];
            String url;
            String filter;

            // this should be in the form: type, title, args

            switch(args[0]) {
                case "custom":
                    url = args[2];
                    filter = args[3];
                    break;
                case "nodesnmp":
                    if(args.length < 4) {
                        p.sendMessage(ChatHelper.format("Not enough parameters"));
                        return true;
                    }
                    url = "measurements/node%5B" + args[2] + "%5D.nodeSnmp%5B%5D/" + args[3];
                    filter = "/columns/0/values/46";
                    break;
                case "ifsnmp":
                    if(args.length < 5) {
                        p.sendMessage(ChatHelper.format("Not enough parameters"));
                        return true;
                    }
                    url = "measurements/node%5B" + args[2] + "%5D.interfaceSnmp%5B" + args[3] + "%5D/" + args[4];
                    filter = "/columns/0/values/46";
                    break;
                default:
                    p.sendMessage(ChatHelper.format("Unknown metric type"));
                    return true;
            }

            p.sendMessage(ChatHelper.format("Fetching " + args[0] + " metric value"));
            plugin.getMetricsClient().getMetric(title, url, filter, plugin.getCurrentBlock());
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
