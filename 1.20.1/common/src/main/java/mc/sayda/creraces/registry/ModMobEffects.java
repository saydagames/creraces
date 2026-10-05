package mc.sayda.creraces.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.effect.BanishmentEffect;
import mc.sayda.creraces.effect.BleedingEffect;
import mc.sayda.creraces.effect.BlindedEffect;
import mc.sayda.creraces.effect.BoilingEffect;
import mc.sayda.creraces.effect.BrokenWingsEffect;
import mc.sayda.creraces.effect.CamouflageEffect;
import mc.sayda.creraces.effect.DizzinessEffect;
import mc.sayda.creraces.effect.FairyDustEffect;
import mc.sayda.creraces.effect.FeatherstormEffect;
import mc.sayda.creraces.effect.FrozenEffect;
import mc.sayda.creraces.effect.InvulnerabilityEffect;
import mc.sayda.creraces.effect.LifeDrainEffect;
import mc.sayda.creraces.effect.RatVenomEffect;
import mc.sayda.creraces.effect.RevealingEffect;
import mc.sayda.creraces.effect.ShieldEffect;
import mc.sayda.creraces.effect.SimpleEffect;
import mc.sayda.creraces.effect.SoggyEffect;
import mc.sayda.creraces.effect.TalonStrikeEffect;
import mc.sayda.creraces.effect.ThornsEffect;
import mc.sayda.creraces.effect.TrollCurseEffect;
import mc.sayda.creraces.effect.TrueInvisibilityEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class ModMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(CreRaces.MODID,
            Registries.MOB_EFFECT);

    public static final RegistrySupplier<MobEffect> TALON_STRIKE = MOB_EFFECTS.register("talon_strike",
            () -> new TalonStrikeEffect(MobEffectCategory.HARMFUL, 0x8B0000));
    public static final RegistrySupplier<MobEffect> LIFE_DRAIN = MOB_EFFECTS.register("life_drain",
            () -> new LifeDrainEffect(MobEffectCategory.HARMFUL, 0x4B0082));
    public static final RegistrySupplier<MobEffect> RAT_VENOM = MOB_EFFECTS.register("rat_venom",
            RatVenomEffect::new);
    public static final RegistrySupplier<MobEffect> TRUE_INVISIBILITY = MOB_EFFECTS.register("true_invisibility",
            () -> new TrueInvisibilityEffect(MobEffectCategory.BENEFICIAL, 0xFFFFFF));
    public static final RegistrySupplier<MobEffect> SOGGY = MOB_EFFECTS.register("soggy",
            SoggyEffect::new);

    public static final RegistrySupplier<MobEffect> NYMPH_CALL = MOB_EFFECTS.register("nymph_call",
            () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0x00FF00));

    public static final RegistrySupplier<MobEffect> ROOTED = MOB_EFFECTS.register("rooted",
            () -> new SimpleEffect(MobEffectCategory.HARMFUL, 0x654321)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, "68ac4f36-0016-4680-b3be-c6a4c37a0265", -1.0D,
                            AttributeModifier.Operation.MULTIPLY_TOTAL));

    /** PlayerMixin blocks attacking and interacting while this is active. */
    public static final RegistrySupplier<MobEffect> DISARMED = MOB_EFFECTS.register("disarmed",
            () -> new SimpleEffect(MobEffectCategory.HARMFUL, 0x808080));

    /** Roots like ROOTED and also blocks attacking and interacting (PlayerMixin). */
    public static final RegistrySupplier<MobEffect> STUNNED = MOB_EFFECTS.register("stunned",
            () -> new SimpleEffect(MobEffectCategory.HARMFUL, 0xFFFF00)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, "4cb5918e-e21c-480d-a10d-b1f3712f1e06", -1.0D,
                            AttributeModifier.Operation.MULTIPLY_TOTAL));

    public static final RegistrySupplier<MobEffect> FROZEN = MOB_EFFECTS.register("frozen",
            () -> new FrozenEffect(MobEffectCategory.HARMFUL, 0xADD8E6));

    public static final RegistrySupplier<MobEffect> TROLL_CURSE = MOB_EFFECTS.register("troll_curse",
            TrollCurseEffect::new);

    public static final RegistrySupplier<MobEffect> DIZZINESS = MOB_EFFECTS.register("dizziness",
            () -> new DizzinessEffect(MobEffectCategory.HARMFUL, 0x87CEEB));

    public static final RegistrySupplier<MobEffect> FEATHERSTORM = MOB_EFFECTS.register("featherstorm",
            FeatherstormEffect::new);

    /** Marker the Troll leaves on a target; its next charged hit consumes it for bonus damage. */
    public static final RegistrySupplier<MobEffect> MAUL = MOB_EFFECTS.register("maul",
            () -> new SimpleEffect(MobEffectCategory.HARMFUL, 0x8B4513));

    public static final RegistrySupplier<MobEffect> BOILING = MOB_EFFECTS.register("boiling",
            BoilingEffect::new);

    public static final RegistrySupplier<MobEffect> BLEEDING = MOB_EFFECTS.register("bleeding",
            BleedingEffect::new);

    public static final RegistrySupplier<MobEffect> BROKEN_WINGS = MOB_EFFECTS.register("broken_wings",
            BrokenWingsEffect::new);

    public static final RegistrySupplier<MobEffect> FAIRY_DUST_EFFECT = MOB_EFFECTS.register("fairy_dust",
            FairyDustEffect::new);

    // Element markers, not currently referenced by any code or data.
    public static final RegistrySupplier<MobEffect> AIR_ELEMENT = MOB_EFFECTS.register("air_element",
            () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0x87CEEB));
    public static final RegistrySupplier<MobEffect> EARTH_ELEMENT = MOB_EFFECTS.register("earth_element",
            () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0x8B4513));
    public static final RegistrySupplier<MobEffect> FIRE_ELEMENT = MOB_EFFECTS.register("fire_element",
            () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0xFF4500));
    public static final RegistrySupplier<MobEffect> WATER_ELEMENT = MOB_EFFECTS.register("water_element",
            () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0x0000FF));

    public static final RegistrySupplier<MobEffect> BLINDED = MOB_EFFECTS.register("blinded",
            BlindedEffect::new);
    public static final RegistrySupplier<MobEffect> CAMOUFLAGE = MOB_EFFECTS.register("camouflage",
            CamouflageEffect::new);
    public static final RegistrySupplier<MobEffect> FOUL_PLAY = MOB_EFFECTS.register("foul_play",
            () -> new SimpleEffect(MobEffectCategory.HARMFUL, 0x4B0082));
    public static final RegistrySupplier<MobEffect> INVULNERABILITY = MOB_EFFECTS.register("invulnerability",
            InvulnerabilityEffect::new);
    public static final RegistrySupplier<MobEffect> THORNS = MOB_EFFECTS.register("thorns",
            ThornsEffect::new);
    public static final RegistrySupplier<MobEffect> SHIELD = MOB_EFFECTS.register("shield",
            () -> new ShieldEffect(MobEffectCategory.BENEFICIAL, 0xFFFF00));
    public static final RegistrySupplier<MobEffect> AP_SHIELD = MOB_EFFECTS.register("ap_shield",
            () -> new ShieldEffect(MobEffectCategory.BENEFICIAL, 0x00FFFF));
    public static final RegistrySupplier<MobEffect> AD_SHIELD = MOB_EFFECTS.register("ad_shield",
            () -> new ShieldEffect(MobEffectCategory.BENEFICIAL, 0xFF00FF));

    public static final RegistrySupplier<MobEffect> BANISHMENT = MOB_EFFECTS.register("banishment",
            BanishmentEffect::new);
    public static final RegistrySupplier<MobEffect> REVEALING = MOB_EFFECTS.register("revealing",
            RevealingEffect::new);

    /**
     * Immunity to sunlight burning without a helmet. Read by ResourceTicker for undead players and by
     * MobMixin for mobs that burn in daylight.
     */
    public static final RegistrySupplier<MobEffect> SUN_RESISTANCE = MOB_EFFECTS.register("sun_resistance",
            () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0xFFD700));

    /** -0.4 on HEALING_RECEIVED, so healing drops to 60%. */
    public static final RegistrySupplier<MobEffect> GRIEVOUS_WOUNDS = MOB_EFFECTS.register("grievous_wounds",
            () -> new SimpleEffect(MobEffectCategory.HARMFUL, 0x8B0000)
                    .addAttributeModifier(ModAttributes.HEALING_RECEIVED.get(),
                            "a0f3d95c-9c9e-4e8a-b1f3-d9d7a2d5d361", -0.4D, AttributeModifier.Operation.ADDITION));

    @SuppressWarnings("null")
    public static boolean isInvisible(LivingEntity entity) {
        return entity.hasEffect(TRUE_INVISIBILITY.get()) || entity.hasEffect(CAMOUFLAGE.get());
    }

    public static void register() {
        MOB_EFFECTS.register();
    }
}
