package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
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
    private final int fireDuration;
    private final ScalingValue healAmount;
    private final ScalingValue damagePerStack;
    private final String stackEffect;

    public DamageAction(ScalingValue amount, String damageTypeId, ScalingValue knockback, String sourceEntity,
            int fireDuration, ScalingValue healAmount, ScalingValue damagePerStack, String stackEffect) {
        this.amount = amount;
        this.damageTypeId = damageTypeId;
        this.knockback = knockback;
        this.sourceEntity = sourceEntity;
        this.fireDuration = fireDuration;
        this.healAmount = healAmount;
        this.damagePerStack = damagePerStack;
        this.stackEffect = stackEffect;
    }

    @Override
    public void execute(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
        net.minecraft.world.entity.LivingEntity actualTarget = target != null ? target : player;
        double dmg = amount.evaluate(player, actualTarget);

        if (damagePerStack != null && stackEffect != null && !stackEffect.isEmpty()) {
            net.minecraft.resources.ResourceLocation effectId = new net.minecraft.resources.ResourceLocation(
                    stackEffect);
            net.minecraft.world.effect.MobEffect effect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                    .get(effectId);
            if (effect != null && actualTarget.hasEffect(effect)) {
                int amplifier = actualTarget.getEffect(effect).getAmplifier();
                dmg += (amplifier + 1) * damagePerStack.evaluate(player, actualTarget);
            }
        }

        if (dmg > 0 || fireDuration > 0) {
            net.minecraft.world.damagesource.DamageSource source;
            net.minecraft.world.entity.Entity damageSourceEntity = "self".equalsIgnoreCase(sourceEntity) ? player
                    : ("target".equalsIgnoreCase(sourceEntity) ? target : null);

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
                    source = player.damageSources().playerAttack(player);
                }
            } else {
                source = player.damageSources().playerAttack(player);
            }

            if (fireDuration > 0) {
                actualTarget.setSecondsOnFire(fireDuration);
            }

            if (dmg > 0) {
                actualTarget.hurt(source, (float) dmg);
            }

            if (knockback != null) {
                float kb = (float) knockback.evaluate(player, actualTarget);
                if (kb > 0) {
                    actualTarget.knockback(kb, player.getX() - actualTarget.getX(),
                            player.getZ() - actualTarget.getZ());
                }
            }

            if (healAmount != null) {
                float heal = (float) healAmount.evaluate(player);
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
            int fire = net.minecraft.util.GsonHelper.getAsInt(json, "fire_duration", 0);
            ScalingValue heal = json.has("heal_amount") ? ScalingValue.fromJson(json, "heal_amount", 0.0) : null;
            ScalingValue dmgPerStack = json.has("damage_per_stack")
                    ? ScalingValue.fromJson(json, "damage_per_stack", 0.0)
                    : null;
            String effect = net.minecraft.util.GsonHelper.getAsString(json, "stack_effect", "");

            return new DamageAction(amount, damageType, knockback, source, fire, heal, dmgPerStack, effect);
        });
    }
}
