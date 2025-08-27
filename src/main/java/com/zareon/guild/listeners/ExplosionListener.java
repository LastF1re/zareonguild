package com.zareon.guild.listeners;

import com.zareon.guild.ZareonGuild;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExplosionListener implements Listener {
    private final ZareonGuild plugin;
    private final boolean protectGuildRegions;
    private final boolean allowTNTInOwnRegions;
    private final List<Material> protectedBlocks;
    private final int explosionProtectionRadius;

    public ExplosionListener(Z {
        this.plugin = pluginuildRegions = plugin.getConfig().getBoolean("regions.protect-from-explosions", true);
        this.allowTNTInOwnRegions = plugin.getConfig().getBoolean("tnt.allow-in-own-regions", false);
        this.explosionProtectionRadius = plugin.getConfig().getInt("regions.explosion-protection-radius", 10);
        this.protectedBlocks = loadProtectedBlocks();
    }

    private List<Material> loadProtectedBlocks() {
        List<Material> blocks = new ArrayList<>();
        List<String> blockNames = plugin.getConfig().getStringList("regions.protected-blocks");

        for (String blockName : blockNames) {
            try {
                Material material = Material.valueOf(blockName.toUpperCase());
                blocks.add(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Неизвестный материал в конфиге: " + blockName);
            }
        }

        // Добавляем базовые защищаемые блоки, если список пуст
        if (blocks.isEmpty()) {
            blocks.add(Material.CHEST);
            blocks.add(Material.TRAPPED_CHEST);
            blocks.add(Material.FURNACE);
            blocks.add(Material.BEACON);
            blocks.add(Material.ENCHANTING_TABLE);
            blocks.add(Material.ANVIL);
            blocks.add(Material.BREWING_STAND);
        }

        return blocks;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        if (!protectGuildRegions) return;

        Location explosionLocation = event.getEntity().getLocation();
        String regionOwner = plugin.getRegionManager().getRegionOwner(explosionLocation);

        if (regionOwner != null) {
            // Если взрыв происходит в регионе гильдии и TNT не разрешен в собственных регионах
            if (!allowTNTInOwnRegions) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!protectGuildRegions) return;

        Location explosionLocation = event.getLocation();
        String explosionRegionOwner = plugin.getRegionManager().getRegionOwner(explosionLocation);
        Iterator<Block> blockIterator = event.blockList().iterator();

        while (blockIterator.hasNext()) {
            Block block = blockIterator.next();
            String blockRegionOwner = plugin.getRegionManager().getRegionOwner(block.getLocation());

            if (blockRegionOwner != null) {
                // Если блок в регионе гильдии
                if (!allowTNTInOwnRegions) {
                    // Полная защита - убираем блок из списка разрушаемых
                    blockIterator.remove();
                } else {
                    // Частичная защита - защищаем только важные блоки
                    if (isProtectedBlock(block)) {
                        blockIterator.remove();
                    }
                }
            } else {
                // Блок вне регионов гильдий
                // Проверяем, не находится ли он слишком близко к региону
                if (isNearGuildRegion(block.getLocation())) {
                    if (isProtectedBlock(block)) {
                        blockIterator.remove();
                    }
                }
            }
        }

        // Уменьшаем силу взрыва в регионах гильдий
        if (explosionRegionOwner != null) {
            // Уменьшаем силу взрыва на 50%
            event.setYield(event.getYield() * 0.5f);
        }
    }

    private boolean isProtectedBlock(Block block) {
        return protectedBlocks.contains(block.getType());
    }

    private boolean isNearGuildRegion(Location location) {
        return !plugin.getRegionManager().getNearbyRegions(location, explosionProtectionRadius).isEmpty();
    }

    // Дополнительные методы для детального контроля взрывов
    public boolean canExplodeInRegion(Location location, String guildName) {
        String regionOwner = plugin.getRegionManager().getRegionOwner(location);

        if (regionOwner == null) return true;

        // Если это регион той же гильдии и разрешены взрывы в собственных регионах
        if (regionOwner.equals(guildName) && allowTNTInOwnRegions) {
            return true;
        }

        // Иначе взрывы запрещены
        return false;
    }

    public void addProtectedBlock(Material material) {
        if (!protectedBlocks.contains(material)) {
            protectedBlocks.add(material);
            updateConfig();
        }
    }

    public void removeProtectedBlock(Material material) {
        if (protectedBlocks.remove(material)) {
            updateConfig();
        }
    }

    private void updateConfig() {
        List<String> blockNames = new ArrayList<>();
        for (Material material : protectedBlocks) {
            blockNames.add(material.name());
        }
        plugin.getConfig().set("regions.protected-blocks", blockNames);
        plugin.saveConfig();
    }
}
