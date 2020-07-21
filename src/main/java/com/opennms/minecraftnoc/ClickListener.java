package com.opennms.minecraftnoc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.logging.Level;

public class ClickListener implements Listener {
    private MinecraftNOC plugin;

    public ClickListener(MinecraftNOC main) {
        this.plugin = main;
    }

    @EventHandler
    public void onBlockClick(PlayerInteractEvent e) {
        if(e.getHand() != EquipmentSlot.HAND || e.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        if(e.getPlayer().getInventory().getItemInMainHand().getType() != Material.STICK) {
            return;
        }

        Block b = e.getClickedBlock();
        if(!(b.getState() instanceof Sign)) {
            return;
        }

        Location loc = b.getLocation();
        int X = loc.getBlockX();
        int Y = loc.getBlockY();
        int Z = loc.getBlockZ();
        Bukkit.getLogger().log(Level.WARNING, "Clicked block at "+X+","+Y+","+Z);
        e.setCancelled(true);
    }
}