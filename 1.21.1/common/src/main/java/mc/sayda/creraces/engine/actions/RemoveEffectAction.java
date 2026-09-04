package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class RemoveEffectAction implements ActionRegistry.RaceAction {

    private final List<net.minecraft.core.Holder<MobEffect>> effects;
    private final ScalingValue radius;
    private final mc.sayda.creraces.engine.TargetFilter targets;
    private final boolean useTarget;

    public RemoveEffectAction(List<net.minecraft.core.Holder<MobEffect>> effects, ScalingValue radius,
            mc.sayda.creraces.engine.TargetFilter targets, boolean useTarget) {
        this.effects = effects;
        this.radius = radius;
        this.targets = targets;
        this.useTarget = useTarget;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        if (effects.isEmpty()) return true;

        java.util.function.Consumer<LivingEntity> remove = e -> {
            for (net.minecraft.core.Holder<MobEffect> effect : effects) {
                e.removeEffect(effect);
            }
        };

        double r = radius.evaluate(player, target, slot);
        int maxAoeRadius = mc.sayda.creraces.config.CreRacesConfig.AOE_MAX_RADIUS.get();
        if (maxAoeRadius > 0)
            r = Math.min(r, maxAoeRadius);

        if (r > 0) {
            net.minecraft.world.phys.AABB area = player.getBoundingBox().inflate(r);
            player.level().getEntitiesOfClass(LivingEntity.class, area, e -> targets.isValid(e, player))
                    .forEach(remove);
        } else {
            LivingEntity entity = mc.sayda.creraces.engine.TargetFilter.resolveSmartTarget(player, target, useTarget);
            if (entity != null && targets.isValid(entity, player)) {
                remove.accept(entity);
            }
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "remove_effect"), json -> {
            List<net.minecraft.core.Holder<MobEffect>> effects = new ArrayList<>();

            if (json.has("effects")) {
                for (com.google.gson.JsonElement e : json.getAsJsonArray("effects")) {
                    String id = e.isJsonPrimitive() ? e.getAsString()
                            : e.getAsJsonObject().get("effect").getAsString();
                    var eff = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.tryParse(id));
                    if (eff.isEmpty()) {
                        CreRaces.LOGGER.error("RemoveEffectAction: Unknown effect ID '{}'.", id);
                    } else {
                        effects.add(eff.get());
                    }
                }
            } else if (json.has("effect")) {
                String effectId = GsonHelper.getAsString(json, "effect");
                var effect = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(effectId));
                if (effect.isEmpty()) {
                    CreRaces.LOGGER.error("RemoveEffectAction: Unknown effect ID '{}'.", effectId);
                    return null;
                }
                effects.add(effect.get());
            } else {
                CreRaces.LOGGER.error("RemoveEffectAction: Missing 'effect' or 'effects' field.");
                return null;
            }

            ScalingValue radius = ScalingValue.fromJson(json, "radius", 0.0);
            mc.sayda.creraces.engine.TargetFilter targets = mc.sayda.creraces.engine.TargetFilter.fromJson(json,
                    "targets", java.util.Set.of("enemies", "self"));
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);

            return new RemoveEffectAction(effects, radius, targets, useTarget);
        });
    }
}
