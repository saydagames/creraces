package mc.sayda.creraces.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("jumping")
    boolean isJumping();

    @Accessor("jumping")
    void setJumping(boolean jumping);

    @Invoker("isAffectedByFluids")
    boolean callIsAffectedByFluids();

    @Invoker("jumpFromGround")
    void callJumpFromGround();

    @Invoker("jumpInLiquid")
    void callJumpInLiquid(net.minecraft.tags.TagKey<net.minecraft.world.level.material.Fluid> tag);
}
