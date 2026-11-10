package mc.sayda.creraces.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import mc.sayda.creraces.race.StandInEntityTypes;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pufferfish;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Predicate;

@Mixin(Pufferfish.class)
public abstract class PufferfishMixin {

    // Pufferfish don't puff up at aquatic players, like they don't at other sea life.
    @ModifyExpressionValue(method = "<clinit>", at = @At(value = "FIELD", opcode = Opcodes.GETSTATIC,
            target = "Lnet/minecraft/world/entity/animal/Pufferfish;SCARY_MOB:Ljava/util/function/Predicate;"))
    private static Predicate<LivingEntity> creraces$skipStandInSeaLife(Predicate<LivingEntity> selector) {
        return StandInEntityTypes.excluding(selector, EntityTypeTags.NOT_SCARY_FOR_PUFFERFISH);
    }
}
