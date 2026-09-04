package mc.sayda.creraces.fabric;

import mc.sayda.creraces.CreRaces;
import net.fabricmc.api.ModInitializer;

public class CreRacesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        mc.sayda.creraces.config.fabric.FabricConfig.load();
        mc.sayda.creraces.util.PlatformServices.burnTimeHandler = stack -> net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
                .getFuel().getOrDefault(stack.getItem(), 0);
        // Trinkets isn't a hard dependency (fabric.mod.json has no "depends" entry for it), so it can
        // be absent. Only assign the Trinkets-backed lookup when it's actually loaded; the default
        // beltFinder (Optional.empty()) otherwise avoids a NoClassDefFoundError on TrinketsApi the
        // moment a player tries to use the belt.
        if (dev.architectury.platform.Platform.isModLoaded("trinkets")) {
                mc.sayda.creraces.util.PlatformServices.beltFinder = mc.sayda.creraces.fabric.compat.TrinketsBeltCompat::findBelt;
        }
        CreRaces.init();
        CreRacesFabricVillagerTrades.init();
        CreRacesFabricVillageStructures.init();
        // VeilwoodBiomeInjector.init() is NOT called here: TerraBlender is also a "main" entrypoint
        // mod, and Fabric doesn't guarantee entrypoint order within the same category, so calling
        // Regions.register() here can race TerraBlender's own config loading and NPE. It's called
        // from CreRacesFabricClient/CreRacesFabricServer instead, since Fabric always runs every
        // "main" entrypoint to completion before any "client"/"server" entrypoint starts.
        net.fabricmc.fabric.api.biome.v1.BiomeModifications.addFeature(
                net.fabricmc.fabric.api.biome.v1.BiomeSelectors.foundInOverworld(),
                net.minecraft.world.level.levelgen.GenerationStep.Decoration.VEGETAL_DECORATION,
                net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.PLACED_FEATURE,
                        new net.minecraft.resources.ResourceLocation("creraces", "essence_vortex")));
        net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistry.registerPotionRecipe(
            net.minecraft.world.item.alchemy.Potions.AWKWARD,
            net.minecraft.world.item.crafting.Ingredient.of(
                mc.sayda.creraces.registry.ModItems.VEIL_BLOOM_ITEM.get()),
            mc.sayda.creraces.registry.ModPotions.REVEALING.get());
    }
}
