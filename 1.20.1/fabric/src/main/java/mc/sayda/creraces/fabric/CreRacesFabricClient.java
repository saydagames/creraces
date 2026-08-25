
package mc.sayda.creraces.fabric;

import mc.sayda.creraces.client.CreRacesClient;
import mc.sayda.creraces.registry.ModFluids;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class CreRacesFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Safe to call here: Fabric runs every "main" entrypoint (including TerraBlender's, which
        // loads TerraBlender.CONFIG) to completion before any "client" entrypoint starts.
        mc.sayda.creraces.worldgen.VeilwoodBiomeInjector.init();

        FluidRenderHandlerRegistry.INSTANCE.register(
                ModFluids.FAIRY_SOURCE.get(),
                ModFluids.FAIRY_SOURCE_FLOWING.get(),
                new SimpleFluidRenderHandler(
                        new ResourceLocation("creraces", "block/fairy_source_still"),
                        new ResourceLocation("creraces", "block/fairy_source_flow"),
                        new ResourceLocation("creraces", "block/fairy_source_overlay")));

        // Fluid render type must be translucent separately so the in-world liquid
        // renders with alpha blending (analogous to water's translucent layer).
        BlockRenderLayerMap.INSTANCE.putFluids(
                RenderType.translucent(),
                ModFluids.FAIRY_SOURCE.get(),
                ModFluids.FAIRY_SOURCE_FLOWING.get());

        // Saplings have transparent pixels and must use cutout so they don't render black.
        BlockRenderLayerMap.INSTANCE.putBlock(
                mc.sayda.creraces.registry.ModBlocks.DRYAD_SAPLING.get(),
                RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(
                mc.sayda.creraces.registry.ModBlocks.VEIL_WILLOW_SAPLING.get(),
                RenderType.cutout());

        CreRacesClient.init();
        // MenuGUIScreen is registered via MenuRegistry.registerScreenFactory inside
        // CreRacesClient.init() -> CLIENT_SETUP. Architectury delegates this call to
        // MenuScreens.register on Fabric, so a separate direct call here caused a
        // duplicate registration crash (IllegalStateException: Duplicate registration
        // for creraces:menu_gui).

        // Spirit Compass angle model predicate
        net.minecraft.client.renderer.item.ItemProperties.register(
                mc.sayda.creraces.registry.ModItems.SPIRIT_COMPASS.get(),
                new ResourceLocation("creraces", "angle"),
                (stack, level, entity, seed) -> {
                    if (entity == null) return 0f;
                    net.minecraft.nbt.CompoundTag tag = stack.getTag();
                    if (tag == null || !tag.getBoolean("HasTarget")) {
                        return level != null ? (float) ((level.getGameTime() % 32) / 32.0) : 0f;
                    }
                    double dx = tag.getInt("TargetX") - entity.getX();
                    double dz = tag.getInt("TargetZ") - entity.getZ();
                    double worldAngle = Math.toDegrees(Math.atan2(dx, dz));
                    double relative = ((worldAngle - entity.getYRot()) % 360 + 360) % 360;
                    return (float) (relative / 360.0);
                });
    }
}
