package mc.sayda.creraces.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.effect.LifeDrainEffect;
import mc.sayda.creraces.effect.TalonStrikeEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class ModMobEffects {
        public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(CreRaces.MODID,
                        Registries.MOB_EFFECT);

        public static final RegistrySupplier<MobEffect> TALON_STRIKE = MOB_EFFECTS.register("talon_strike",
                        () -> new TalonStrikeEffect(MobEffectCategory.HARMFUL, 0x8B0000));
        public static final RegistrySupplier<MobEffect> LIFE_DRAIN = MOB_EFFECTS.register("life_drain",
                        () -> new LifeDrainEffect(MobEffectCategory.HARMFUL, 0x4B0082));
        public static final RegistrySupplier<MobEffect> RAT_VENOM = MOB_EFFECTS.register("rat_venom",
                        () -> new mc.sayda.creraces.effect.RatVenomEffect());
        public static final RegistrySupplier<MobEffect> TRUE_INVISIBILITY = MOB_EFFECTS.register("true_invisibility",
                        () -> new mc.sayda.creraces.effect.TrueInvisibilityEffect(MobEffectCategory.BENEFICIAL,
                                        0xFFFFFF));
        public static final RegistrySupplier<MobEffect> SOGGY = MOB_EFFECTS.register("soggy",
                        () -> new mc.sayda.creraces.effect.SoggyEffect());

        public static void register() {
                MOB_EFFECTS.register();
        }
}
