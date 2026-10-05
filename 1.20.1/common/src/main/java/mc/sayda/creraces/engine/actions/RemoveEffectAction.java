package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonElement;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TargetFilter;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Removes status effects from every valid entity in a radius, or from the target or caster. */
public class RemoveEffectAction implements ActionRegistry.RaceAction {

    private final List<MobEffect> effects;
    private final ScalingValue radius;
    private final TargetFilter targets;
    private final boolean useTarget;

    public RemoveEffectAction(List<MobEffect> effects, ScalingValue radius, TargetFilter targets,
            boolean useTarget) {
        this.effects = effects;
        this.radius = radius;
        this.targets = targets;
        this.useTarget = useTarget;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (effects.isEmpty()) {
            return true;
        }
        double r = AreaTargets.clampRadius(radius.evaluate(player, target, slot));
        if (r > 0) {
            AreaTargets.around(player, r, e -> targets.isValid(e, player)).forEach(this::removeFrom);
        } else {
            LivingEntity entity = TargetFilter.resolveSmartTarget(player, target, useTarget);
            if (entity != null && targets.isValid(entity, player)) {
                removeFrom(entity);
            }
        }
        return true;
    }

    private void removeFrom(LivingEntity entity) {
        for (MobEffect effect : effects) {
            entity.removeEffect(effect);
        }
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "remove_effect"), json -> {
            List<MobEffect> effects = new ArrayList<>();
            if (json.has("effects")) {
                // Entries may be plain ids or objects with an "effect" field; unknown ids are skipped.
                for (JsonElement element : json.getAsJsonArray("effects")) {
                    String id = element.isJsonPrimitive() ? element.getAsString()
                            : element.getAsJsonObject().get("effect").getAsString();
                    MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.tryParse(id));
                    if (effect == null) {
                        CreRaces.LOGGER.error("RemoveEffectAction: Unknown effect ID '{}'.", id);
                    } else {
                        effects.add(effect);
                    }
                }
            } else if (json.has("effect")) {
                String id = GsonHelper.getAsString(json, "effect");
                MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(new ResourceLocation(id));
                if (effect == null) {
                    CreRaces.LOGGER.error("RemoveEffectAction: Unknown effect ID '{}'.", id);
                    return null;
                }
                effects.add(effect);
            } else {
                CreRaces.LOGGER.error("RemoveEffectAction: Missing 'effect' or 'effects' field.");
                return null;
            }
            return new RemoveEffectAction(effects,
                    ScalingValue.fromJson(json, "radius", 0.0),
                    TargetFilter.fromJson(json, "targets", Set.of("enemies", "self")),
                    GsonHelper.getAsBoolean(json, "use_target", false));
        });
    }
}
