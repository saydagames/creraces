package mc.sayda.creraces.item;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.registry.ModMobEffects;
import mc.sayda.creraces.util.RaceUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

public class DryadAppleItem extends Item {
    // Nymph's Call is for nymphs and every race descended from them (dryads, oreads, aurai, naiads).
    private static final ResourceLocation NYMPH = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "nymph");

    private final Variant variant;

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
                    .saturationModifier(this.saturation)
                    .alwaysEdible();

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
    public ItemStack finishUsingItem(ItemStack stack, Level level, @Nonnull LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);

        if (!level.isClientSide && entity instanceof Player player
                && RaceUtils.isRaceOrDescendant(RaceUtils.raceOf(player), NYMPH)) {
            player.addEffect(new MobEffectInstance(ModMobEffects.NYMPH_CALL, 18000, 0, false, false, true));
        }

        return result;
    }
}
