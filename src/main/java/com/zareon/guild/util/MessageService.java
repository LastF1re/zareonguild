package com.zareon.guild.util;

import com.zareon.guild.ZareonGuildPlugin;
import com.zareon.guild.data.Guild;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MessageService {

    private final ZareonGuildPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private FileConfiguration cfg;

    public MessageService(ZareonGuildPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        cfg = YamlConfiguration.loadConfiguration(file);
    }

    public Component prefix() {
        return mm.deserialize(cfg.getString("prefix", "<gray>[Guild]</gray> "));
    }

    public void send(CommandSender sender, String path, Map<String, String> vars) {
        String base = cfg.getString(path, "<red>Missing message: " + path + "</red>");
        if (base == null) base = path;
        String with = apply(base, vars);
        Component comp = Component.empty().append(prefix()).append(mm.deserialize(with));
        sender.sendMessage(comp);
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, new HashMap<>());
    }

    public Component format(String path, Map<String, String> vars) {
        String base = cfg.getString(path, path);
        return mm.deserialize(apply(base, vars));
    }

    public String apply(String text, Map<String, String> vars) {
        String out = text;
        if (vars != null) {
            for (Map.Entry<String, String> e : vars.entrySet()) {
                out = out.replace("<" + e.getKey() + ">", e.getValue());
            }
        }
        return out;
    }

    public Component inline(String raw, Map<String, String> vars) {
        return mm.deserialize(apply(raw, vars));
    }

    public Component guildTagOrDash(Guild guild) {
        String tag = guild != null ? guild.getTag() : null;
        if (tag == null || tag.isBlank()) {
            return mm.deserialize(cfg.getString("placeholders.no_guild_tag", "<gray>-</gray>"));
        }
        return mm.deserialize(tag);
    }

    public void actionOpen(Player player, Component component) {
        // Paper: directly send adventure component
        player.sendMessage(component);
    }
}
