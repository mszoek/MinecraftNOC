package com.opennms.minecraftnoc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.entity.Player;

public class CommandGetPNG implements CommandExecutor {
    private MinecraftNOC plugin;

    public CommandGetPNG(MinecraftNOC main) {
        this.plugin = main;
    }

    public boolean onCommand(CommandSender cs, Command c, String label, String[] args) {
        if (cs instanceof Player) {
            Player p = (Player) cs;

            p.sendMessage(ChatHelper.format("Fetching PNG"));
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
