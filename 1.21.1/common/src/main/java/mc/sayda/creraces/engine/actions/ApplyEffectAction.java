package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.effect.SourceTrackedEffect;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.EntitySelector;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.util.IPersistentDataAccessor;
import mc.sayda.creraces.util.RaceUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Gives an entity one "effect" or a list of "effects" ({effect, duration, amplifier}). A duration
 * of -1 is infinite. "increment_amplifier" stacks on top of an effect already present.
 */
@SuppressWarnings("null")
public class ApplyEffectAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = CreRaces.id("apply_effect");

    /** One effect to apply; shared with the keep_effect trait. */
    public record EffectData(Holder<MobEffect> effect, ScalingValue duration, ScalingValue amplifier) {
        public static EffectData fromJson(JsonObject json) {
            ResourceLocation effectId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "effect"));
            Holder<MobEffect> effect = effectId != null ? BuiltInRegistries.MOB_EFFECT.getHolder(effectId).orElse(null) : null;
            if (effect == null) {
                throw new IllegalArgumentException("unknown effect '" + effectId + "'");
            }
            return new EffectData(effect, ScalingValue.fromJson(json, "duration", 200.0),
                    ScalingValue.fromJson(json, "amplifier", 0.0));
        }

        /** "effects" when present, else the single effect described by the object itself. */
        public static List<EffectData> listFromJson(JsonObject json) {
            List<EffectData> effects = new ArrayList<>();
            if (json.has("effects") && json.get("effects").isJsonArray()) {
                for (JsonElement element : json.getAsJsonArray("effects")) {
                    effects.add(fromJson(element.getAsJsonObject()));
                }
            } else if (json.has("effect")) {
                effects.add(fromJson(json));
            } else {
                throw new IllegalArgumentException("needs an \"effect\" or an \"effects\" list");
            }
            return effects;
        }
    }

    private final List<EffectData> effects;
    private final EntitySelector entity;
    private final boolean ambient;
    private final boolean visible;
    private final boolean showIcon;
    private final boolean incrementAmplifier;

    private ApplyEffectAction(List<EffectData> effects, EntitySelector entity, boolean ambient, boolean visible,
            boolean showIcon, boolean incrementAmplifier) {
        this.effects = effects;
        this.entity = entity;
        this.ambient = ambient;
        this.visible = visible;
        this.showIcon = showIcon;
        this.incrementAmplifier = incrementAmplifier;
    }

    public static void register() {
        ActionRegistry.register(ID, json -> {
            boolean visible = GsonHelper.getAsBoolean(json, "visible", true);
            return new ApplyEffectAction(EffectData.listFromJson(json),
                    EntitySelector.fromJson(json, EntitySelector.AUTO),
                    GsonHelper.getAsBoolean(json, "ambient", true),
                    visible,
                    GsonHelper.getAsBoolean(json, "show_icon", visible),
                    GsonHelper.getAsBoolean(json, "increment_amplifier", false));
        });
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        LivingEntity subject = entity.resolve(player, target);
        if (subject == null) {
            return true;
        }
        for (EffectData data : effects) {
            apply(player, target, subject, data, slot, interactPos);
        }
        return true;
    }

    private void apply(Player player, @Nullable LivingEntity target, LivingEntity subject, EffectData data,
            @Nullable AbilitySlot slot, @Nullable BlockPos interactPos) {
        ResourceLocation effectId = data.effect().unwrapKey().map(ResourceKey::location).orElse(null);
        if (effectId != null && RaceUtils.isImmuneToEffect(subject, effectId)) {
            return;
        }

        int duration = (int) data.duration().evaluate(player, target, slot, interactPos);
        int amplifier = (int) Math.round(data.amplifier().evaluate(player, target, slot, interactPos));
        MobEffectInstance existing = subject.getEffect(data.effect());
        if (incrementAmplifier && existing != null) {
            amplifier += existing.getAmplifier() + 1;
        }

        if (data.effect().value() instanceof SourceTrackedEffect tracked) {
            recordSource(player, subject, tracked);
        }
        subject.addEffect(new MobEffectInstance(data.effect(), duration, amplifier, ambient, visible, showIcon));
    }

    private static void recordSource(Player player, LivingEntity entity, SourceTrackedEffect effect) {
        if (entity instanceof IPersistentDataAccessor accessor) {
            accessor.creraces$getPersistentData().putString(effect.sourceKey(), player.getUUID().toString());
        }
        // A charmed player's client steers them, so it needs the source straight away.
        if (entity instanceof ServerPlayer target) {
            BoundaryHandler.resyncVariables(target, target);
        }
    }
}
