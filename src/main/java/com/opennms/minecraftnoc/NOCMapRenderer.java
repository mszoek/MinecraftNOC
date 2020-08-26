package com.opennms.minecraftnoc;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class NOCMapRenderer extends MapRenderer {
    private final Map<Integer, BufferedImage> mapImages;

    public NOCMapRenderer() { mapImages = new HashMap<>(); }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if(!mapImages.containsKey(map.getId())) {
            return;
        }

        canvas.drawImage(0, 0, mapImages.get(map.getId()));
    }

    public void applyToMap(MapView map) {
        for (MapRenderer renderer : map.getRenderers()) {
            map.removeRenderer(renderer);
        }
        map.addRenderer(this);
    }

    public void setMapImage(int mapId, BufferedImage image) { mapImages.put(mapId, image); }
}
