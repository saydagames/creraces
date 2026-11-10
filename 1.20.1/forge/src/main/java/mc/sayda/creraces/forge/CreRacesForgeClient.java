package mc.sayda.creraces.forge;

import mc.sayda.creraces.item.SpiritCompassItem;
import mc.sayda.creraces.registry.ModFluids;
import mc.sayda.creraces.registry.ModItems;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-only setup, kept out of CreRacesForge because that class also loads on a dedicated server. */
public final class CreRacesForgeClient {
    private CreRacesForgeClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> {
            // Translucent layer for the fluids themselves, so LiquidBlockRenderer alpha-blends them
            ItemBlockRenderTypes.setRenderLayer(ModFluids.FAIRY_SOURCE.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.FAIRY_SOURCE_FLOWING.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.ETERVEIL.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.ETERVEIL_FLOWING.get(), RenderType.translucent());
            // Spirit Compass needle model predicate
            ItemProperties.register(ModItems.SPIRIT_COMPASS.get(), new ResourceLocation("creraces", "angle"),
                    (stack, level, entity, seed) -> SpiritCompassItem.needleAngle(stack, level, entity));
        }));
    }
}
