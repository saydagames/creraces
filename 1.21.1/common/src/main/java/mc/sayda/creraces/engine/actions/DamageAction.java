package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TargetFilter;
import mc.sayda.creraces.team.RaceTeamManager;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Damages the target (or the caster when there is none), with optional fire, knockback, lifesteal
 * and bonus damage per stack of an effect on the victim.
 */
@SuppressWarnings("null")
public class DamageAction implements ActionRegistry.RaceAction {

    private final ScalingValue amount;
    private final String damageTypeId;
    @Nullable
    private final ScalingValue knockback;
    private final String sourceEntity;
    private final ScalingValue fireDuration;
    @Nullable
    private final ScalingValue healAmount;
    @Nullable
    private final ScalingValue damagePerStack;
    @Nullable
    private final Holder<MobEffect> stackEffect;
    private final TargetFilter targets;
    private final boolean disableKnockback;

    public DamageAction(ScalingValue amount, String damageTypeId, @Nullable ScalingValue knockback, String sourceEntity,
            ScalingValue fireDuration, @Nullable ScalingValue healAmount, @Nullable ScalingValue damagePerStack,
            String stackEffect, TargetFilter targets, boolean disableKnockback) {
        this.amount = amount;
        this.damageTypeId = damageTypeId;
        this.knockback = knockback;
        this.sourceEntity = sourceEntity;
        this.fireDuration = fireDuration;
        this.healAmount = healAmount;
        this.damagePerStack = damagePerStack;
        this.stackEffect = stackEffect.isEmpty() ? null
                : BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(stackEffect)).orElse(null);
        this.targets = targets;
        this.disableKnockback = disableKnockback;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        targets.applyToSingleTarget(player, target, (p, victim) -> applyDamage(p, victim, slot));
        return true;
    }

    private void applyDamage(Player player, LivingEntity victim, @Nullable AbilitySlot slot) {
        // Allies are never hurt while friendly fire is off, whatever the target filter allows.
        if (victim != player && !RaceTeamManager.canHurt(victim, player)) {
            return;
        }

        double damage = amount.evaluate(player, victim, slot);
        if (damagePerStack != null && stackEffect != null) {
            MobEffectInstance stacks = victim.getEffect(stackEffect);
            if (stacks != null) {
                damage += (stacks.getAmplifier() + 1) * damagePerStack.evaluate(player, victim, slot);
            }
        }
        double fireSeconds = fireDuration.evaluate(player, victim, slot);
        if (damage <= 0 && fireSeconds <= 0) {
            return;
        }

        DamageSource source = createDamageSource(player, victim);
        if (fireSeconds > 0) {
            victim.igniteForSeconds((float) fireSeconds);
        }
        if (damage > 0) {
            if (disableKnockback) {
                Vec3 motion = victim.getDeltaMovement();
                victim.hurt(source, (float) damage);
                victim.setDeltaMovement(motion);
                // Players only pick up the restored motion when it is force-synced.
                victim.hurtMarked = true;
            } else {
                victim.hurt(source, (float) damage);
            }
        }

        if (knockback != null) {
            float strength = (float) knockback.evaluate(player, victim, slot);
            if (strength > 0) {
                victim.knockback(strength, player.getX() - victim.getX(), player.getZ() - victim.getZ());
            }
        }
        if (healAmount != null) {
            float heal = (float) healAmount.evaluate(player, victim, slot);
            if (heal > 0) {
                player.heal(heal);
            }
        }
    }

    private DamageSource createDamageSource(Player player, LivingEntity victim) {
        if (damageTypeId.isEmpty()) {
            return player.damageSources().playerAttack(player);
        }
        Entity attacker = "self".equalsIgnoreCase(sourceEntity) ? player
                : "target".equalsIgnoreCase(sourceEntity) ? victim : null;
        Registry<DamageType> registry = player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        // An id without a namespace resolves to a vanilla damage type.
        ResourceLocation typeId = ResourceLocation.parse(damageTypeId);
        Holder<DamageType> type = registry.getHolder(ResourceKey.create(Registries.DAMAGE_TYPE, typeId))
                .orElseGet(() -> {
                    CreRaces.LOGGER.warn("DamageAction: unknown damage type '{}', falling back to player_attack",
                            typeId);
                    return registry.getHolderOrThrow(DamageTypes.PLAYER_ATTACK);
                });
        return new DamageSource(type, attacker, attacker);
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "damage"), json -> {
            String stackEffect = GsonHelper.getAsString(json, "stack_effect", "");
            if (!stackEffect.isEmpty() && !BuiltInRegistries.MOB_EFFECT.containsKey(ResourceLocation.parse(stackEffect))) {
                CreRaces.LOGGER.error("DamageAction: Unknown mob effect ID '{}' in stack_effect field.", stackEffect);
            }
            return new DamageAction(
                    ScalingValue.fromJson(json, "amount", 1.0),
                    GsonHelper.getAsString(json, "damage_type", "minecraft:player_attack"),
                    json.has("knockback") ? ScalingValue.fromJson(json, "knockback", 0.0) : null,
                    GsonHelper.getAsString(json, "source", "self"),
                    ScalingValue.fromJson(json, "fire_duration", 0.0),
                    json.has("heal_amount") ? ScalingValue.fromJson(json, "heal_amount", 0.0) : null,
                    json.has("damage_per_stack") ? ScalingValue.fromJson(json, "damage_per_stack", 0.0) : null,
                    stackEffect,
                    TargetFilter.fromJson(json, "targets", Set.of("enemies")),
                    GsonHelper.getAsBoolean(json, "disable_knockback", false));
        });
    }
}
