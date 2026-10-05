package mc.sayda.creraces.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Exposes PoiTypes.TYPE_BY_STATE, which vanilla only fills at bootstrap, so modded PoiTypes can add
 * their own states afterwards.
 */
@Mixin(PoiTypes.class)
public interface PoiTypesAccessor {
    @Accessor("TYPE_BY_STATE")
    static Map<BlockState, Holder<PoiType>> creraces$getTypeByState() {
        throw new UnsupportedOperationException("Mixin not applied");
    }
}
