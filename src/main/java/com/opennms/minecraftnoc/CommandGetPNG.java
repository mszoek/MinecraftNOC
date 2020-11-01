package com.opennms.minecraftnoc;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;

import java.util.List;
import java.util.Vector;
import java.util.logging.Level;

public class CommandGetPNG implements CommandExecutor {
    private final GrafanaClientImpl grafanaClient;
    private final MinecraftNOC plugin;

    public CommandGetPNG(MinecraftNOC main) {
        plugin = main;
        grafanaClient = main.getGrafanaClient();
    }

    public boolean onCommand(CommandSender cs, Command c, String label, String[] args) {
        if (cs instanceof Player) {
            Player p = (Player) cs;

            // get the cuboid corners
            ItemFrame frame = (ItemFrame)plugin.getCurrentEntityTopLeft();
            Location loc = frame.getLocation();

            ItemFrame frame2 = (ItemFrame)plugin.getCurrentEntityBotRight();
            Location loc2 = frame2.getLocation();

            World world = loc.getWorld();
            List<Integer> mapIDs = new Vector<>();
            int width, height;

            // our selection is only one block thick so either X or Z will be constant
            // figure out which direction the wall goes
            if(loc.getBlockX() == loc2.getBlockX()) {
                int z1 = loc.getBlockZ();
                int z2 = loc2.getBlockZ();
                if(z1 > z2) {
                    getEntitiesAtLocation(mapIDs, loc, loc2, 0, -1);
                } else {
                    getEntitiesAtLocation(mapIDs, loc, loc2, 0, 1);
                }
                width = 1 + Math.max(z1, z2) - Math.min(z1, z2);
            } else {
                int x1 = loc.getBlockX();
                int x2 = loc2.getBlockX();
                if(x1 > x2) {
                    getEntitiesAtLocation(mapIDs, loc, loc2, -1, 0);
                } else {
                    getEntitiesAtLocation(mapIDs, loc, loc2, 1, 0);
                }
                width = 1 + Math.max(x1, x2) - Math.min(x1, x2);
            }
            height = 1 + loc.getBlockY() - loc2.getBlockY();

            if(!mapIDs.isEmpty()) {
                int X = loc.getBlockX();
                int Y = loc.getBlockY();
                int Z = loc.getBlockZ();
                String pos = X + "," + Y + "," + Z;
                String path = "images." + pos;
                FileConfiguration config = plugin.getConfig();
                config.set(path + ".panel", args[0]);
                config.set(path + ".maps", mapIDs);
                config.set(path + ".dashname", grafanaClient.getCurrentDashboardName());
                config.set(path + ".width", width);
                config.set(path + ".height", height);
                plugin.saveConfig();

                p.sendMessage(ChatHelper.format("Fetching PNG"));
                grafanaClient.renderPngForPanel(loc, null, args[0], width, height);
            } else {
                p.sendMessage(ChatHelper.format("No maps found in selection"));
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

    // this is so ugly. Spigot needs a better way to get entities by location.
    public void getEntitiesAtLocation(List<Integer> mapIDs, Location loc, Location loc2, int xInc, int zInc) {
        World world = loc.getWorld();

        if(xInc != 0) {
            int z = loc.getBlockZ();
            for(int x = loc.getBlockX(); xInc > 0 ? x <= loc2.getBlockX() : x >= loc2.getBlockX(); x = x + xInc) {
                for (int y = loc.getBlockY(); y >= loc2.getBlockY(); y--) {
                    Location curloc = new Location(world, (double)x, (double)y, (double)z);
                    filterEntitiesAtBlock(mapIDs, curloc);
                }
            }
        } else {
            int x = loc.getBlockX();
            for (int z = loc.getBlockZ(); zInc > 0 ? z <= loc2.getBlockZ() : z >= loc2.getBlockZ(); z = z + zInc) {
                for (int y = loc.getBlockY(); y >= loc2.getBlockY(); y--) {
                    Location curloc = new Location(world, (double)x, (double)y, (double)z);
                    filterEntitiesAtBlock(mapIDs, curloc);
                }
            }
        }
    }

    public void filterEntitiesAtBlock(List<Integer> mapIDs, Location loc) {
        loc.getWorld().getNearbyEntities(loc, 1.0, 1.0, 1.0).forEach((ent) -> {
            Location entloc = ent.getLocation();
            if(ent.getType() == EntityType.ITEM_FRAME && entloc.getBlockX() == loc.getBlockX() && entloc.getBlockY() == loc.getBlockY() && entloc.getBlockZ() == loc.getBlockZ()) {
                // get the map data for each item frame at this block (should be just one)
                ItemStack items = ((ItemFrame)ent).getItem();
                MapMeta meta = (MapMeta)(items.getItemMeta());
                if(meta.hasMapView()) {
                    mapIDs.add(meta.getMapView().getId());
                }
            }
        });
    }
}
