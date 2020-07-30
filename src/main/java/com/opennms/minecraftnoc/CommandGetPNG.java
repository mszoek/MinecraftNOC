package com.opennms.minecraftnoc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Objects;
import java.util.TimeZone;

public class CommandGetPNG implements CommandExecutor {
    private final GrafanaClientImpl grafanaClient;
    private final String baseDashboard;
    private final MinecraftNOC plugin;

    public CommandGetPNG(MinecraftNOC main) {
        plugin = main;
        grafanaClient = new GrafanaClientImpl(main);
        baseDashboard = Objects.requireNonNull(main.getConfig().get("grafana.dashboard")).toString();
    }

    public boolean onCommand(CommandSender cs, Command c, String label, String[] args) {
        if (cs instanceof Player) {
            Player p = (Player) cs;

            p.sendMessage(ChatHelper.format("Fetching PNG"));
            GregorianCalendar gcal = (GregorianCalendar) GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
            long now = gcal.getTimeInMillis() / 1000;
            gcal.add(Calendar.MINUTE, -60);
            long then = gcal.getTimeInMillis() / 1000;

            // get the ID of this frame's map
            ItemFrame frame = (ItemFrame)plugin.getCurrentEntity();
            MapMeta mm = (MapMeta)frame.getItem().getItemMeta();

            grafanaClient.renderPngForPanel(baseDashboard, args[0], mm.getMapView().getId(), then, now,null);
            return true;
        } else {
            // Sender is console
            if (cs instanceof ConsoleCommandSender) {
                cs.sendMessage("This command can't be run from the console yet!");
            } else if (cs instanceof BlockCommandSender) {
                cs.sendMessage("This command can't be run from command blocks.");
            }
        }

        return false;
    }
}
