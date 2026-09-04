package mc.sayda.creraces.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
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
public class FairySourceRecipe implements Recipe<SingleRecipeInput> {

    public static final ResourceLocation TYPE_ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "fairy_source");

    public static final MapCodec<FairySourceRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(r -> r.ingredient),
            ItemStack.CODEC.fieldOf("result").forGetter(r -> r.result)
    ).apply(instance, FairySourceRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FairySourceRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, r -> r.ingredient,
            ItemStack.STREAM_CODEC, r -> r.result,
            FairySourceRecipe::new);

    private final Ingredient ingredient;
    private final ItemStack result;

    public FairySourceRecipe(Ingredient ingredient, ItemStack result) {
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

    // ── Recipe<SingleRecipeInput> contract (not used; no inventory, entity-driven) ──

    @Override public boolean matches(@Nonnull SingleRecipeInput input, @Nonnull Level level) {
        return ingredient.test(input.item());
    }

    @Override
    @Nonnull
    public ItemStack assemble(@Nonnull SingleRecipeInput input, @Nonnull HolderLookup.Provider registries) { return result.copy(); }

    @Override public boolean canCraftInDimensions(int w, int h) { return true; }

    /** Resolved by direct fluid-interaction code, not a crafting grid; keep it out of the recipe book. */
    @Override public boolean isSpecial() { return true; }

    @Override
    @Nonnull
    public ItemStack getResultItem(@Nonnull HolderLookup.Provider registries) { return result.copy(); }

    @Override
    @Nonnull
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, ingredient);
    }

    @Override @Nonnull public RecipeSerializer<?> getSerializer() { return ModRecipes.FAIRY_SOURCE_SERIALIZER.get(); }
    @Override @Nonnull public RecipeType<?> getType()             { return ModRecipes.FAIRY_SOURCE_TYPE.get(); }

    // ── Serializer ────────────────────────────────────────────────────────────

    public static class Serializer implements RecipeSerializer<FairySourceRecipe> {
        @Override @Nonnull public MapCodec<FairySourceRecipe> codec() { return CODEC; }
        @Override @Nonnull public StreamCodec<RegistryFriendlyByteBuf, FairySourceRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
