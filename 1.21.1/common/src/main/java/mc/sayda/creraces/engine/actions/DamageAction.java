package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.CastSource;
import mc.sayda.creraces.engine.EntitySelector;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.team.RaceTeamManager;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Hurts an entity (the target unless "entity" says "self"). "source" is who the damage counts as
 * coming from: "self" (default), "target" or "none". Allies are never hurt while friendly fire is off.
 */
@SuppressWarnings("null")
public class DamageAction implements ActionRegistry.RaceAction {
    private enum Source { SELF, TARGET, NONE }

    private final ScalingValue amount;
    private final ResourceLocation damageType;
    private final Source source;
    @Nullable
    private final ScalingValue knockback;
    private final boolean disableKnockback;
    private final EntitySelector entity;

    public DamageAction(ScalingValue amount, ResourceLocation damageType, Source source,
            @Nullable ScalingValue knockback, boolean disableKnockback, EntitySelector entity) {
        this.amount = amount;
        this.damageType = damageType;
        this.source = source;
        this.knockback = knockback;
        this.disableKnockback = disableKnockback;
        this.entity = entity;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        LivingEntity victim = entity.resolve(player, target);
        if (victim == null || victim != player && !RaceTeamManager.canHurt(victim, player)) {
            return true;
        }
        double damage = amount.evaluate(player, target, slot, interactPos) * CastSource.damageMultiplier();
        if (damage > 0) {
            DamageSource damageSource = createDamageSource(player, target);
            if (disableKnockback) {
                Vec3 motion = victim.getDeltaMovement();
                victim.hurt(damageSource, (float) damage);
                victim.setDeltaMovement(motion);
                // Players only pick up the restored motion when it is force-synced.
                victim.hurtMarked = true;
            } else {
                victim.hurt(damageSource, (float) damage);
            }
        }
        if (knockback != null) {
            float strength = (float) knockback.evaluate(player, target, slot, interactPos);
            if (strength > 0) {
                LivingEntity source = CastSource.self(player);
                victim.knockback(strength, source.getX() - victim.getX(), source.getZ() - victim.getZ());
            }
        }
        return true;
    }

    private DamageSource createDamageSource(Player player, @Nullable LivingEntity target) {
        Entity attacker = switch (source) {
            case SELF -> player;
            case TARGET -> target;
            case NONE -> null;
        };
        Registry<DamageType> registry = player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        Holder<DamageType> type = registry.getHolder(ResourceKey.create(Registries.DAMAGE_TYPE, damageType))
                .orElseGet(() -> {
                    CreRaces.LOGGER.warn("DamageAction: unknown damage type '{}', using player_attack", damageType);
                    return registry.getHolderOrThrow(DamageTypes.PLAYER_ATTACK);
                });
        return new DamageSource(type, attacker, attacker);
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("damage"), json -> {
            String typeText = GsonHelper.getAsString(json, "damage_type", "minecraft:player_attack");
            // An id without a namespace is a vanilla damage type.
            ResourceLocation damageType = ResourceLocation.tryParse(typeText.contains(":") ? typeText : "minecraft:" + typeText);
            if (damageType == null) {
                throw new IllegalArgumentException("malformed damage_type '" + typeText + "'");
            }
            String sourceName = GsonHelper.getAsString(json, "source", "self");
            Source source = switch (sourceName) {
                case "self" -> Source.SELF;
                case "target" -> Source.TARGET;
                case "none" -> Source.NONE;
                default -> throw new IllegalArgumentException("unknown source '" + sourceName + "' (self, target, none)");
            };
            return new DamageAction(
                    ScalingValue.fromJson(json, "amount", 1.0),
                    damageType,
                    source,
                    json.has("knockback") ? ScalingValue.fromJson(json, "knockback", 0.0) : null,
                    GsonHelper.getAsBoolean(json, "disable_knockback", false),
                    EntitySelector.fromJson(json, EntitySelector.TARGET));
        });
    }
}
