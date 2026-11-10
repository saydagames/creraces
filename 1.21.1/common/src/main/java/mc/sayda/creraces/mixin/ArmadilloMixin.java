package mc.sayda.creraces.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import mc.sayda.creraces.race.StandInEntityTypes;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Armadillo.class)
public abstract class ArmadilloMixin {

    // Armadillos curl up near undead players like they do near undead mobs.
    @WrapOperation(method = "isScaredBy", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/EntityType;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean creraces$checkStandInTypes(EntityType<?> type, TagKey<EntityType<?>> tag,
            Operation<Boolean> original, @Local(argsOnly = true) LivingEntity entity) {
        return original.call(type, tag) || StandInEntityTypes.standInIs(entity, tag);
    }
}
