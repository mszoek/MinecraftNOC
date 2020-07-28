package com.opennms.minecraftnoc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.entity.Player;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class CommandGetPNG implements CommandExecutor {
    private final GrafanaClientImpl grafanaClient;

    public CommandGetPNG(MinecraftNOC main) {
        grafanaClient = new GrafanaClientImpl(main);
    }

    public boolean onCommand(CommandSender cs, Command c, String label, String[] args) {
        if (cs instanceof Player) {
            Player p = (Player) cs;

            p.sendMessage(ChatHelper.format("Fetching PNG"));
            GregorianCalendar gcal = (GregorianCalendar) GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
            long now = gcal.getTimeInMillis() / 1000;
            gcal.add(Calendar.MINUTE, -60);
            long then = gcal.getTimeInMillis() / 1000;
            p.sendMessage(ChatHelper.format("then = "+then+" now = "+now));
            grafanaClient.renderPngForPanel(args[0],"8",512,256,then,now,null);
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
