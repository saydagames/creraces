package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.effect.SourceTrackedEffect;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TargetFilter;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import mc.sayda.creraces.util.RaceUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Applies one or more status effects to the caster, the target, or every valid entity in a radius. */
public class ApplyEffectAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "apply_effect");

    private record EffectData(Holder<MobEffect> effect, ScalingValue duration, ScalingValue amplifier) {
        static EffectData fromJson(JsonObject json) {
            ResourceLocation effectId = ResourceLocation.parse(GsonHelper.getAsString(json, "effect"));
            Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.getHolder(effectId)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown effect ID: " + effectId));
            return new EffectData(effect, ScalingValue.fromJson(json, "duration", 200.0),
                    ScalingValue.fromJson(json, "amplifier", 0.0));
        }
    }

    private final List<EffectData> effects;
    private final boolean ambient;
    private final boolean visible;
    private final boolean showIcon;
    private final ScalingValue radius;
    private final TargetFilter targets;
    private final boolean incrementAmplifier;

    private ApplyEffectAction(List<EffectData> effects, boolean ambient, boolean visible, boolean showIcon,
            ScalingValue radius, TargetFilter targets, boolean incrementAmplifier) {
        this.effects = effects;
        this.ambient = ambient;
        this.visible = visible;
        this.showIcon = showIcon;
        this.radius = radius;
        this.targets = targets;
        this.incrementAmplifier = incrementAmplifier;
    }

    public static void register() {
        ActionRegistry.register(ID, json -> {
            List<EffectData> effects = new ArrayList<>();
            if (json.has("effects") && json.get("effects").isJsonArray()) {
                for (JsonElement element : json.getAsJsonArray("effects")) {
                    effects.add(EffectData.fromJson(element.getAsJsonObject()));
                }
            } else if (json.has("effect")) {
                effects.add(EffectData.fromJson(json));
            } else {
                throw new IllegalArgumentException("Missing 'effect' or 'effects' array in apply_effect action");
            }

            boolean visible = GsonHelper.getAsBoolean(json, "visible", true);
            ScalingValue radius = ScalingValue.fromJson(json, "radius", 0.0);
            // A single-target cast may land on the caster by default; an area cast only hits enemies.
            Set<String> defaultTargets = radius.isZero() ? Set.of("enemies", "self") : Set.of("enemies");
            return new ApplyEffectAction(effects,
                    GsonHelper.getAsBoolean(json, "ambient", true),
                    visible,
                    GsonHelper.getAsBoolean(json, "show_icon", visible),
                    radius,
                    TargetFilter.fromJson(json, "targets", defaultTargets),
                    GsonHelper.getAsBoolean(json, "increment_amplifier", false));
        });
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (effects.isEmpty()) {
            return true;
        }

        double r = AreaTargets.clampRadius(radius.evaluate(player, target, slot, interactPos));
        if (r > 0) {
            AreaTargets.around(player, r, e -> targets.isValid(e, player)).forEach(e -> applyAll(player, e, slot));
        } else {
            targets.applyToSingleTarget(player, target, (p, e) -> applyAll(p, e, slot));
        }
        return true;
    }

    private void applyAll(Player player, LivingEntity entity, @Nullable AbilitySlot slot) {
        for (EffectData data : effects) {
            apply(player, entity, data, slot);
        }
    }

    private void apply(Player player, LivingEntity entity, EffectData data, @Nullable AbilitySlot slot) {
        ResourceLocation effectId = data.effect().unwrapKey().map(ResourceKey::location).orElse(null);
        if (effectId != null && RaceUtils.isImmuneToEffect(entity, effectId)) {
            return;
        }

        // A duration of -1 goes straight through: vanilla treats it as infinite.
        int duration = (int) data.duration().evaluate(player, entity, slot);
        int amplifier = (int) Math.round(data.amplifier().evaluate(player, entity, slot));
        MobEffectInstance existing = entity.getEffect(data.effect());
        if (incrementAmplifier && existing != null) {
            amplifier += existing.getAmplifier() + 1;
        }

        if (data.effect().value() instanceof SourceTrackedEffect) {
            recordSource(player, entity);
        }
        entity.addEffect(new MobEffectInstance(data.effect(), duration, amplifier, ambient, visible, showIcon));
    }

    private static void recordSource(Player player, LivingEntity entity) {
        if (entity instanceof IPersistentDataAccessor accessor) {
            CompoundTag data = accessor.creraces$getPersistentData();
            if (data != null) {
                data.putString(SourceTrackedEffect.SOURCE_KEY, player.getUUID().toString());
            }
        }
    }
}
