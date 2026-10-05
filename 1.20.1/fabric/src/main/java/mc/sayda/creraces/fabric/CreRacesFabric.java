package mc.sayda.creraces.fabric;

import dev.architectury.platform.Platform;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.config.fabric.FabricConfig;
import mc.sayda.creraces.fabric.compat.TrinketsBeltCompat;
import mc.sayda.creraces.registry.ModItems;
import mc.sayda.creraces.registry.ModPotions;
import mc.sayda.creraces.util.PlatformServices;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.levelgen.GenerationStep;

public class CreRacesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricConfig.load();
        PlatformServices.burnTimeHandler = stack -> AbstractFurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0);
        // Trinkets is not a hard dependency (fabric.mod.json has no "depends" entry for it), so only
        // assign the Trinkets-backed lookup when it is actually loaded.
        if (Platform.isModLoaded("trinkets")) {
            PlatformServices.beltFinder = TrinketsBeltCompat::findBelt;
        }
        CreRaces.init();
        CreRacesFabricVillagerTrades.init();
        CreRacesFabricVillageStructures.init();
        // VeilwoodBiomeInjector.init() is NOT called here: TerraBlender is also a "main" entrypoint
        // mod, and Fabric doesn't guarantee entrypoint order within the same category, so calling
        // Regions.register() here can race TerraBlender's own config loading and NPE. It's called
        // from CreRacesFabricClient/CreRacesFabricServer instead, since Fabric always runs every
        // "main" entrypoint to completion before any "client"/"server" entrypoint starts.
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.VEGETAL_DECORATION,
                ResourceKey.create(Registries.PLACED_FEATURE, new ResourceLocation("creraces", "essence_vortex")));
        FabricBrewingRecipeRegistry.registerPotionRecipe(
                Potions.AWKWARD,
                Ingredient.of(ModItems.VEIL_BLOOM_ITEM.get()),
                ModPotions.REVEALING.get());
    }
}
