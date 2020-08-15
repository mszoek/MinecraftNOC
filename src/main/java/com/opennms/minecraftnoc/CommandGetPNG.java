package com.opennms.minecraftnoc;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;

public class CommandGetPNG implements CommandExecutor {
    private final GrafanaClientImpl grafanaClient;
    private final MinecraftNOC plugin;

    public CommandGetPNG(MinecraftNOC main) {
        plugin = main;
        grafanaClient = new GrafanaClientImpl(main);
    }

    public boolean onCommand(CommandSender cs, Command c, String label, String[] args) {
        if (cs instanceof Player) {
            Player p = (Player) cs;

            p.sendMessage(ChatHelper.format("Fetching PNG"));
            // get the ID of this frame's map
            ItemFrame frame = (ItemFrame)plugin.getCurrentEntity();
            MapMeta mm = (MapMeta)frame.getItem().getItemMeta();

            Location loc = frame.getLocation();
            ItemStack items = frame.getItem();
            MapMeta meta = (MapMeta)(items.getItemMeta());
            if(meta.hasMapView()) {
                grafanaClient.renderPngForPanel(loc, meta.getMapView(), null, args[0]);
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
