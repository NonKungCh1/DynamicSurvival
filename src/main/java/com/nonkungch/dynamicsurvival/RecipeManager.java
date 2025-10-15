package com.nonkungch.dynamicsurvival;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class RecipeManager {

    private final DynamicSurvival plugin;

    public RecipeManager(DynamicSurvival plugin) {
        this.plugin = plugin;
    }

    public void registerRecipes() {
        registerLeafArmorFromLeavesRecipes();
    }

    private void registerLeafArmorFromLeavesRecipes() {
        ItemStack leafHelmet = createDyedArmor(Material.LEATHER_HELMET, "§aหมวกใบไม้");
        ShapedRecipe helmetRecipe = new ShapedRecipe(new NamespacedKey(plugin, "leaf_helmet_from_leaves"), leafHelmet);
        helmetRecipe.shape("LLL", "L L");
        helmetRecipe.setIngredient('L', new RecipeChoice.MaterialChoice(Tag.LEAVES));
        Bukkit.addRecipe(helmetRecipe);

        ItemStack leafChestplate = createDyedArmor(Material.LEATHER_CHESTPLATE, "§aเสื้อใบไม้");
        ShapedRecipe chestplateRecipe = new ShapedRecipe(new NamespacedKey(plugin, "leaf_chestplate_from_leaves"), leafChestplate);
        chestplateRecipe.shape("L L", "LLL", "LLL");
        chestplateRecipe.setIngredient('L', new RecipeChoice.MaterialChoice(Tag.LEAVES));
        Bukkit.addRecipe(chestplateRecipe);
        
        ItemStack leafLeggings = createDyedArmor(Material.LEATHER_LEGGINGS, "§aกางเกงใบไม้");
        ShapedRecipe leggingsRecipe = new ShapedRecipe(new NamespacedKey(plugin, "leaf_leggings_from_leaves"), leafLeggings);
        leggingsRecipe.shape("LLL", "L L", "L L");
        leggingsRecipe.setIngredient('L', new RecipeChoice.MaterialChoice(Tag.LEAVES));
        Bukkit.addRecipe(leggingsRecipe);

        ItemStack leafBoots = createDyedArmor(Material.LEATHER_BOOTS, "§aรองเท้าใบไม้");
        ShapedRecipe bootsRecipe = new ShapedRecipe(new NamespacedKey(plugin, "leaf_boots_from_leaves"), leafBoots);
        bootsRecipe.shape("L L", "L L");
        bootsRecipe.setIngredient('L', new RecipeChoice.MaterialChoice(Tag.LEAVES));
        Bukkit.addRecipe(bootsRecipe);
        
        plugin.getLogger().info("Successfully registered recipes for crafting leaf armor from leaves!");
    }

    private ItemStack createDyedArmor(Material leatherArmorPiece, String name) {
        ItemStack item = new ItemStack(leatherArmorPiece);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof LeatherArmorMeta) {
            LeatherArmorMeta leatherMeta = (LeatherArmorMeta) meta;
            leatherMeta.setDisplayName(name);
            leatherMeta.setColor(Color.fromRGB(80, 150, 45));
            PersistentDataContainer data = leatherMeta.getPersistentDataContainer();
            data.set(new NamespacedKey(plugin, "is_leaf_armor"), PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(leatherMeta);
        }
        return item;
    }
}
