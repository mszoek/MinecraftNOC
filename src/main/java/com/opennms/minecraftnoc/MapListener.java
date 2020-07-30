package com.opennms.minecraftnoc;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.map.MapView;

public class MapListener implements Listener {
    private MinecraftNOC plugin;

    public MapListener(MinecraftNOC main) {
        this.plugin = main;
    }

    @EventHandler
    public void onMapInitialize(MapInitializeEvent event) {
        MapView map = event.getMap();
        plugin.getServer().broadcastMessage("[MapListener] Map " + map.getId() + " initialized.");
//      plugin.getMapRenderer().applyToMap(map);
    }
}
