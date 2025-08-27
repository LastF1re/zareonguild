package com.zareon.guild.models;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.kyori.adventure.text.minimessage.MiniMessage;

public class CustomTNT {
    private String id;
    private String displayName;
    private List<String> description;
    private float power;
    private boolean breaksObsidian;
    private boolean breaksWater;
    private ShapedRecipe recipe;
    private Map<Character, Material> ingredients;
    private ItemStack itemStack;

    public CustomTNT(String id, ConfigurationSection config) {
        this.id = id;

        MiniMessage miniMessage = MiniMessage.miniMessage();

        this.displayName = config.getString("display", id);

        List<String> loreList = new ArrayList<>();
        loreList.add(config.getString("description", "A custom TNT"));
        if (config.getBoolean("breaks-obsidian", false)) {
            loreList.add("§7Can break obsidian");
        }
        if (config.getBoolean("breaks-water", false)) {
            loreList.add("§7Can break water");
        }
        this.description = loreList;

        this.power = (float) config.getDouble("power", 4.0);
        this.breaksObsidian = config.getBoolean("breaks-obsidian", false);
        this.breaksWater = config.getBoolean("breaks-water", false);

        // Create the custom TNT item
        this.itemStack = new ItemStack(Material.TNT);
        ItemMeta meta = itemStack.getItemMeta();
        meta.displayName(miniMessage.deserialize(displayName));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String line : description) {
            lore.add(miniMessage.deserialize(line));
        }
        meta.lore(lore);

        itemStack.setItemMeta(meta);

        // Parse recipe
        ConfigurationSection recipeSection = config.getConfigurationSection("craft");
        if (recipeSection != null) {
            String[] shape = recipeSection.getStringList("shape").toArray(new String[0]);

            this.ingredients = new HashMap<>();
            ConfigurationSection ingredientsSection = recipeSection.getConfigurationSection("ingredients");
            for (String key : ingredientsSection.getKeys(false)) {
                char character = key.charAt(0);
                String materialName = ingredientsSection.getString(key);
                Material material = Material.valueOf(materialName);
                ingredients.put(character, material);
            }
        }
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getDescription() {
        return description;
    }

    public float getPower() {
        return power;
    }

    public boolean breaksObsidian() {
        return breaksObsidian;
    }

    public boolean breaksWater() {
        return breaksWater;
    }

    public ShapedRecipe getRecipe() {
        return recipe;
    }

    public Map<Character, Material> getIngredients() {
        return ingredients;
    }

    public ItemStack getItemStack() {
        return itemStack.clone();
    }
}
