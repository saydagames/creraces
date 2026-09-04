package mc.sayda.creraces.registry;

import com.google.common.collect.ImmutableSet;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.block.QuestBoardBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.state.BlockState;

public class ModPoiTypes {
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(CreRaces.MODID,
            Registries.POINT_OF_INTEREST_TYPE);

    // Matches only the MASTER cell of the Quest Board's 3x2 wall - a PoiType can cover a
    // subset of one block's states (vanilla does the same for beds, filtering to BedPart.HEAD).
    public static final RegistrySupplier<PoiType> GUILD_RECEPTIONIST = POI_TYPES.register("guild_receptionist",
            () -> {
                ImmutableSet<BlockState> masterStates = ModBlocks.QUEST_BOARD.get().getStateDefinition()
                        .getPossibleStates().stream()
                        .filter(QuestBoardBlock::isMaster)
                        .collect(ImmutableSet.toImmutableSet());
                return new PoiType(masterStates, 1, 1);
            });

    public static void register() {
        POI_TYPES.register();
    }
}
