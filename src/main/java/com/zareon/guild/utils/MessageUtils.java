package com.zareon.guild.utils;

import com.zareon.guild.ZareonGuild;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

public class MessageUtils {
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static ZareonGuild plugin;

    public static void init(ZareonGuild instance) {
        plugin = instance;
    }

    public static void sendMessage(Player player, String key) {
        String message = plugin.getConfigManager().getMessage(key);
        player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getPrefix() + message));
    }

    public static void sendMessage(Player player, String key, String... replacements) {
        String message = plugin.getConfigManager().getMessage(key);

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                String placeholder = replacements[i];
                String value = replacements[i + 1];
                message = message.replace("<" + placeholder + ">", value);
            }
        }

        player.sendMessage(miniMessage.deserialize(plugin.getConfigManager().getPrefix() + message));
    }

    public static Component parse(String text) {
        return miniMessage.deserialize(text);
    }
}
