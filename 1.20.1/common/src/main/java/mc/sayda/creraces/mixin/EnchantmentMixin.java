package mc.sayda.creraces.mixin;

import mc.sayda.creraces.registry.ModItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Items in #creraces:unenchantable take no enchantments from an anvil or /enchant. 1.21.1 needs no
// such tag, since there an item only takes the enchantments its item tags allow.
@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {

    @Inject(method = "canEnchant", at = @At("HEAD"), cancellable = true)
    private void creraces$rejectUnenchantable(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.is(ModItemTags.UNENCHANTABLE)) {
            cir.setReturnValue(false);
        }
    }
}
