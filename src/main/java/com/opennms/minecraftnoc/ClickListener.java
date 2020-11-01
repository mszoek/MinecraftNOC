package com.opennms.minecraftnoc;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.logging.Level;

public class ClickListener implements Listener {
    private MinecraftNOC plugin;

    public ClickListener(MinecraftNOC main) {
        this.plugin = main;
    }

    public static boolean hasToolEquipped(Player p) {
        return (p.getInventory().getItemInMainHand().getType() == Material.STICK);
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEvent e) {
        if(e.getAction() == Action.LEFT_CLICK_AIR) {
            Player p = e.getPlayer();
            if(!hasToolEquipped(p)) {
                return;
            }

            plugin.clearCurrentBlock();
            plugin.clearCurrentEntity();
            p.sendMessage(ChatHelper.format("Cleared current item and block"));
        }
    }

    @EventHandler
    public void onEntityInteract(EntityDamageByEntityEvent e) {
        if(e.getEntityType() != EntityType.ITEM_FRAME) {
            return;
        }

        Player p = (Player)e.getDamager();
        if(!hasToolEquipped(p)) {
            return;
        }

        ItemFrame frame = (ItemFrame)e.getEntity();
        if(frame.getItem().getType() != Material.FILLED_MAP) {
            return;
        }

        plugin.setCurrentEntityTopLeft(frame);
        p.sendMessage(ChatHelper.format("Item top left position set!"));

        e.setCancelled(true);
    }

    @EventHandler
    public void onEntityRightClick(PlayerInteractEntityEvent e) {
        if(e.getRightClicked().getType() != EntityType.ITEM_FRAME) {
            return;
        }

        Player p = e.getPlayer();
        if(!hasToolEquipped(p)) {
            return;
        }

        ItemFrame frame = (ItemFrame)e.getRightClicked();
        if(frame.getItem().getType() != Material.FILLED_MAP) {
            return;
        }

        plugin.setCurrentEntityBotRight(frame);
        p.sendMessage(ChatHelper.format("Item bottom right position set!"));

        e.setCancelled(true);
    }

    @EventHandler
    public void onBlockClick(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBlock();
        if (b.getState() instanceof Sign) {
            if (hasToolEquipped(p)) {
                plugin.setCurrentBlock(b);
                p.sendMessage(ChatHelper.format("Current block set!"));
                e.setCancelled(true);
            } else {
                if(plugin.getCurrentBlock().equals(b)) {
                    plugin.clearCurrentBlock();
                }
                Location loc = b.getLocation();
                int X = loc.getBlockX();
                int Y = loc.getBlockY();
                int Z = loc.getBlockZ();
                String pos = X + "," + Y + "," + Z;

                String path = "signs." + pos;
                FileConfiguration config = plugin.getConfig();
                config.set(path, null);
                plugin.saveConfig();
            }
        }
    }
}
