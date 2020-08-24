package com.opennms.minecraftnoc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class CommandDashboard implements CommandExecutor {
    private final MinecraftNOC plugin;

    public CommandDashboard(MinecraftNOC main) { plugin = main; }

    public boolean onCommand(CommandSender cs, Command c, String label, String[] args) {
        if (cs instanceof Player) {
            Player p = (Player) cs;
            ConfigurationSection boards = plugin.getConfig().getConfigurationSection("dashboards");

            switch(args.length) {
                case 3:
                    if(args[0].contentEquals("add")) {
                        String tag = args[1];
                        String url = args[2];
                        if (boards.contains(tag)) {
                            p.sendMessage(ChatHelper.format("Dashboard " + tag + "exists. Remove it first!"));
                        } else {
                            boards.createSection(tag);
                            boards.set(tag + ".type", "grafana");
                            boards.set(tag + ".path", url);
                            plugin.saveConfig();
                            p.sendMessage(ChatHelper.format("Added dashboard "+args[1]));
                        }
                    }
                    break;
                case 2:
                    if(args[0].contentEquals("remove")) {
                        boards.set(args[1], null);
                        plugin.saveConfig();
                        p.sendMessage(ChatHelper.format("Removed dashboard "+args[1]));
                    } else if(args[0].contentEquals("select")) {
                        String tag = args[1];
                        if(boards.contains(tag)) {
                            String path = boards.getString(tag + ".path");
                            plugin.getGrafanaClient().setDashboard(tag, path);
                            p.sendMessage(ChatHelper.format("Selected dashboard '"+tag+"' with path "+path));
                        } else {
                            p.sendMessage(ChatHelper.format("Dashboard '"+tag+"' not found"));
                        }
                    }
                    break;
                case 1:
                    if(args[0].contentEquals("list")) {
                        boards.getValues(false).forEach((k,v) -> {
                            ConfigurationSection sect = (ConfigurationSection)v;
                            p.sendMessage(k + " (" + sect.get("type") + ") " + sect.get("path"));
                        });
                    }
                    break;
                default:
                    p.sendMessage(ChatHelper.format("dashboard (add|remove|select|list) [nametag] [url]"));
            }
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
