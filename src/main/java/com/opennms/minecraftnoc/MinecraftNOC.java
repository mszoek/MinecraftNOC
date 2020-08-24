package com.opennms.minecraftnoc;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.map.MapView;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public final class MinecraftNOC extends JavaPlugin {
    private Block currentBlock;
    private Entity currentEntity;
    private MetricsClientImpl metricsClient;
    private GrafanaClientImpl grafanaClient;
    private NOCMapRenderer mapRenderer;
    private Updater updater;
    private Map<Integer, MapView> initializedMaps;

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults();
        saveDefaultConfig();

        currentBlock = null;
        currentEntity = null;
        initializedMaps = new HashMap<>();

        metricsClient = new MetricsClientImpl(this);
        grafanaClient = new GrafanaClientImpl(this);

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new ClickListener(this), this);
        pm.registerEvents(new MapListener(this), this);
        getCommand("getpng").setExecutor(new CommandGetPNG(this));
        getCommand("getmetric").setExecutor(new CommandGetMetric(this));
        getCommand("dashboard").setExecutor(new CommandDashboard(this));

        mapRenderer = new NOCMapRenderer();
        updater = new Updater(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public Block getCurrentBlock() { return currentBlock; }
    public void setCurrentBlock(Block block) { currentBlock = block; }
    public void clearCurrentBlock() { currentBlock = null; }

    public Entity getCurrentEntity() { return currentEntity; }
    public void setCurrentEntity(Entity ent) { currentEntity = ent; }
    public void clearCurrentEntity() { currentEntity = null; }

    public MetricsClientImpl getMetricsClient() { return metricsClient; }
    public GrafanaClientImpl getGrafanaClient() { return grafanaClient; }
    public NOCMapRenderer getMapRenderer() { return mapRenderer; }

    public boolean hasMap(int mapId) { return initializedMaps.containsKey(mapId); }
    public void addMap(int mapId, MapView entity) { initializedMaps.put(mapId, entity); }
    public void removeMap(int mapId) { initializedMaps.remove(mapId); }
    public MapView getMap(int mapId) { return initializedMaps.get(mapId); }
}
