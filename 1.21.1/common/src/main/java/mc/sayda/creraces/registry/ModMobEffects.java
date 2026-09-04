package mc.sayda.creraces.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.effect.FrozenEffect;
import mc.sayda.creraces.effect.LifeDrainEffect;
import mc.sayda.creraces.effect.TalonStrikeEffect;
import mc.sayda.creraces.effect.TrollCurseEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class ModMobEffects {
        public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(CreRaces.MODID,
                        Registries.MOB_EFFECT);

        private static final RegistrySupplier<MobEffect> TALON_STRIKE_ENTRY = MOB_EFFECTS.register("talon_strike",
                        () -> new TalonStrikeEffect(MobEffectCategory.HARMFUL, 0x8B0000));
        private static final RegistrySupplier<MobEffect> LIFE_DRAIN_ENTRY = MOB_EFFECTS.register("life_drain",
                        () -> new LifeDrainEffect(MobEffectCategory.HARMFUL, 0x4B0082));
        private static final RegistrySupplier<MobEffect> RAT_VENOM_ENTRY = MOB_EFFECTS.register("rat_venom",
                        () -> new mc.sayda.creraces.effect.RatVenomEffect());
        private static final RegistrySupplier<MobEffect> TRUE_INVISIBILITY_ENTRY = MOB_EFFECTS.register("true_invisibility",
                        () -> new mc.sayda.creraces.effect.TrueInvisibilityEffect(MobEffectCategory.BENEFICIAL,
                                        0xFFFFFF));
        private static final RegistrySupplier<MobEffect> SOGGY_ENTRY = MOB_EFFECTS.register("soggy",
                        () -> new mc.sayda.creraces.effect.SoggyEffect());

        private static final RegistrySupplier<MobEffect> NYMPH_CALL_ENTRY = MOB_EFFECTS.register("nymph_call",
                        () -> new mc.sayda.creraces.effect.SimpleEffect(MobEffectCategory.BENEFICIAL, 0x00FF00));

        private static final RegistrySupplier<MobEffect> ROOTED_ENTRY = MOB_EFFECTS.register("rooted",
                        () -> new mc.sayda.creraces.effect.SimpleEffect(MobEffectCategory.HARMFUL, 0x654321)
                                        .addAttributeModifier(
                                                        net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,
                                                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("creraces", "rooted_speed"), -1.0D,
                                                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

        private static final RegistrySupplier<MobEffect> DISARMED_ENTRY = MOB_EFFECTS.register("disarmed",
                        () -> new mc.sayda.creraces.effect.SimpleEffect(MobEffectCategory.HARMFUL, 0x808080));

        private static final RegistrySupplier<MobEffect> STUNNED_ENTRY = MOB_EFFECTS.register("stunned",
                        () -> new mc.sayda.creraces.effect.SimpleEffect(MobEffectCategory.HARMFUL, 0xFFFF00)
                                        .addAttributeModifier(
                                                        net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,
                                                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("creraces", "stunned_speed"), -1.0D,
                                                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

        /**
         * Frozen - Troll sunlight effect. Roots (speed ×-1) and disarms the entity.
         * PlayerMixin checks FROZEN alongside DISARMED to block attacking and
         * interactions.
         */
        private static final RegistrySupplier<MobEffect> FROZEN_ENTRY = MOB_EFFECTS.register("frozen",
                        () -> new FrozenEffect(MobEffectCategory.HARMFUL, 0xADD8E6));

        /**
         * Troll's Curse - applied by the Troll Pillar to nearby entities.
         * Buffs Troll-race allies (Speed I), debuffs others (Slowness I + Weakness I).
         */
        private static final RegistrySupplier<MobEffect> TROLL_CURSE_ENTRY = MOB_EFFECTS.register("troll_curse",
                        () -> new TrollCurseEffect());

        private static final RegistrySupplier<MobEffect> DIZZINESS_ENTRY = MOB_EFFECTS.register("dizziness",
                        () -> new mc.sayda.creraces.effect.DizzinessEffect(MobEffectCategory.HARMFUL, 0x87CEEB));

        /**
         * Featherstorm - Harpy beneficial effect.
         * Granted to the player on hit.
         */
        private static final RegistrySupplier<MobEffect> FEATHERSTORM_ENTRY = MOB_EFFECTS.register("featherstorm",
                        () -> new mc.sayda.creraces.effect.FeatherstormEffect());

        /**
         * Applied by Troll to "maul-mark" a target - reduces defenses for the follow-up
         * hit.
         */
        private static final RegistrySupplier<MobEffect> MAUL_ENTRY = MOB_EFFECTS.register("maul",
                        () -> new mc.sayda.creraces.effect.SimpleEffect(MobEffectCategory.HARMFUL, 0x8B4513));

        /** Applied by Mermaid's Spicy Whirlpool ability to nearby enemies in water. */
        private static final RegistrySupplier<MobEffect> BOILING_ENTRY = MOB_EFFECTS.register("boiling",
                        () -> new mc.sayda.creraces.effect.BoilingEffect());

        /** Stacking bleed debuff tracked on Lycan kill chains. */
        private static final RegistrySupplier<MobEffect> BLEEDING_ENTRY = MOB_EFFECTS.register("bleeding",
                        () -> new mc.sayda.creraces.effect.BleedingEffect());

        /**
         * Broken Wings: debuff applied to fairies when their wings are damaged.
         * Disables or impairs flight; cleared by standing in fairy_source.
         */
        private static final RegistrySupplier<MobEffect> BROKEN_WINGS_ENTRY = MOB_EFFECTS.register("broken_wings",
                        () -> new mc.sayda.creraces.effect.BrokenWingsEffect());

        /**
         * Fairy Dust: temporarily restores Pehkui MOTION to 1.0 (full flight speed) for 60 seconds.
         */
        private static final RegistrySupplier<MobEffect> FAIRY_DUST_EFFECT_ENTRY = MOB_EFFECTS.register("fairy_dust",
                        () -> new mc.sayda.creraces.effect.FairyDustEffect());

        // Element effects function as simple markers/beneficial states
        private static final RegistrySupplier<MobEffect> AIR_ELEMENT_ENTRY = MOB_EFFECTS.register("air_element",
                        () -> new mc.sayda.creraces.effect.SimpleEffect(MobEffectCategory.BENEFICIAL, 0x87CEEB));
        private static final RegistrySupplier<MobEffect> EARTH_ELEMENT_ENTRY = MOB_EFFECTS.register("earth_element",
                        () -> new mc.sayda.creraces.effect.SimpleEffect(MobEffectCategory.BENEFICIAL, 0x8B4513));
        private static final RegistrySupplier<MobEffect> FIRE_ELEMENT_ENTRY = MOB_EFFECTS.register("fire_element",
                        () -> new mc.sayda.creraces.effect.SimpleEffect(MobEffectCategory.BENEFICIAL, 0xFF4500));
        private static final RegistrySupplier<MobEffect> WATER_ELEMENT_ENTRY = MOB_EFFECTS.register("water_element",
                        () -> new mc.sayda.creraces.effect.SimpleEffect(MobEffectCategory.BENEFICIAL, 0x0000FF));

        private static final RegistrySupplier<MobEffect> BLINDED_ENTRY = MOB_EFFECTS.register("blinded",
                        () -> new mc.sayda.creraces.effect.BlindedEffect());
        private static final RegistrySupplier<MobEffect> CAMOUFLAGE_ENTRY = MOB_EFFECTS.register("camouflage",
                        () -> new mc.sayda.creraces.effect.CamouflageEffect());
        private static final RegistrySupplier<MobEffect> FOUL_PLAY_ENTRY = MOB_EFFECTS.register("foul_play",
                        () -> new mc.sayda.creraces.effect.SimpleEffect(MobEffectCategory.HARMFUL, 0x4B0082));
        private static final RegistrySupplier<MobEffect> INVULNERABILITY_ENTRY = MOB_EFFECTS.register("invulnerability",
                        () -> new mc.sayda.creraces.effect.InvulnerabilityEffect());
        private static final RegistrySupplier<MobEffect> THORNS_ENTRY = MOB_EFFECTS.register("thorns",
                        () -> new mc.sayda.creraces.effect.ThornsEffect());
        private static final RegistrySupplier<MobEffect> SHIELD_ENTRY = MOB_EFFECTS.register("shield",
                        () -> new mc.sayda.creraces.effect.ShieldEffect(MobEffectCategory.BENEFICIAL, 0xFFFF00));
        private static final RegistrySupplier<MobEffect> AP_SHIELD_ENTRY = MOB_EFFECTS.register("ap_shield",
                        () -> new mc.sayda.creraces.effect.ShieldEffect(MobEffectCategory.BENEFICIAL, 0x00FFFF));
        private static final RegistrySupplier<MobEffect> AD_SHIELD_ENTRY = MOB_EFFECTS.register("ad_shield",
                        () -> new mc.sayda.creraces.effect.ShieldEffect(MobEffectCategory.BENEFICIAL, 0xFF00FF));

        /** Instantaneously forces the target into the spirit realm. */
        private static final RegistrySupplier<MobEffect> BANISHMENT_ENTRY = MOB_EFFECTS.register("banishment",
                        () -> new mc.sayda.creraces.effect.BanishmentEffect());

        /** Instantaneously forces the target out of the spirit realm. */
        private static final RegistrySupplier<MobEffect> REVEALING_ENTRY = MOB_EFFECTS.register("revealing",
                        () -> new mc.sayda.creraces.effect.RevealingEffect());

        /** Grievous Wounds - reduces healing received by 40% (multiplier 0.6). */
        private static final RegistrySupplier<MobEffect> GRIEVOUS_WOUNDS_ENTRY = MOB_EFFECTS.register("grievous_wounds",
                        () -> new mc.sayda.creraces.effect.SimpleEffect(MobEffectCategory.HARMFUL, 0x8B0000)
                                        .addAttributeModifier(
                                                        mc.sayda.creraces.registry.ModAttributes.HEALING_RECEIVED,
                                                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("creraces", "grievous_wounds_healing"), -0.4D,
                                                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));


        @SuppressWarnings("null")
        public static boolean isInvisible(net.minecraft.world.entity.LivingEntity entity) {
                return entity.hasEffect(TRUE_INVISIBILITY) || entity.hasEffect(CAMOUFLAGE);
        }


        // Architectury's RegistrySupplier implements Holder, but it is not the registry's own
        // reference holder, so MobEffectInstance could not serialize it ("Unregistered holder").
        // These are bound to the real registry holder as each entry registers; LivingEntity keys
        // its effect map by holder identity, so every add/has/remove call has to use these.
        public static Holder<MobEffect> TALON_STRIKE;
        public static Holder<MobEffect> LIFE_DRAIN;
        public static Holder<MobEffect> RAT_VENOM;
        public static Holder<MobEffect> TRUE_INVISIBILITY;
        public static Holder<MobEffect> SOGGY;
        public static Holder<MobEffect> NYMPH_CALL;
        public static Holder<MobEffect> ROOTED;
        public static Holder<MobEffect> DISARMED;
        public static Holder<MobEffect> STUNNED;
        public static Holder<MobEffect> FROZEN;
        public static Holder<MobEffect> TROLL_CURSE;
        public static Holder<MobEffect> DIZZINESS;
        public static Holder<MobEffect> FEATHERSTORM;
        public static Holder<MobEffect> MAUL;
        public static Holder<MobEffect> BOILING;
        public static Holder<MobEffect> BLEEDING;
        public static Holder<MobEffect> BROKEN_WINGS;
        public static Holder<MobEffect> FAIRY_DUST_EFFECT;
        public static Holder<MobEffect> AIR_ELEMENT;
        public static Holder<MobEffect> EARTH_ELEMENT;
        public static Holder<MobEffect> FIRE_ELEMENT;
        public static Holder<MobEffect> WATER_ELEMENT;
        public static Holder<MobEffect> BLINDED;
        public static Holder<MobEffect> CAMOUFLAGE;
        public static Holder<MobEffect> FOUL_PLAY;
        public static Holder<MobEffect> INVULNERABILITY;
        public static Holder<MobEffect> THORNS;
        public static Holder<MobEffect> SHIELD;
        public static Holder<MobEffect> AP_SHIELD;
        public static Holder<MobEffect> AD_SHIELD;
        public static Holder<MobEffect> BANISHMENT;
        public static Holder<MobEffect> REVEALING;
        public static Holder<MobEffect> GRIEVOUS_WOUNDS;

        public static void register() {
                MOB_EFFECTS.register();
                TALON_STRIKE_ENTRY.listen(v -> TALON_STRIKE = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                LIFE_DRAIN_ENTRY.listen(v -> LIFE_DRAIN = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                RAT_VENOM_ENTRY.listen(v -> RAT_VENOM = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                TRUE_INVISIBILITY_ENTRY.listen(v -> TRUE_INVISIBILITY = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                SOGGY_ENTRY.listen(v -> SOGGY = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                NYMPH_CALL_ENTRY.listen(v -> NYMPH_CALL = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                ROOTED_ENTRY.listen(v -> ROOTED = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                DISARMED_ENTRY.listen(v -> DISARMED = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                STUNNED_ENTRY.listen(v -> STUNNED = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                FROZEN_ENTRY.listen(v -> FROZEN = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                TROLL_CURSE_ENTRY.listen(v -> TROLL_CURSE = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                DIZZINESS_ENTRY.listen(v -> DIZZINESS = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                FEATHERSTORM_ENTRY.listen(v -> FEATHERSTORM = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                MAUL_ENTRY.listen(v -> MAUL = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                BOILING_ENTRY.listen(v -> BOILING = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                BLEEDING_ENTRY.listen(v -> BLEEDING = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                BROKEN_WINGS_ENTRY.listen(v -> BROKEN_WINGS = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                FAIRY_DUST_EFFECT_ENTRY.listen(v -> FAIRY_DUST_EFFECT = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                AIR_ELEMENT_ENTRY.listen(v -> AIR_ELEMENT = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                EARTH_ELEMENT_ENTRY.listen(v -> EARTH_ELEMENT = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                FIRE_ELEMENT_ENTRY.listen(v -> FIRE_ELEMENT = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                WATER_ELEMENT_ENTRY.listen(v -> WATER_ELEMENT = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                BLINDED_ENTRY.listen(v -> BLINDED = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                CAMOUFLAGE_ENTRY.listen(v -> CAMOUFLAGE = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                FOUL_PLAY_ENTRY.listen(v -> FOUL_PLAY = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                INVULNERABILITY_ENTRY.listen(v -> INVULNERABILITY = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                THORNS_ENTRY.listen(v -> THORNS = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                SHIELD_ENTRY.listen(v -> SHIELD = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                AP_SHIELD_ENTRY.listen(v -> AP_SHIELD = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                AD_SHIELD_ENTRY.listen(v -> AD_SHIELD = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                BANISHMENT_ENTRY.listen(v -> BANISHMENT = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                REVEALING_ENTRY.listen(v -> REVEALING = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
                GRIEVOUS_WOUNDS_ENTRY.listen(v -> GRIEVOUS_WOUNDS = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(v));
        }
}
