package mc.sayda.creraces.village;

import com.mojang.datafixers.util.Pair;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.mixin.StructureTemplatePoolAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

import java.util.ArrayList;
import java.util.List;

/**
 * Injects the Guild Receptionist's guild_hut into all five vanilla village house pools
 * (plains/savanna/taiga/snowy/desert) at server start. Pool weight is not the real spawn rate -
 * village generation skips any candidate that doesn't fit against already-placed houses.
 */
public final class VillageStructureInjector {
    private VillageStructureInjector() {
    }

    private static final int WEIGHT = 10;
    private static final ResourceLocation GUILD_HUT = new ResourceLocation("creraces", "guild_hut");

    public static void inject(MinecraftServer server) {
        Registry<StructureTemplatePool> poolRegistry = server.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        Registry<StructureProcessorList> processorRegistry = server.registryAccess().registryOrThrow(Registries.PROCESSOR_LIST);

        addPiece(poolRegistry, processorRegistry, "plains", "creraces:guild_hut_plains");
        addPiece(poolRegistry, processorRegistry, "savanna", "creraces:oak_to_acacia");
        addPiece(poolRegistry, processorRegistry, "taiga", "creraces:oak_to_spruce");
        // Snowy reuses the taiga (spruce) swap - vanilla's own snowy houses are spruce-trimmed too.
        addPiece(poolRegistry, processorRegistry, "snowy", "creraces:oak_to_spruce");
        // Desert has no vanilla wood-reskin convention (its houses are sandstone-dominant with
        // oak trim, same as plains) - reuse the plains processor rather than inventing a mapping.
        addPiece(poolRegistry, processorRegistry, "desert", "creraces:guild_hut_plains");
    }

    private static void addPiece(Registry<StructureTemplatePool> poolRegistry,
            Registry<StructureProcessorList> processorRegistry, String biome, String processorId) {
        ResourceLocation poolId = new ResourceLocation("minecraft", "village/" + biome + "/houses");
        StructureTemplatePool pool = poolRegistry.get(poolId);
        if (pool == null) {
            CreRaces.LOGGER.warn("[CreRaces] Could not find village pool {} to inject guild_hut into.", poolId);
            return;
        }

        Holder<StructureProcessorList> processor = processorRegistry.getHolderOrThrow(
                ResourceKey.create(Registries.PROCESSOR_LIST, new ResourceLocation(processorId)));

        SinglePoolElement piece = SinglePoolElement.single(GUILD_HUT.toString(), processor)
                .apply(StructureTemplatePool.Projection.RIGID);

        StructureTemplatePoolAccessor accessor = (StructureTemplatePoolAccessor) pool;
        for (int i = 0; i < WEIGHT; i++) {
            accessor.creraces$getTemplates().add(piece);
        }
        List<Pair<StructurePoolElement, Integer>> rawTemplates = new ArrayList<>(accessor.creraces$getRawTemplates());
        rawTemplates.add(Pair.of(piece, WEIGHT));
        accessor.creraces$setRawTemplates(rawTemplates);

        CreRaces.LOGGER.info("[CreRaces] Injected guild_hut into {} (weight {}).", poolId, WEIGHT);
    }
}
