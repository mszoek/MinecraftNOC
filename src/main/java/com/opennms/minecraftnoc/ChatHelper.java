package com.opennms.minecraftnoc;
import org.bukkit.ChatColor;

public class ChatHelper {
    static final String prefix = "&e&lMinecraftNOC &7➤ ";

    public static String format(String input) {
        return ChatColor.translateAlternateColorCodes('&', prefix+input);
    }
}
