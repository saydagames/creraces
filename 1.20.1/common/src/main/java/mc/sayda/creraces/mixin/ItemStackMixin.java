package mc.sayda.creraces.mixin;

import mc.sayda.creraces.registry.ModItemTags;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    // Keeps #creraces:unenchantable items out of the enchanting table; see EnchantmentMixin.
    @Inject(method = "isEnchantable", at = @At("HEAD"), cancellable = true)
    private void creraces$rejectUnenchantable(CallbackInfoReturnable<Boolean> cir) {
        if (((ItemStack) (Object) this).is(ModItemTags.UNENCHANTABLE)) {
            cir.setReturnValue(false);
        }
    }
}
