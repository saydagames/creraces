package mc.sayda.creraces.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.recipe.FairySourceRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public class ModRecipes {

    private static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(CreRaces.MODID, Registries.RECIPE_TYPE);

    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(CreRaces.MODID, Registries.RECIPE_SERIALIZER);

    public static final RegistrySupplier<RecipeType<FairySourceRecipe>> FAIRY_SOURCE_TYPE =
            TYPES.register("fairy_source", () -> new RecipeType<FairySourceRecipe>() {
                @Override public String toString() { return CreRaces.MODID + ":fairy_source"; }
            });

    public static final RegistrySupplier<RecipeSerializer<FairySourceRecipe>> FAIRY_SOURCE_SERIALIZER =
            SERIALIZERS.register("fairy_source", FairySourceRecipe.Serializer::new);

    public static void register() {
        TYPES.register();
        SERIALIZERS.register();
    }
}
