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

    /*
     * FIXME: find a better way to identify NOC maps
     * This is really really suboptimal. We add every map initialized
     * in the world to the java map of spigot maps. Lots of memory can
     * be used even though many of the maps will not be used for the
     * NOC.
     */
    @EventHandler
    public void onMapInitialize(MapInitializeEvent event) {
        MapView map = event.getMap();
        plugin.addMap(map.getId(), map);
    }
}
