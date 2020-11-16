package de.kaonashi.minecraft.multichestsearch;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;

import de.kaonashi.minecraft.commons.advancements.AdvancementAPI;
import de.kaonashi.minecraft.commons.advancements.Condition;
import de.kaonashi.minecraft.commons.advancements.Requirements;
import de.kaonashi.minecraft.commons.advancements.Rewards;
import de.kaonashi.minecraft.commons.advancements.Trigger;
import de.kaonashi.minecraft.commons.advancements.TriggerType;
import de.kaonashi.minecraft.commons.advancements.conditions.primitive.ItemList;
import de.kaonashi.minecraft.commons.item.CustomItemStack;
import de.kaonashi.minecraft.commons.plugin.KaonashiPlugin;
import de.kaonashi.minecraft.commons.plugin.StaticPluginAccess;
import de.kaonashi.minecraft.commons.registry.AdvancementGetter;
import de.kaonashi.minecraft.commons.registry.ItemGetter;
import de.kaonashi.minecraft.commons.registry.RecipeGetter;

public class MultiChestSearchItem extends CustomItemStack {

    /**
     * Reference ItemStack.
     */
    @ItemGetter("Recipes.MultiChestSearcher")
    public static final CustomItemStack ITEM_STACK = new MultiChestSearchItem();

    private static final String KEY = "multi_chest_searcher";

    @RecipeGetter("Recipes.MultiChestSearcher")
    public static Recipe MultiChestSearchRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(StaticPluginAccess.get(), KEY), ITEM_STACK);
        // @formatter:off
        recipe.shape("AAA", "AAA", " B ")
                .setIngredient('A', Material.GLASS)
                .setIngredient('B', Material.STICK);
        // @formatter:on
        return recipe;
    }

    @AdvancementGetter("Recipes.MultiChestSearcher")
    public static AdvancementAPI.AdvancementAPIBuilder MultiChestSearchRecipeAdvancement() {
        final KaonashiPlugin plugin = StaticPluginAccess.get();
        // @formatter:off
        return AdvancementAPI.builder(new NamespacedKey(plugin, "recipes/" + KEY))
                .trigger(Trigger.builder(TriggerType.INVENTORY_CHANGED, "has_glass")
                        .condition(Condition.builder("items", ItemList.builder(Material.GLASS)))
                ).trigger(Trigger.builder(TriggerType.INVENTORY_CHANGED, "has_stick")
                        .condition(Condition.builder("items", ItemList.builder(Material.STICK)))
                ).trigger(Trigger.builder(TriggerType.INVENTORY_CHANGED, "has_string")
                        .condition(Condition.builder("items", ItemList.builder(Material.STRING)))
                ).trigger(Trigger.builder(TriggerType.RECIPE_UNLOCKED, "has_the_recipe")
                        .condition(Condition.builder("recipe", plugin.getNamespace() + KEY))
                )
                .requirements(Requirements.builder("has_string", "has_stick", "has_the_recipe", "has_glass"))
                .rewards(Rewards.builder().recipe(plugin.getNamespace() + KEY));
        // @formatter:on
    }

    public MultiChestSearchItem() {
        super(new NamespacedKey(StaticPluginAccess.get(), KEY), Material.STICK);
        setDisplayName("Chest Marker");
        setLore("§8Marks a Chest for searching.");
    }
}
