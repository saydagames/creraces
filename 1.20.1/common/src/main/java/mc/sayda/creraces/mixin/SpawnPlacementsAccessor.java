package mc.sayda.creraces.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes SpawnPlacements' private registration method so modded mobs can register a spawn placement. */
@Mixin(SpawnPlacements.class)
public interface SpawnPlacementsAccessor {
    @Invoker("register")
    static <T extends Mob> void creraces$callRegister(EntityType<T> type, SpawnPlacements.Type placementType,
            Heightmap.Types heightmapType, SpawnPlacements.SpawnPredicate<T> predicate) {
        throw new UnsupportedOperationException("Mixin not applied");
    }
}
