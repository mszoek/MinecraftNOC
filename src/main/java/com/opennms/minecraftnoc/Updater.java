package com.opennms.minecraftnoc;

// Worker thread to periodically refresh the metrics and map images

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.Collection;
import java.util.Objects;
import java.util.logging.Level;

public class Updater implements Runnable {
    private MinecraftNOC plugin;
    private FileConfiguration config;
    private boolean shouldRun = true;
    private int interval;

    public Updater(MinecraftNOC main) {
        plugin = main;
        config = plugin.getConfig();
        interval = Integer.parseInt(Objects.requireNonNull(config.get("update.interval")).toString());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this);
    }

    public void stop() { shouldRun = false; }

    public void run() {
        World world = plugin.getServer().getWorlds().get(0);

        while(shouldRun) {
            ConfigurationSection signs = config.getConfigurationSection("signs");
            for(String key : signs.getKeys(false)) {
                String[] pos = key.split(",");
                Block block = world.getBlockAt(Integer.parseInt(pos[0]), Integer.parseInt(pos[1]), Integer.parseInt(pos[2]));
                String title = signs.get(key+".title").toString();
                String url = signs.get(key+".url").toString();
                String filter = signs.get(key+".filter").toString();
                plugin.getMetricsClient().getMetric(title, url, filter, block);
            }

            ConfigurationSection images = config.getConfigurationSection("images");
            for(String key : images.getKeys(false)) {
                String panel = images.get(key+".panel").toString();
                Integer map = Integer.parseInt(images.get(key+".map").toString());
                if(plugin.hasMap(map)) {
                    String[] pos = key.split(",");
                    Location loc = new Location(world, Double.parseDouble(pos[0]), Double.parseDouble(pos[1]), Double.parseDouble(pos[2]));
                    plugin.getGrafanaClient().renderPngForPanel(loc, plugin.getMap(map), panel);
                }
            }

            try {
                Thread.sleep(interval, 0);
            } catch(InterruptedException e) {
                Bukkit.getLogger().log(Level.FINER, e.getLocalizedMessage());
            }
        }
    }
}