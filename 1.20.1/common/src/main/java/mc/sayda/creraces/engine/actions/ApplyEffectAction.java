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

/**
 * Action that applies a status effect (potion effect) to the player or target.
 */
public class ApplyEffectAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = new ResourceLocation("creraces", "apply_effect");

    private final ResourceLocation effectId;
    private final ScalingValue duration;
    private final ScalingValue amplifier;
    private final boolean ambient;
    private final boolean visible;
    private final boolean useTarget;
    private final ScalingValue radius;
    private final boolean targetPlayers;
    private final boolean targetEnemies;
    private final boolean excludeCaster;
    private final boolean incrementAmplifier;

    public ApplyEffectAction(ResourceLocation effectId, ScalingValue duration,
            ScalingValue amplifier, boolean ambient, boolean visible,
            boolean useTarget, ScalingValue radius, boolean targetPlayers, boolean targetEnemies, boolean excludeCaster,
            boolean incrementAmplifier) {
        this.effectId = effectId;
        this.duration = duration;
        this.amplifier = amplifier;
        this.ambient = ambient;
        this.visible = visible;
        this.useTarget = useTarget;
        this.radius = radius;
        this.targetPlayers = targetPlayers;
        this.targetEnemies = targetEnemies;
        this.excludeCaster = excludeCaster;
        this.incrementAmplifier = incrementAmplifier;
    }

    public static void register() {
        ActionRegistry.register(ID, json -> {
            ResourceLocation effectId = new ResourceLocation(GsonHelper.getAsString(json, "effect"));
            ScalingValue duration = ScalingValue.fromJson(json, "duration", 200.0);
            ScalingValue amplifier = ScalingValue.fromJson(json, "amplifier", 0.0);
            boolean ambient = GsonHelper.getAsBoolean(json, "ambient", false);
            boolean visible = GsonHelper.getAsBoolean(json, "visible", true);
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);

            ScalingValue radius = ScalingValue.fromJson(json, "radius", 0.0);
            boolean targetPlayers = GsonHelper.getAsBoolean(json, "target_players", true);
            boolean targetEnemies = GsonHelper.getAsBoolean(json, "target_enemies", true);
            boolean excludeCaster = GsonHelper.getAsBoolean(json, "exclude_caster", true);
            boolean incrementAmplifier = GsonHelper.getAsBoolean(json, "increment_amplifier", false);

            return new ApplyEffectAction(effectId, duration, amplifier, ambient, visible, useTarget,
                    radius, targetPlayers, targetEnemies, excludeCaster, incrementAmplifier);
        });
    }

    @SuppressWarnings("null")
    @Override
    public boolean execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(effectId);
        if (effect == null)
            return true;

        double r = radius.evaluate(player, target);
        int maxAoeRadius = mc.sayda.creraces.config.CreRacesConfig.AOE_MAX_RADIUS.get();
        if (maxAoeRadius > 0)
            r = Math.min(r, maxAoeRadius);
        if (r > 0) {
            // AoE Mode
            net.minecraft.world.phys.AABB area = player.getBoundingBox().inflate(r);
            player.level().getEntitiesOfClass(LivingEntity.class, area, e -> {
                if (excludeCaster && e == player)
                    return false;
                if (!targetPlayers && e instanceof Player)
                    return false;
                if (!targetEnemies && !(e instanceof Player))
                    return false;
                return mc.sayda.creraces.team.RaceTeamManager.canHurt(e, player);
            }).forEach(e -> applyToEntity(player, e, effect));
        } else {
            // Single Target Mode
            LivingEntity entity = (target != null) ? target : (useTarget ? null : player);
            if (entity != null) {
                applyToEntity(player, entity, effect);
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

        entity.addEffect(new MobEffectInstance(effect, finalDuration, finalAmplifier, ambient, visible));
    }
}
