package com.opennms.minecraftnoc;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MinecraftFont;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class NOCMapRenderer extends MapRenderer {
    private Map<Integer, BufferedImage> mapImages;

    public NOCMapRenderer() {
        mapImages = new HashMap<>();
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if(!mapImages.containsKey(map.getId()))
            return;

        canvas.drawImage(0, 0, mapImages.get(map.getId()));
//        canvas.drawText(10, 10, MinecraftFont.Font, "§0;Map #"+map.getId()+" rendered!");
    }

    public void applyToMap(MapView map) {
        for (MapRenderer renderer : map.getRenderers()) {
            map.removeRenderer(renderer);
        }
        map.addRenderer(this);
    }

    public void setMapImage(int mapId, BufferedImage image) {
        mapImages.put(mapId, image);
    }
}
