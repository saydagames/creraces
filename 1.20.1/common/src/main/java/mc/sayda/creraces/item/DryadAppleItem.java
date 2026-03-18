package mc.sayda.creraces.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.registry.ModMobEffects;

public class DryadAppleItem extends Item {
    private final Variant variant;
    private static final net.minecraft.resources.ResourceLocation DRYAD_RACE = new net.minecraft.resources.ResourceLocation("creraces", "dryad");

    public enum Variant {
        DEFAULT(4, 2.4f, Rarity.COMMON),
        GOLDEN(4, 9.6f, Rarity.RARE),
        ENCHANTED(4, 9.6f, Rarity.EPIC);

        final int nutrition;
        final float saturation;
        final Rarity rarity;

        Variant(int nutrition, float saturation, Rarity rarity) {
            this.nutrition = nutrition;
            this.saturation = saturation;
            this.rarity = rarity;
        }

        public FoodProperties getFoodProperties() {
            FoodProperties.Builder builder = new FoodProperties.Builder()
                    .nutrition(this.nutrition)
                    .saturationMod(this.saturation)
                    .alwaysEat();
            
            if (this == GOLDEN) {
                builder.effect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1), 1.0F)
                       .effect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 0), 1.0F);
            } else if (this == ENCHANTED) {
                builder.effect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1), 1.0F)
                       .effect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 3), 1.0F)
                       .effect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 6000, 0), 1.0F)
                       .effect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0), 1.0F);
            }
            return builder.build();
        }
    }

    public DryadAppleItem(Variant variant, Properties properties) {
        super(properties.rarity(variant.rarity).food(variant.getFoodProperties()));
        this.variant = variant;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return variant == Variant.ENCHANTED || super.isFoil(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        
        if (!level.isClientSide && entity instanceof Player player) {
            DataUtils.getVariables(player).ifPresent(vars -> {
                if (DRYAD_RACE.equals(vars.getRace())) {
                    // Apply Nymph Call for 15 minutes
                    player.addEffect(new MobEffectInstance(ModMobEffects.NYMPH_CALL.get(), 18000, 0));
                }
            });
        }
        
        return result;
    }
}
