package mc.sayda.creraces.registry;

import com.mojang.serialization.Codec;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;

/**
 * Mod data components. 1.21 matches villager trade costs with ItemCost's DataComponentPredicate,
 * which compares a component for exact equality, so trade-relevant state has to live in its own
 * component rather than inside the scroll's CUSTOM_DATA blob (which also carries per-quest data
 * that differs on every scroll).
 */
public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(CreRaces.MODID, Registries.DATA_COMPONENT_TYPE);

    /** Tier of a COMPLETED quest scroll. Absent while the quest is still active. */
    public static final RegistrySupplier<DataComponentType<Integer>> QUEST_GRADE =
            COMPONENTS.register("quest_grade", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    public static void register() {
        COMPONENTS.register();
    }
}
