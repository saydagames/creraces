package mc.sayda.creraces.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.effect.FrozenEffect;
import mc.sayda.creraces.effect.LifeDrainEffect;
import mc.sayda.creraces.effect.TalonStrikeEffect;
import mc.sayda.creraces.effect.TrollCurseEffect;
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

        public static final RegistrySupplier<MobEffect> NYMPH_CALL = MOB_EFFECTS.register("nymph_call",
                        () -> new mc.sayda.creraces.effect.SimpleEffect(MobEffectCategory.BENEFICIAL, 0x00FF00));

        public static final RegistrySupplier<MobEffect> ROOTED = MOB_EFFECTS.register("rooted",
                        () -> new mc.sayda.creraces.effect.SimpleEffect(MobEffectCategory.HARMFUL, 0x654321)
                                        .addAttributeModifier(
                                                        net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,
                                                        "68ac4f36-0016-4680-b3be-c6a4c37a0265", -1.0D,
                                                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL));

        public static final RegistrySupplier<MobEffect> DISARMED = MOB_EFFECTS.register("disarmed",
                        () -> new mc.sayda.creraces.effect.SimpleEffect(MobEffectCategory.HARMFUL, 0x808080));

        public static final RegistrySupplier<MobEffect> STUNNED = MOB_EFFECTS.register("stunned",
                        () -> new mc.sayda.creraces.effect.SimpleEffect(MobEffectCategory.HARMFUL, 0xFFFF00)
                                        .addAttributeModifier(
                                                        net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,
                                                        "4cb5918e-e21c-480d-a10d-b1f3712f1e06", -1.0D,
                                                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL));

        /**
         * Frozen - Troll sunlight effect. Roots (speed ×-1) and disarms the entity.
         * PlayerMixin checks FROZEN alongside DISARMED to block attacking and
         * interactions.
         */
        public static final RegistrySupplier<MobEffect> FROZEN = MOB_EFFECTS.register("frozen",
                        () -> new FrozenEffect(MobEffectCategory.HARMFUL, 0xADD8E6));

        /**
         * Troll's Curse - applied by the Troll Pillar to nearby entities.
         * Buffs Troll-race allies (Speed I), debuffs others (Slowness I + Weakness I).
         */
        public static final RegistrySupplier<MobEffect> TROLL_CURSE = MOB_EFFECTS.register("troll_curse",
                        () -> new TrollCurseEffect());

        public static final RegistrySupplier<MobEffect> DIZZINESS = MOB_EFFECTS.register("dizziness",
                        () -> new mc.sayda.creraces.effect.DizzinessEffect(MobEffectCategory.HARMFUL, 0x87CEEB));

        /**
         * Featherstorm - Harpy beneficial effect.
         * Granted to the player on hit.
         */
        public static final RegistrySupplier<MobEffect> FEATHERSTORM = MOB_EFFECTS.register("featherstorm",
                        () -> new mc.sayda.creraces.effect.FeatherstormEffect());

        /**
         * Applied by Troll to "maul-mark" a target - reduces defenses for the follow-up
         * hit.
         */
        public static final RegistrySupplier<MobEffect> MAUL = MOB_EFFECTS.register("maul",
                        () -> new mc.sayda.creraces.effect.SimpleEffect(MobEffectCategory.HARMFUL, 0x8B4513));

        /** Applied by Mermaid's Spicy Whirlpool ability to nearby enemies in water. */
        public static final RegistrySupplier<MobEffect> BOILING = MOB_EFFECTS.register("boiling",
                        () -> new mc.sayda.creraces.effect.SimpleEffect(MobEffectCategory.HARMFUL, 0xFF4500));

        /** Stacking bleed debuff tracked on Lycan kill chains. */
        public static final RegistrySupplier<MobEffect> BLEEDING = MOB_EFFECTS.register("bleeding",
                        () -> new mc.sayda.creraces.effect.SimpleEffect(MobEffectCategory.HARMFUL, 0xCC0000));

        // Element effects function as simple markers/beneficial states
        public static final RegistrySupplier<MobEffect> AIR_ELEMENT = MOB_EFFECTS.register("air_element",
                        () -> new mc.sayda.creraces.effect.SimpleEffect(MobEffectCategory.BENEFICIAL, 0x87CEEB));
        public static final RegistrySupplier<MobEffect> EARTH_ELEMENT = MOB_EFFECTS.register("earth_element",
                        () -> new mc.sayda.creraces.effect.SimpleEffect(MobEffectCategory.BENEFICIAL, 0x8B4513));
        public static final RegistrySupplier<MobEffect> FIRE_ELEMENT = MOB_EFFECTS.register("fire_element",
                        () -> new mc.sayda.creraces.effect.SimpleEffect(MobEffectCategory.BENEFICIAL, 0xFF4500));
        public static final RegistrySupplier<MobEffect> WATER_ELEMENT = MOB_EFFECTS.register("water_element",
                        () -> new mc.sayda.creraces.effect.SimpleEffect(MobEffectCategory.BENEFICIAL, 0x0000FF));

        public static final RegistrySupplier<MobEffect> BLINDED = MOB_EFFECTS.register("blinded",
                        () -> new mc.sayda.creraces.effect.BlindedEffect());
        public static final RegistrySupplier<MobEffect> FOUL_PLAY = MOB_EFFECTS.register("foul_play",
                        () -> new mc.sayda.creraces.effect.SimpleEffect(MobEffectCategory.HARMFUL, 0x4B0082));
        public static final RegistrySupplier<MobEffect> INVULNERABILITY = MOB_EFFECTS.register("invulnerability",
                        () -> new mc.sayda.creraces.effect.InvulnerabilityEffect());
        public static final RegistrySupplier<MobEffect> THORNS = MOB_EFFECTS.register("thorns",
                        () -> new mc.sayda.creraces.effect.ThornsEffect());
        public static final RegistrySupplier<MobEffect> SHIELD = MOB_EFFECTS.register("shield",
                        () -> new mc.sayda.creraces.effect.ShieldEffect(MobEffectCategory.BENEFICIAL, 0xFFFF00));
        public static final RegistrySupplier<MobEffect> AP_SHIELD = MOB_EFFECTS.register("ap_shield",
                        () -> new mc.sayda.creraces.effect.ShieldEffect(MobEffectCategory.BENEFICIAL, 0x00FFFF));
        public static final RegistrySupplier<MobEffect> AD_SHIELD = MOB_EFFECTS.register("ad_shield",
                        () -> new mc.sayda.creraces.effect.ShieldEffect(MobEffectCategory.BENEFICIAL, 0xFF00FF));

        /** Grievous Wounds - reduces healing received by 40% (multiplier 0.6). */
        public static final RegistrySupplier<MobEffect> GRIEVOUS_WOUNDS = MOB_EFFECTS.register("grievous_wounds",
                        () -> new mc.sayda.creraces.effect.SimpleEffect(MobEffectCategory.HARMFUL, 0x8B0000)
                                        .addAttributeModifier(
                                                        mc.sayda.creraces.registry.ModAttributes.HEALING_RECEIVED.get(),
                                                        "a0f3d95c-9c9e-4e8a-b1f3-d9d7a2d5d361", -0.4D,
                                                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));

        public static void register() {
                MOB_EFFECTS.register();
        }
}
