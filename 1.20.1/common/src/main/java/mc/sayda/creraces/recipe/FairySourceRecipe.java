package mc.sayda.creraces.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.registry.ModRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

/**
 * Defines a conversion that occurs when an item sits in fairy source fluid.
 * No time or experience; one item entity matching the ingredient is
 * immediately replaced by the result stack (scaled by input count).
 *
 * JSON format:
 * {
 *   "type": "creraces:fairy_source",
 *   "ingredient": { "item": "minecraft:sugar" },   // supports tags too
 *   "result": { "item": "creraces:fairy_dust", "count": 1 }
 * }
 */
@SuppressWarnings("null")
public class FairySourceRecipe implements Recipe<Container> {

    public static final ResourceLocation TYPE_ID = new ResourceLocation(CreRaces.MODID, "fairy_source");

    private final ResourceLocation id;
    private final Ingredient ingredient;
    private final ItemStack result;

    public FairySourceRecipe(ResourceLocation id, Ingredient ingredient, ItemStack result) {
        this.id         = id;
        this.ingredient = ingredient;
        this.result     = result;
    }

    public Ingredient getIngredient() { return ingredient; }

    /**
     * Produces the output stack for the given input item entity.
     * Multiplies result count by the input count so the whole pile converts at once.
     */
    public ItemStack craft(ItemStack input) {
        ItemStack out = result.copy();
        out.setCount(result.getCount() * input.getCount());
        return out;
    }

    // ── Recipe<Container> contract (not used; no inventory, entity-driven) ──

    @Override public boolean matches(@Nonnull Container c, @Nonnull Level l) { return false; }

    @Override
    @Nonnull
    public ItemStack assemble(@Nonnull Container c, @Nonnull RegistryAccess r) { return result.copy(); }

    @Override public boolean canCraftInDimensions(int w, int h) { return true; }

    /** Resolved by direct fluid-interaction code, not a crafting grid; keep it out of the recipe book. */
    @Override public boolean isSpecial() { return true; }

    @Override
    @Nonnull
    public ItemStack getResultItem(@Nonnull RegistryAccess r) { return result.copy(); }

    @Override
    @Nonnull
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, ingredient);
    }

    @Override @Nonnull public ResourceLocation getId()            { return id; }
    @Override @Nonnull public RecipeSerializer<?> getSerializer() { return ModRecipes.FAIRY_SOURCE_SERIALIZER.get(); }
    @Override @Nonnull public RecipeType<?> getType()             { return ModRecipes.FAIRY_SOURCE_TYPE.get(); }

    // ── Serializer ────────────────────────────────────────────────────────────

    public static class Serializer implements RecipeSerializer<FairySourceRecipe> {

        @Override
        @Nonnull
        public FairySourceRecipe fromJson(@Nonnull ResourceLocation id, @Nonnull JsonObject json) {
            JsonElement ingredientEl = GsonHelper.getNonNull(json, "ingredient");
            Ingredient ingredient = Ingredient.fromJson(ingredientEl);
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            return new FairySourceRecipe(id, ingredient, result);
        }

        @Override
        @Nonnull
        public FairySourceRecipe fromNetwork(@Nonnull ResourceLocation id, @Nonnull FriendlyByteBuf buf) {
            Ingredient ingredient = Ingredient.fromNetwork(buf);
            ItemStack result = buf.readItem();
            return new FairySourceRecipe(id, ingredient, result);
        }

        @Override
        public void toNetwork(@Nonnull FriendlyByteBuf buf, @Nonnull FairySourceRecipe recipe) {
            recipe.ingredient.toNetwork(buf);
            buf.writeItem(recipe.result);
        }
    }
}
