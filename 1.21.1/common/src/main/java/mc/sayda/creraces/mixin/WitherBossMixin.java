package mc.sayda.creraces.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import mc.sayda.creraces.race.StandInEntityTypes;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Predicate;

// The Wither doesn't target undead players, like undead mobs. Its hurt() check is left alone so they
// can still damage it.
@Mixin(WitherBoss.class)
public abstract class WitherBossMixin {

    // Builds TARGETING_CONDITIONS, used to pick head targets.
    @ModifyExpressionValue(method = "<clinit>", at = @At(value = "FIELD", opcode = Opcodes.GETSTATIC,
            target = "Lnet/minecraft/world/entity/boss/wither/WitherBoss;LIVING_ENTITY_SELECTOR:Ljava/util/function/Predicate;"))
    private static Predicate<LivingEntity> creraces$skipStandInFriends(Predicate<LivingEntity> selector) {
        return StandInEntityTypes.excluding(selector, EntityTypeTags.WITHER_FRIENDS);
    }

    // The main target goal.
    @ModifyExpressionValue(method = "registerGoals", at = @At(value = "FIELD", opcode = Opcodes.GETSTATIC,
            target = "Lnet/minecraft/world/entity/boss/wither/WitherBoss;LIVING_ENTITY_SELECTOR:Ljava/util/function/Predicate;"))
    private Predicate<LivingEntity> creraces$skipStandInFriendsInGoal(Predicate<LivingEntity> selector) {
        return StandInEntityTypes.excluding(selector, EntityTypeTags.WITHER_FRIENDS);
    }
}
