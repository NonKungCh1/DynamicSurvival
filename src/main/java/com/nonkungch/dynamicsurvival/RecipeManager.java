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
    private final PouchManager pouchManager;

    public RecipeManager(DynamicSurvival plugin, PouchManager pouchManager) {
        this.plugin = plugin;
        this.pouchManager = pouchManager;
    }

    public void registerRecipes() {
        registerLeafArmorFromLeavesRecipes();
        registerPouchRecipes();
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

        plugin.getLogger().info("Successfully registered recipes for Leaf Armor!");
    }

    private void registerPouchRecipes() {
        // --- Tier 1 Water Pouch ---
        ItemStack pouchTier1 = pouchManager.createPouch(1);
        ShapedRecipe pouch1Recipe = new ShapedRecipe(new NamespacedKey(plugin, "water_pouch_t1"), pouchTier1);
        pouch1Recipe.shape("RSR", "RLR", "RRR");
        pouch1Recipe.setIngredient('R', Material.RABBIT_HIDE);
        pouch1Recipe.setIngredient('L', Material.LEATHER);
        pouch1Recipe.setIngredient('S', Material.STRING);
        Bukkit.addRecipe(pouch1Recipe);

        // --- Upgrade to Tier 2 ---
        ItemStack pouchTier2 = pouchManager.createPouch(2);
        ShapedRecipe pouch2Recipe = new ShapedRecipe(new NamespacedKey(plugin, "water_pouch_t2"), pouchTier2);
        pouch2Recipe.shape("LLL", "LPL", "LLL");
        pouch2Recipe.setIngredient('L', Material.LEATHER);
        pouch2Recipe.setIngredient('P', new RecipeChoice.ExactChoice(pouchManager.createPouch(1)));
        Bukkit.addRecipe(pouch2Recipe);

        // --- Upgrade to Tier 3 ---
        ItemStack pouchTier3 = pouchManager.createPouch(3);
        ShapedRecipe pouch3Recipe = new ShapedRecipe(new NamespacedKey(plugin, "water_pouch_t3"), pouchTier3);
        pouch3Recipe.shape("MMM", "MPM", "MMM");
        pouch3Recipe.setIngredient('M', Material.PHANTOM_MEMBRANE);
        pouch3Recipe.setIngredient('P', new RecipeChoice.ExactChoice(pouchManager.createPouch(2)));
        Bukkit.addRecipe(pouch3Recipe);

        plugin.getLogger().info("Successfully registered Water Pouch recipes!");
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
