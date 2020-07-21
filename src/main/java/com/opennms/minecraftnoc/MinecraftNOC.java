package com.opennms.minecraftnoc;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class MinecraftNOC extends JavaPlugin {

    private GrafanaClientImpl grafanaClient;

    @Override
    public void onEnable() {
        // Plugin startup logic

        getConfig().options().copyDefaults();
        saveDefaultConfig();

        FileConfiguration config = getConfig();
        grafanaClient = new GrafanaClientImpl(config.get("grafana.baseurl").toString(),
                config.get("grafana.apikey").toString());

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new ClickListener(this), this);
        getCommand("getpng").setExecutor(new CommandGetPNG(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

}
