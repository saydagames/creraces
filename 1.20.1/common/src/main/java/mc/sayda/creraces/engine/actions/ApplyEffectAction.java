package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.Objects;

/**
 * Action that applies a status effect (potion effect) to the player or target.
 */
public class ApplyEffectAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = new ResourceLocation("creraces", "apply_effect");

    private final net.minecraft.world.effect.MobEffect cachedEffect;
    private final ScalingValue duration;
    private final ScalingValue amplifier;
    private final boolean ambient;
    private final boolean visible;
    private final ScalingValue radius;
    private final mc.sayda.creraces.engine.TargetFilter targets;
    private final boolean incrementAmplifier;

    public ApplyEffectAction(ResourceLocation effectId, ScalingValue duration,
            ScalingValue amplifier, boolean ambient, boolean visible,
            ScalingValue radius, mc.sayda.creraces.engine.TargetFilter targets,
            boolean incrementAmplifier) {
        this.cachedEffect = Objects.requireNonNull(BuiltInRegistries.MOB_EFFECT.get(Objects.requireNonNull(effectId)));
        this.duration = duration;
        this.amplifier = amplifier;
        this.ambient = ambient;
        this.visible = visible;
        this.radius = radius;
        this.targets = targets;
        this.incrementAmplifier = incrementAmplifier;
    }

    public static void register() {
        ActionRegistry.register(ID, json -> {
            ResourceLocation effectId = new ResourceLocation(
                    Objects.requireNonNull(GsonHelper.getAsString(json, "effect")));
            if (!BuiltInRegistries.MOB_EFFECT.containsKey(effectId)) {
                mc.sayda.creraces.CreRaces.LOGGER.error("Unknown mob effect ID in apply_effect action: {}", effectId);
            }
            ScalingValue duration = ScalingValue.fromJson(json, "duration", 200.0);
            ScalingValue amplifier = ScalingValue.fromJson(json, "amplifier", 0.0);
            boolean ambient = GsonHelper.getAsBoolean(json, "ambient", true);
            boolean visible = GsonHelper.getAsBoolean(json, "visible", true);
            ScalingValue radius = ScalingValue.fromJson(json, "radius", 0.0);
            java.util.Set<String> defaultAllow = radius.isZero() ? java.util.Set.of("enemies", "self")
                    : java.util.Set.of("enemies");
            mc.sayda.creraces.engine.TargetFilter targets = mc.sayda.creraces.engine.TargetFilter.fromJson(json,
                    "targets", defaultAllow);
            boolean incrementAmplifier = GsonHelper.getAsBoolean(json, "increment_amplifier", false);

            return new ApplyEffectAction(effectId, duration, amplifier, ambient, visible,
                    radius, targets, incrementAmplifier);
        });
    }

    @SuppressWarnings("null")
    @Override
    public boolean execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        if (cachedEffect == null)
            return true;

        double r = radius.evaluate(player, target);
        int maxAoeRadius = 100;
        if (maxAoeRadius > 0)
            r = Math.min(r, maxAoeRadius);
        if (r > 0) {
            // AoE Mode
            net.minecraft.world.phys.AABB area = player.getBoundingBox().inflate(r);
            player.level().getEntitiesOfClass(LivingEntity.class, area, e -> targets.isValid(e, player))
                    .forEach(e -> applyToEntity(player, e, cachedEffect));
        } else {
            // Single Target Mode logic: Check both player and target if they exist
            if (target != null) {
                if (targets.isValid(target, player)) {
                    applyToEntity(player, target, cachedEffect);
                }
            } else {
                if (targets.isValid(player, player)) {
                    applyToEntity(player, player, cachedEffect);
                }
            }
        }
        return true;
    }

    private void applyToEntity(Player player, LivingEntity entity, MobEffect effect) {
        int finalDuration = (int) duration.evaluate(player, entity);
        int finalAmplifier = (int) amplifier.evaluate(player, entity);

        if (incrementAmplifier && entity.hasEffect(effect)) {
            finalAmplifier += entity.getEffect(effect).getAmplifier() + 1;
        }

        // Attribution: Link player to target for effects that require a source (e.g. Rat Venom)
        if (player != null && entity instanceof mc.sayda.creraces.util.IPersistentDataAccessor accessor) {
            ResourceLocation effectId = BuiltInRegistries.MOB_EFFECT.getKey(effect);
            if (effectId != null && effectId.toString().equals("creraces:rat_venom")) {
                accessor.creraces$getPersistentData().putString("creraces:venom_source", player.getUUID().toString());
            }
        }

        // Vanilla MobEffectInstance treats -1 as infinite duration
        entity.addEffect(new MobEffectInstance(effect, finalDuration == -1 ? -1 : finalDuration, finalAmplifier,
                ambient, visible));
    }
}
