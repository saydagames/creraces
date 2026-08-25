package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class DamageAction implements ActionRegistry.RaceAction {

    private final ScalingValue amount;
    private final String damageTypeId;
    private final ScalingValue knockback;
    private final String sourceEntity;
    private final ScalingValue fireDuration;
    private final ScalingValue healAmount;
    private final ScalingValue damagePerStack;
    private final String stackEffect;
    private final mc.sayda.creraces.engine.TargetFilter targets;
    private final boolean disableKnockback;

    public DamageAction(ScalingValue amount, String damageTypeId, ScalingValue knockback, String sourceEntity,
            ScalingValue fireDuration, ScalingValue healAmount, ScalingValue damagePerStack, String stackEffect, mc.sayda.creraces.engine.TargetFilter targets, boolean disableKnockback) {
        this.amount = amount;
        this.damageTypeId = damageTypeId;
        this.knockback = knockback;
        this.sourceEntity = sourceEntity;
        this.fireDuration = fireDuration;
        this.healAmount = healAmount;
        this.damagePerStack = damagePerStack;
        this.stackEffect = stackEffect;
        this.targets = targets;
        this.disableKnockback = disableKnockback;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        
        targets.applyToSingleTarget(player, target, (p, e) -> applyDamage(p, e, slot));
        return true;
    }

    private void applyDamage(Player player, net.minecraft.world.entity.LivingEntity actualTarget, 
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
        // Safety Guard: Don't damage allies if FF is off
        if (actualTarget != player && !mc.sayda.creraces.team.RaceTeamManager.canHurt(actualTarget, player)) {
            return;
        }

        double dmg = amount.evaluate(player, actualTarget, slot);

        if (damagePerStack != null && stackEffect != null && !stackEffect.isEmpty()) {
            net.minecraft.resources.ResourceLocation effectId = new net.minecraft.resources.ResourceLocation(
                    stackEffect);
            net.minecraft.world.effect.MobEffect effect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                    .get(effectId);
            if (effect != null && actualTarget.hasEffect(effect)) {
                net.minecraft.world.effect.MobEffectInstance inst = actualTarget.getEffect(effect);
                if (inst != null) {
                    int amplifier = inst.getAmplifier();
                    dmg += (amplifier + 1) * damagePerStack.evaluate(player, actualTarget, slot);
                }
            }
        }

        double fireSecs = fireDuration.evaluate(player, actualTarget, slot);

        if (dmg > 0 || fireSecs > 0) {
            net.minecraft.world.damagesource.DamageSource source;
            net.minecraft.world.entity.Entity damageSourceEntity = "self".equalsIgnoreCase(sourceEntity) ? player
                    : ("target".equalsIgnoreCase(sourceEntity) ? actualTarget : null);

            if (damageTypeId != null && !damageTypeId.isEmpty()) {
                if (damageTypeId.contains(":")) {
                    var registry = player.level().registryAccess()
                            .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE);
                    var holder = registry.getHolder(net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                            new net.minecraft.resources.ResourceLocation(damageTypeId)));

                    source = new net.minecraft.world.damagesource.DamageSource(
                            holder.orElseGet(() -> registry
                                    .getHolderOrThrow(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)),
                            damageSourceEntity, damageSourceEntity);
                } else {
                    // No namespace: treat as minecraft: shorthand
                    var registry = player.level().registryAccess()
                            .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE);
                    var holder = registry.getHolder(net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                            new net.minecraft.resources.ResourceLocation("minecraft", damageTypeId)));
                    source = new net.minecraft.world.damagesource.DamageSource(
                            holder.orElseGet(() -> {
                                mc.sayda.creraces.CreRaces.LOGGER.warn("DamageAction: unknown damage type 'minecraft:{}', falling back to player_attack", damageTypeId);
                                return registry.getHolderOrThrow(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK);
                            }),
                            damageSourceEntity, damageSourceEntity);
                }
            } else {
                source = player.damageSources().playerAttack(player);
            }

            if (fireSecs > 0) {
                actualTarget.setSecondsOnFire((int) fireSecs);
            }

            if (dmg > 0) {
                if (disableKnockback) {
                    net.minecraft.world.phys.Vec3 motion = actualTarget.getDeltaMovement();
                    actualTarget.hurt(source, (float) dmg);
                    actualTarget.setDeltaMovement(motion);
                    actualTarget.hurtMarked = true; // Force sync for players
                } else {
                    actualTarget.hurt(source, (float) dmg);
                }
            }

            if (knockback != null) {
                float kb = (float) knockback.evaluate(player, actualTarget, slot);
                if (kb > 0) {
                    actualTarget.knockback(kb, player.getX() - actualTarget.getX(),
                            player.getZ() - actualTarget.getZ());
                }
            }

            if (healAmount != null) {
                float heal = (float) healAmount.evaluate(player, actualTarget, slot);
                if (heal > 0) {
                    player.heal(heal);
                }
            }
        }
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "damage"), json -> {
            ScalingValue amount = ScalingValue.fromJson(json, "amount", 1.0);
            String damageType = net.minecraft.util.GsonHelper.getAsString(json, "damage_type",
                    "minecraft:player_attack");
            ScalingValue knockback = json.has("knockback") ? ScalingValue.fromJson(json, "knockback", 0.0) : null;
            String source = net.minecraft.util.GsonHelper.getAsString(json, "source", "self");
            ScalingValue fire = ScalingValue.fromJson(json, "fire_duration", 0.0);
            ScalingValue heal = json.has("heal_amount") ? ScalingValue.fromJson(json, "heal_amount", 0.0) : null;
            ScalingValue dmgPerStack = json.has("damage_per_stack")
                    ? ScalingValue.fromJson(json, "damage_per_stack", 0.0)
                    : null;
            String effect = net.minecraft.util.GsonHelper.getAsString(json, "stack_effect", "");
            if (!effect.isEmpty() && !net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                    .containsKey(new ResourceLocation(effect))) {
                CreRaces.LOGGER.error("DamageAction: Unknown mob effect ID '{}' in stack_effect field.", effect);
            }
            mc.sayda.creraces.engine.TargetFilter targets = mc.sayda.creraces.engine.TargetFilter.fromJson(json,
                    "targets", java.util.Set.of("enemies"));
            boolean disableKnockback = net.minecraft.util.GsonHelper.getAsBoolean(json, "disable_knockback", false);

            return new DamageAction(amount, damageType, knockback, source, fire, heal, dmgPerStack, effect, targets, disableKnockback);
        });
    }
}
