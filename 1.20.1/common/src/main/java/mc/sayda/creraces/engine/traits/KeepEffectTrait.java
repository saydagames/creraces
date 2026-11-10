package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.actions.ApplyEffectAction;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.engine.condition.ConditionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.util.RaceUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Keeps one "effect" or a list of "effects" on the player while "condition" holds (always, without
 * one), checked every "interval" ticks. Only the effect this trait added is ever removed: a potion
 * of the same effect is left alone, and if it was stronger, this trait's own copy is removed once
 * the potion runs out and it surfaces again.
 */
@SuppressWarnings("null")
public class KeepEffectTrait extends PeriodicTrait {
    private final List<ApplyEffectAction.EffectData> effects;
    private final boolean ambient;
    private final boolean visible;
    private final boolean showIcon;
    @Nullable
    private final Condition condition;

    KeepEffectTrait(JsonObject json) {
        super(TraitIds.of(json), ScalingValue.fromJson(json, "interval", 1.0));
        this.effects = ApplyEffectAction.EffectData.listFromJson(json);
        this.visible = GsonHelper.getAsBoolean(json, "visible", true);
        this.ambient = GsonHelper.getAsBoolean(json, "ambient", true);
        this.showIcon = GsonHelper.getAsBoolean(json, "show_icon", visible);
        this.condition = ConditionRegistry.optional(json, "condition");
    }

    @Override
    protected boolean shouldExecute(Player player, IPlayerVariables vars) {
        return true;
    }

    @Override
    protected void execute(Player player, IPlayerVariables vars) {
        boolean wanted = condition == null || condition.evaluate(player, null, null, null);
        for (ApplyEffectAction.EffectData data : effects) {
            ResourceLocation effectId = BuiltInRegistries.MOB_EFFECT.getKey(data.effect());
            if (effectId == null) {
                continue;
            }
            // Remembers that this trait's copy is on the player, so a later removal never takes someone else's.
            ResourceLocation appliedId = traitId.withSuffix("/" + effectId.getNamespace() + "/" + effectId.getPath());
            int amplifier = (int) Math.round(data.amplifier().evaluate(player));
            MobEffectInstance current = player.getEffect(data.effect());
            boolean ours = current != null && isOurs(current, amplifier);

            if (wanted && !RaceUtils.isImmuneToEffect(player, effectId)) {
                if (current != null && current.isInfiniteDuration() && !ours && vars.getPersistentState(appliedId) > 0) {
                    // Our own copy at an old amplifier: vanilla never lowers one, so swap it out.
                    player.removeEffect(data.effect());
                }
                player.addEffect(new MobEffectInstance(data.effect(), MobEffectInstance.INFINITE_DURATION, amplifier,
                        ambient, visible, showIcon));
                vars.setPersistentState(appliedId, 1.0);
            } else if (vars.getPersistentState(appliedId) > 0) {
                if (current == null) {
                    vars.setPersistentState(appliedId, 0.0);
                } else if (ours) {
                    player.removeEffect(data.effect());
                    vars.setPersistentState(appliedId, 0.0);
                }
                // Otherwise a stronger potion is on top; ours is removed once it runs out.
            }
        }
    }

    private boolean isOurs(MobEffectInstance instance, int amplifier) {
        return instance.isInfiniteDuration() && instance.getAmplifier() == amplifier
                && instance.isAmbient() == ambient && instance.isVisible() == visible;
    }

    public static void register() {
        TraitRegistry.register(CreRaces.id("keep_effect"), KeepEffectTrait::new);
    }
}
