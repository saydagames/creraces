package mc.sayda.creraces.registry;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

public class ModWoodTypes {
    public static final WoodType DRYAD = mc.sayda.creraces.mixin.WoodTypeAccessor.creraces$callRegister(
        new WoodType("dryad", BlockSetType.OAK, SoundType.WOOD, SoundType.HANGING_SIGN,
            SoundEvents.FENCE_GATE_CLOSE, SoundEvents.FENCE_GATE_OPEN));

    public static final WoodType VEIL_WILLOW = mc.sayda.creraces.mixin.WoodTypeAccessor.creraces$callRegister(
        new WoodType("veil_willow", BlockSetType.OAK, SoundType.WOOD, SoundType.HANGING_SIGN,
            SoundEvents.FENCE_GATE_CLOSE, SoundEvents.FENCE_GATE_OPEN));

    public static void init() {}
}
