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

    private static class EffectData {
        private final MobEffect effect;
        private final ScalingValue duration;
        private final ScalingValue amplifier;

        public EffectData(ResourceLocation effectId, ScalingValue duration, ScalingValue amplifier) {
            this.effect = Objects.requireNonNull(BuiltInRegistries.MOB_EFFECT.get(Objects.requireNonNull(effectId)));
            this.duration = duration;
            this.amplifier = amplifier;
        }
    }

    private final java.util.List<EffectData> effects;
    private final boolean ambient;
    private final boolean visible;
    private final ScalingValue radius;
    private final mc.sayda.creraces.engine.TargetFilter targets;
    private final boolean incrementAmplifier;

    public ApplyEffectAction(java.util.List<EffectData> effects, boolean ambient, boolean visible,
            ScalingValue radius, mc.sayda.creraces.engine.TargetFilter targets,
            boolean incrementAmplifier) {
        this.effects = effects;
        this.ambient = ambient;
        this.visible = visible;
        this.radius = radius;
        this.targets = targets;
        this.incrementAmplifier = incrementAmplifier;
    }

    public static void register() {
        ActionRegistry.register(ID, json -> {
            java.util.List<EffectData> effects = new java.util.ArrayList<>();

            if (json.has("effects") && json.get("effects").isJsonArray()) {
                com.google.gson.JsonArray array = json.getAsJsonArray("effects");
                for (int i = 0; i < array.size(); i++) {
                    com.google.gson.JsonObject obj = array.get(i).getAsJsonObject();
                    ResourceLocation effectId = new ResourceLocation(GsonHelper.getAsString(obj, "effect"));
                    ScalingValue duration = ScalingValue.fromJson(obj, "duration", 200.0);
                    ScalingValue amplifier = ScalingValue.fromJson(obj, "amplifier", 0.0);
                    effects.add(new EffectData(effectId, duration, amplifier));
                }
            } else if (json.has("effect")) {
                ResourceLocation effectId = new ResourceLocation(
                        Objects.requireNonNull(GsonHelper.getAsString(json, "effect")));
                ScalingValue duration = ScalingValue.fromJson(json, "duration", 200.0);
                ScalingValue amplifier = ScalingValue.fromJson(json, "amplifier", 0.0);
                effects.add(new EffectData(effectId, duration, amplifier));
            } else {
                throw new IllegalArgumentException("Missing 'effect' or 'effects' array in apply_effect action");
            }

            boolean ambient = GsonHelper.getAsBoolean(json, "ambient", true);
            boolean visible = GsonHelper.getAsBoolean(json, "visible", true);
            ScalingValue radius = ScalingValue.fromJson(json, "radius", 0.0);
            java.util.Set<String> defaultAllow = radius.isZero() ? java.util.Set.of("enemies", "self")
                    : java.util.Set.of("enemies");
            mc.sayda.creraces.engine.TargetFilter targets = mc.sayda.creraces.engine.TargetFilter.fromJson(json,
                    "targets", defaultAllow);
            boolean incrementAmplifier = GsonHelper.getAsBoolean(json, "increment_amplifier", false);

            return new ApplyEffectAction(effects, ambient, visible,
                    radius, targets, incrementAmplifier);
        });
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        if (effects.isEmpty())
            return true;

        double r = radius.evaluate(player, target, slot);
        int maxAoeRadius = mc.sayda.creraces.config.CreRacesConfig.AOE_MAX_RADIUS.get();
        if (maxAoeRadius > 0)
            r = Math.min(r, maxAoeRadius);
        if (r > 0) {
            // AoE Mode
            net.minecraft.world.phys.AABB area = java.util.Objects.requireNonNull(player.getBoundingBox().inflate(r));
            player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != null && targets.isValid(e, player))
                    .forEach(e -> applyAllToEntity(player, e, slot));
        } else {
            // Single Target Mode logic: Check both player and target if they exist
            if (target != null) {
                if (targets.isValid(target, player)) {
                    applyAllToEntity(player, target, slot);
                }
            } else {
                if (targets.isValid(player, player)) {
                    applyAllToEntity(player, player, slot);
                }
            }
        }
        return true;
    }

    private void applyAllToEntity(Player player, LivingEntity entity, @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
        for (EffectData data : effects) {
            applyToEntity(player, entity, data, slot);
        }
    }

    private void applyToEntity(Player player, LivingEntity entity, EffectData data, @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
        ResourceLocation effectId = BuiltInRegistries.MOB_EFFECT.getKey(java.util.Objects.requireNonNull(data.effect));
        if (effectId != null && mc.sayda.creraces.util.RaceUtils.isImmuneToEffect(entity, effectId)) {
            return;
        }

        int finalDuration = (int) data.duration.evaluate(player, entity, slot);
        int finalAmplifier = (int) Math.round(data.amplifier.evaluate(player, entity, slot));

        MobEffectInstance existing = entity.getEffect(java.util.Objects.requireNonNull(data.effect));
        if (incrementAmplifier && existing != null) {
            finalAmplifier += existing.getAmplifier() + 1;
        }

        // Attribution: Link player to target for effects that require a source scaling (e.g. Rat Venom, Boiling, Bleeding)
        if (player != null && entity instanceof mc.sayda.creraces.util.IPersistentDataAccessor accessor) {
            String effectPath = effectId != null ? effectId.toString() : "";
            if (effectPath.equals("creraces:rat_venom") || effectPath.equals("creraces:boiling") || effectPath.equals("creraces:bleeding")) {
                var dataTag = accessor.creraces$getPersistentData();
                if (dataTag != null) {
                    dataTag.putString("creraces:source", player.getUUID().toString());
                }
            }
        }

        // Vanilla MobEffectInstance treats -1 as infinite duration
        entity.addEffect(new MobEffectInstance(java.util.Objects.requireNonNull(data.effect), finalDuration == -1 ? -1 : finalDuration, finalAmplifier,
                ambient, visible));
    }
}
