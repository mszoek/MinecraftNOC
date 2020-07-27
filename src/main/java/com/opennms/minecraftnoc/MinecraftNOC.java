package com.opennms.minecraftnoc;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class MinecraftNOC extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic

        getConfig().options().copyDefaults();
        saveDefaultConfig();

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new ClickListener(this), this);
        getCommand("getpng").setExecutor(new CommandGetPNG(this));
        getCommand("getmetric").setExecutor(new CommandGetMetric(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

}
