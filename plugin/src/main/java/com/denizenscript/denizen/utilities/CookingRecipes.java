package com.denizenscript.denizen.utilities;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 熔炼类配方（furnace / blasting / smoking / campfire）的“按输入查找”索引。
 * <p>
 * Bukkit 只提供了按产物查找配方的 {@link Bukkit#getRecipesFor}，想知道“某个物品烧出来是什么”只能遍历整张配方表。
 * 而配方输入是 {@link RecipeChoice}，一个输入项可能涵盖多个材料（例如玻璃的输入同时包含沙子与红沙），
 * 脚本侧拿不到完整信息，只能猜。这里在首次查询时构建一份 材料 -&gt; 配方 的索引，之后的查询都是常数级的，
 * 且匹配环节直接交还给原版的 {@link RecipeChoice#test}，多材料输入与精确匹配输入都能正确处理。
 */
public class CookingRecipes {

    /** 允许作为类型参数的取值，其中 cooking 表示不限定具体的炉子类型。 */
    public static final Set<String> VALID_TYPES = Set.of("furnace", "blasting", "smoking", "campfire", "cooking");

    public static final Map<String, Map<Material, List<CookingRecipe<?>>>> indexCache = new HashMap<>();

    /** 服务器配方表发生变化时必须调用，否则索引会一直停留在旧数据上。 */
    public static void clearCache() {
        indexCache.clear();
    }

    public static Map<Material, List<CookingRecipe<?>>> getIndex(String type) {
        return indexCache.computeIfAbsent(type, CookingRecipes::buildIndex);
    }

    public static Map<Material, List<CookingRecipe<?>>> buildIndex(String type) {
        Map<Material, List<CookingRecipe<?>>> index = new HashMap<>();
        Iterator<Recipe> recipeIterator = Bukkit.recipeIterator();
        while (true) {
            Recipe recipe;
            try {
                if (!recipeIterator.hasNext()) {
                    break;
                }
                recipe = recipeIterator.next();
            }
            catch (Throwable ex) {
                // Spigot 的配方迭代器在某一类配方被全部移除后会抛错，这里提前收尾，至少保住已经扫到的部分。
                break;
            }
            if (!(recipe instanceof CookingRecipe<?> cookingRecipe) || !Utilities.isRecipeOfType(recipe, type)) {
                continue;
            }
            for (Material material : getChoiceMaterials(cookingRecipe.getInputChoice())) {
                index.computeIfAbsent(material, (k) -> new ArrayList<>(2)).add(cookingRecipe);
            }
        }
        return index;
    }

    /** 取出一个配方输入项所涵盖的全部材料，仅用作索引的键，最终是否匹配仍由 {@link RecipeChoice#test} 决定。 */
    public static List<Material> getChoiceMaterials(RecipeChoice choice) {
        if (choice == null) {
            return Collections.emptyList();
        }
        if (choice instanceof RecipeChoice.MaterialChoice materialChoice) {
            return materialChoice.getChoices();
        }
        if (choice instanceof RecipeChoice.ExactChoice exactChoice) {
            List<Material> materials = new ArrayList<>(exactChoice.getChoices().size());
            for (ItemStack item : exactChoice.getChoices()) {
                materials.add(item.getType());
            }
            return materials;
        }
        return Collections.singletonList(choice.getItemStack().getType());
    }

    /** 查找以该物品为输入的熔炼配方，找不到时返回 null。 */
    public static CookingRecipe<?> getRecipe(ItemStack input, String type) {
        if (input == null || input.getType() == Material.AIR) {
            return null;
        }
        List<CookingRecipe<?>> candidates = getIndex(type).get(input.getType());
        if (candidates == null) {
            return null;
        }
        for (CookingRecipe<?> recipe : candidates) {
            if (recipe.getInputChoice().test(input)) {
                return recipe;
            }
        }
        return null;
    }
}
