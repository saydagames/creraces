package mc.sayda.creraces.fabric.mixin;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fabric-only: 1.21 added BlockEntity.isValidBlockState(), called from the constructor, which
 * checks the raw type field set by super() rather than the (possibly overridden) getType()
 * method. A block entity that extends a vanilla class with a hardcoded type in its constructor
 * (e.g. ToriiBellBlockEntity extends BellBlockEntity, which always constructs with
 * BlockEntityType.BELL) but overrides getType() to report its own custom type therefore fails
 * this check for any block that isn't in vanilla's own type's validBlocks, even though getType()
 * correctly reports a type whose validBlocks does include it. 1.20.1 had no such check at
 * construction time, so this never surfaced there. NeoForge's own vanilla patches already fix
 * this upstream (their isValidBlockState() calls getType() directly, confirmed by decompiling
 * their patched jar) so this mixin's target doesn't exist there and it must NOT be common (it
 * fails to apply and crashes bootstrap on NeoForge, "required" mixins being fatal on failure).
 * Plain Fabric uses the unpatched vanilla jar and genuinely has the bug, so it lives here only.
 */
@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin {
    @Redirect(method = "isValidBlockState", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/level/block/entity/BlockEntity;type:Lnet/minecraft/world/level/block/entity/BlockEntityType;"))
    private BlockEntityType<?> creraces$useReportedType(BlockEntity instance) {
        return instance.getType();
    }
}
