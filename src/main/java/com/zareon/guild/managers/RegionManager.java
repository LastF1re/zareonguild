package com.zareon.guild.managers;

import com.zareon.guild.ZareonGuild;
import com.zareon.guild.models.Guild;
import com.zareon.guild.models.GuildRegion;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class RegionManager {
    private final ZareonGuild plugin;
    private Map<String, List<GuildRegion>> guildRegions;
    private final int maxRegionsPerGuild;
    private final int maxRegionRadius;
    private final int minDistanceBetweenRegions;

    // Типы регионов и их параметры
    private final Map<String, Integer> regionTypeRadiusLimits;
    private final Map<String, Integer> regionTypeHeightLimits;
    private final Map<String, Integer> regionTypeCosts;

    public RegionManager(ZareonGuild plugin) {
        this.plugin = plugin;
        this.guildRegions = new HashMap<>();
        this.maxRegionsPerGuild = plugin.getConfig().getInt("regions.max-per-guild", 5);
        this.maxRegionRadius = plugin.getConfig().getInt("regions.max-radius", 50);
        this.minDistanceBetweenRegions = plugin.getConfig().getInt("regions.min-distance", 100);

        // Инициализируем типы регионов
        this.regionTypeRadiusLimits = new HashMap<>();
        this.regionTypeHeightLimits = new HashMap<>();
        this.regionTypeCosts = new HashMap<>();
        initializeRegionTypes();
    }

    private void initializeRegionTypes() {
        // SMALL регион
        regionTypeRadiusLimits.put("SMALL", 25);
        regionTypeHeightLimits.put("SMALL", 50);
        regionTypeCosts.put("SMALL", 1000);

        // MEDIUM регион
        regionTypeRadiusLimits.put("MEDIUM", 40);
        regionTypeHeightLimits.put("MEDIUM", 80);
        regionTypeCosts.put("MEDIUM", 5000);

        // LARGE регион
        regionTypeRadiusLimits.put("LARGE", 60);
        regionTypeHeightLimits.put("LARGE", 120);
        regionTypeCosts.put("LARGE", 15000);
    }

    public boolean createRegion(String guildName, Location center, int radius, int height, String type) {
        Guild guild = plugin.getGuildManager().getGuild(guildName);
        if (guild == null) return false;

        // Проверяем лимит регионов
        List<GuildRegion> regions = getGuildRegions(guildName);
        if (regions.size() >= maxRegionsPerGuild) return false;

        // Проверяем корректность типа региона
        if (!isValidRegionType(type)) return false;

        // Проверяем размер региона для данного типа
        if (!isValidRegionSize(type, radius, height)) return false;

        // Проимальное расстояние до других регионов
        if (!isValidDistance(center, radius, guildName)) return false;

        // Проверяем, достаточно ли средств у гильдии
        if (conomyManager().hasEconomy()) {
            int cost = getRegionCost(type);
            double guildBalance = plugin.getEconomyManager().getGuildBalance(guildName);
            if (guildBalance < cost) return false;

            // Списываем средства
            plugin.getEconomyManager().setGuildBalance(guildName, guildBalance - cost);
        }

        GuildRegion region = new GuildRegion(center, radius, height, type);
        regions.add(region);
        guildRegions.put(guildName, regions);

        saveGuildRegions(guildName);
        return true;
    }

    public boolean deleteRegion(String guildName, UUID regionId) {
        List<GuildRegion> regions = getGuildRegions(guildName);
        GuildRegion regionToRemove = regions.stream()
                .filter(region -> region.getId().equals(regionId))
                .findFirst()
                .orElse(null);

        if (regionToRemove == null) return false;

        // Возвращаем часть средств при удалении региона (50%)
        if (plugin.getEconomyManager().hasEconomy()) {
            int refund = getRegionCost(regionToRemove.getType()) / 2;
            double currentBalance = plugin.getEconomyManager().getGuildBalance(guildName);
            plugin.getEconomyManager().setGuildBalance(guildName, currentBalance + refund);
        }

        regions.remove(regionToRemove);
        guildRegions.put(guildName, regions);
        saveGuildRegions(guildName);
        return true;
    }

    public List<GuildRegion> getGuildRegions(String guildName) {
        return guildRegions.getOrDefault(guildName, new ArrayList<>());
    }

    public GuildRegion getRegion(UUID regionId) {
        for (List<GuildRegion> regions : guildRegions.values()) {
            for (GuildRegion region : regions) {
                if (region.getId().equals(regionId)) {
                    return region;
                }
            }
        }
        return null;
    }

    public GuildRegion getRegionAt(Location location) {
        for (Map.Entry<String, List<GuildRegion>> entry : guildRegions.entrySet()) {
            for (GuildRegion region : entry.getValue()) {
                if (region.isInRegion(location)) {
                    return region;
                }
            }
        }
        return null;
    }

    public String getRegionOwner(Location location) {
        for (Map.Entry<String, List<GuildRegion>> entry : guildRegions.entrySet()) {
            for (GuildRegion region : entry.getValue()) {
                if (region.isInRegion(location)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    public boolean isInGuildRegion(Location location, String guildName) {
        List<GuildRegion> regions = getGuildRegions(guildName);
        return regions.stream().anyMatch(region -> region.isInRegion(location));
    }

    public boolean canBuildAt(Location location, String playerGuild) {
        String regionOwner = getRegionOwner(location);

        // Если региона нет, строить можно
        if (regionOwner == null) return true;

        // Если игрок в той же гильдии, что и владелец региона
        return regionOwner.equals(playerGuild);
    }

    private boolean isValidRegionType(String type) {
        return regionTypeRadiusLimits.containsKey(type);
    }

    private boolean isValidRegionSize(String type, int radius, int height) {
        Integer maxRadius = regionTypeRadiusLimits.get(type);
        Integer maxHeight = regionTypeHeightLimits.get(type);

        return maxRadius != null && maxHeight != null &&
                radius <= maxRadius && height <= maxHeight &&
                radius > 0 && height > 0;
    }

    private boolean isValidDistance(Location center, int radius, String excludeGuild) {
        for (Map.Entry<String, List<GuildRegion>> entry : guildRegions.entrySet()) {
            if (entry.getKey().equals(excludeGuild)) continue;

            for (GuildRegion region : entry.getValue()) {
                if (!region.getCenter().getWorld().equals(center.getWorld())) continue;

                double distance = region.getCenter().distance(center);
                double minRequired = radius + region.getRadius() + minDistanceBetweenRegions;

                if (distance < minRequired) {
                    return false;
                }
            }
        }
        return true;
    }

    public void deleteAllGuildRegions(String guildName) {
        guildRegions.remove(guildName);
        plugin.getConfig().set("guild-regions." + guildName, null);
        plugin.saveConfig();
    }

    public int getRegionCount(String guildName) {
        return getGuildRegions(guildName).size();
    }

    public boolean hasRegionLimit(String guildName) {
        return getRegionCount(guildName) >= maxRegionsPerGuild;
    }

    public List<GuildRegion> getRegionsInWorld(World world) {
        List<GuildRegion> worldRegions = new ArrayList<>();
        for (List<GuildRegion> regions : guildRegions.values()) {
            for (GuildRegion region : regions) {
                if (region.getCenter().getWorld().equals(world)) {
                    worldRegions.add(region);
                }
            }
        }
        return worldRegions;
    }

    public List<GuildRegion> getNearbyRegions(Location location, double distance) {
        List<GuildRegion> nearbyRegions = new ArrayList<>();
        for (List<GuildRegion> regions : guildRegions.values()) {
            for (GuildRegion region : regions) {
                if (region.getCenter().getWorld().equals(location.getWorld()) &&
                        region.getCenter().distance(location) <= distance) {
                    nearbyRegions.add(region);
                }
            }
        }
        return nearbyRegions;
    }

    public int getMaxRegionRadius() {
        return maxRegionRadius;
    }

    public int getMaxRegionsPerGuild() {
        return maxRegionsPerGuild;
    }

    public int getRegionCost(String type) {
        return regionTypeCosts.getOrDefault(type, 0);
    }

    public int getMaxRadiusForType(String type) {
        return regionTypeRadiusLimits.getOrDefault(type, 0);
    }

    public int getMaxHeightForType(String type) {
        return regionTypeHeightLimits.getOrDefault(type, 0);
    }

    public SetailableRegionTypes() {
        return regionTypeRadiusLimits.keySet();
    }

    public List<GuildRegion> getOverlappingRegions(Location center, int radius, String excludeGuild) {
        List<GuildRegion> overlappingfor (Map.Entry<String, List<GuildRegion>> entry : guildRegions.entrySet()) {
            if (entry.getKey().equals(excludeGuild)) continue;

            for (GuildRegion region : entry.getValue()) {
                if (!region.getCenter().getWorld().equals(center.getWorld())) continue;

                double distance = region.getCenter().distance(center);
                if (distance < radius + region.getRadius()) {
                    overlapping.add(region);
                }
            }
        }

        return overlapping;
    }

    public boolean canCreateRegionType(String guildName, String type) {
        // Проверяем экономические требования
        if (plugin.getEconomyManager().hasEconomy()) {
            double balance = plugin.getEconomyManager().getGuildBalance(guildName);
            int cost = getRegionCost(type);
            if (balance < cost) return false;
        }

        // Проверяем лимит регионов
        if (hasRegionLimit(guildName)) return false;

        return true;
    }

    public Map<String, Object> getRegionInfo(UUID regionId) {
        GuildRegion region = getRegion(regionId);
        if (region == null) return null;

        Map<String, Object> info = new HashMap<>();
        info.put("id", region.getId().toString());
        info.put("type", region.getType());
        info.put("radius", region.getRadius());
        info.put("height", region.getHeight());
        info.put("center", region.getCenter());
        info.put("created", new Date(region.getCreatedTime()));

        // Находим владельца
        for (Map.Entry<String, List<GuildRegion>> entry : guildRegions.entrySet()) {
            if (entry.getValue().contains(region)) {
                info.put("owner", entry.getKey());
                break;
            }
        }

        return info;
    }

    public List<GuildRegion> getRegionsByType(String guildName, String type) {
        return getGuildRegions(guildName).stream()
                .filter(region -> region.getType().equals(type))
                .collect(java.util.stream.Collectors.toList());
    }

    private void saveGuildRegions(String guildName) {
        List<GuildRegion> regions = getGuildRegions(guildName);
        List<Map<String, Object>> serializedRegions = new ArrayList<>();

        for (GuildRegion region : regions) {
            serializedRegions.add(region.serialize());
        }

        plugin.getConfig().set("guild-regions." + guildName, serializedRegions);
        plugin.saveConfig();
    }

    public void loadGuildRegions() {
        ConfigurationSection regionsSection = plugin.getConfig().getConfigurationSection("guild-regions");
        if (regionsSection == null) return;

        for (String guildName : regionsSection.getKeys(false)) {
            List<GuildRegion> regions = new ArrayList<>();
            List<Map<?, ?>> serializedRegions = regionsSection.getMapList(guildName);

            for (Map<?, ?> serializedRegion : serializedRegions) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> regionMap = (Map<String, Object>) serializedRegion;
                    GuildRegion region = new GuildRegion(regionMap);
                    regions.add(region);
                } catch (Exception e) {
                    plugin.getLogger().warning("Не удалось загрузить регион для гильдии " + guildName + ": " + e.getMessage());
                }
            }

            guildRegions.put(guildName, regions);
        }

        plugin.getLogger().info("Загружено регионов: " + guildRegions.values().stream().mapToInt(List::size).sum());
    }

    // Методы для администрирования
    public void setRegionTypeLimits(String type, int maxRadius, int maxHeight, int cost) {
        regionTypeRadiusLimits.put(type, maxRadius);
        regionTypeHeightLimits.put(type, maxHeight);
        regionTypeCosts.put(type, cost);

        // Сохраняем в конфиг
        plugin.getConfig().set("region-types." + type + ".max-radius", maxRadius);
        plugin.getConfig().set("region-types." + type + ".max-height", maxHeight);
        plugin.getConfig().set("region-types." + type + ".cost", cost);
        plugin.saveConfig();
    }

    public void loadRegionTypes() {
        ConfigurationSection typesSection = plugin.getConfig().getConfigurationSection("region-types");
        if (typesSection != null) {
            for (String type : typesSection.getKeys(false)) {
                int maxRadius = typesSection.getInt(type + ".max-radius", 25);
                int maxHeight = typesSection.getInt(type + ".max-height", 50);
                int cost = typesSection.getInt(type + ".cost", 1000);

                regionTypeRadiusLimits.put(type, maxRadius);
                regionTypeHeightLimits.put(type, maxHeight);
                regionTypeCosts.put(type, cost);
            }
        }
    }
}
