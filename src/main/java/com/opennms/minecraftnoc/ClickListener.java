package com.opennms.minecraftnoc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.logging.Level;

public class ClickListener implements Listener {
    private MinecraftNOC plugin;

    public ClickListener(MinecraftNOC main) {
        this.plugin = main;
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEvent e) {
        if(e.getAction() == Action.LEFT_CLICK_AIR) {
            plugin.clearCurrentBlock();
            plugin.clearCurrentEntity();
            Player p = e.getPlayer();
            p.sendMessage(ChatHelper.format("Cleared current item and block"));
        }
    }

    @EventHandler
    public void onEntityInteract(EntityDamageByEntityEvent e) {
        if(e.getEntityType() != EntityType.ITEM_FRAME) {
            return;
        }

        Player p = (Player)e.getDamager();
        if(p.getInventory().getItemInMainHand().getType() != Material.STICK) {
            return;
        }

        plugin.setCurrentEntity(e.getEntity());
        p.sendMessage(ChatHelper.format("Current item set!"));

        e.setCancelled(true);
    }

    @EventHandler
    public void onBlockClick(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if(p.getInventory().getItemInMainHand().getType() != Material.STICK) {
            return;
        }

       Block b = e.getBlock();
        if (b.getState() instanceof Sign) {
            Location loc = b.getLocation();
            int X = loc.getBlockX();
            int Y = loc.getBlockY();
            int Z = loc.getBlockZ();
            Bukkit.getLogger().log(Level.WARNING, "Clicked block at " + X + "," + Y + "," + Z);

            plugin.setCurrentBlock(b);
            p.sendMessage(ChatHelper.format("Current block set!"));

            e.setCancelled(true);
        }
    }
}