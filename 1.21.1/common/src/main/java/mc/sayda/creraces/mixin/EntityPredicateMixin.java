package mc.sayda.creraces.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import mc.sayda.creraces.race.StandInEntityTypes;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.EntityTypePredicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityPredicate.class)
public abstract class EntityPredicateMixin {

    // Data-driven type checks (enchantment effects such as Smite and Impaling, advancements, loot
    // conditions) also accept the player's stand-in types.
    @WrapOperation(method = "matches(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/advancements/critereon/EntityTypePredicate;matches(Lnet/minecraft/world/entity/EntityType;)Z"))
    private boolean creraces$checkStandInTypes(EntityTypePredicate predicate, EntityType<?> type,
            Operation<Boolean> original, @Local(argsOnly = true) Entity entity) {
        return original.call(predicate, type)
                || StandInEntityTypes.anyMatch(entity, standIn -> original.call(predicate, standIn));
    }
}
