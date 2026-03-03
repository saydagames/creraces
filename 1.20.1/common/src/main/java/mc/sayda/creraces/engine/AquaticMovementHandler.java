package mc.sayda.creraces.engine;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class AquaticMovementHandler {
    private static final UUID SWIM_SPEED_MODIFIER_UUID = UUID.fromString("617462d7-b364-4638-8547-203a92a50334");

    public static void tick(LivingEntity entity) {
        if (entity instanceof Player player) {
            DataUtils.getVariables(player).ifPresent(vars -> {
                Race race = RaceRegistry.get(vars.getRace());
                if (race != null) {
                    mc.sayda.creraces.engine.traits.AquaticMovementTrait aquaticTrait = null;
                    if (race.traits() != null) {
                        for (mc.sayda.creraces.engine.TraitRegistry.RaceTrait trait : race.traits()) {
                            if (trait instanceof mc.sayda.creraces.engine.traits.AquaticMovementTrait at) {
                                aquaticTrait = at;
                                break;
                            }
                        }
                    }

                    if (!player.level().isClientSide()) {
                        handleSpeed(player, race, aquaticTrait);
                        handleVision(player, race);
                    }

                    handleBuoyancy(player, aquaticTrait);
                }
            });
        }
    }

    private static void handleSpeed(Player player, Race race,
            @javax.annotation.Nullable mc.sayda.creraces.engine.traits.AquaticMovementTrait trait) {
        @SuppressWarnings("null") // Attributes.MOVEMENT_SPEED is a DeferredHolder — non-null at runtime
        Attribute movementSpeed = Attributes.MOVEMENT_SPEED;
        AttributeInstance speedAttr = player.getAttribute(movementSpeed);
        if (speedAttr == null)
            return;

        Race.Passives passives = race.passives() != null ? race.passives() : Race.Passives.DEFAULT;
        double multiplier = passives.liquidSpeedMultiplier().evaluate(player);
        if (trait != null) {
            multiplier *= trait.getSpeed().evaluate(player);
        }

        boolean inLiquid = player.isInWater() || player.isInLava();
        @SuppressWarnings("null")
        boolean hasMod = speedAttr.getModifier(SWIM_SPEED_MODIFIER_UUID) != null;

        if (inLiquid && multiplier != 1.0) {
            if (!hasMod) {
                @SuppressWarnings("null")
                AttributeModifier mod = new AttributeModifier(
                        SWIM_SPEED_MODIFIER_UUID,
                        "Aquatic swim speed",
                        multiplier - 1.0,
                        AttributeModifier.Operation.MULTIPLY_TOTAL);
                speedAttr.addPermanentModifier(mod);
            }
        } else {
            if (hasMod) {
                @SuppressWarnings("null")
                UUID uuid = SWIM_SPEED_MODIFIER_UUID;
                speedAttr.removeModifier(uuid);
            }
        }
    }

    private static void handleBuoyancy(Player player,
            @javax.annotation.Nullable mc.sayda.creraces.engine.traits.AquaticMovementTrait trait) {
        if (trait == null || !trait.isNeutralBuoyancy() || !player.isInWater())
            return;

        // Note: This logic needs to run on both sides for smooth movement
        net.minecraft.world.phys.Vec3 vel = player.getDeltaMovement();

        // If not rising or falling via input
        boolean rising = ((mc.sayda.creraces.mixin.LivingEntityAccessor) player).isJumping();
        boolean falling = player.isShiftKeyDown();

        if (!rising && !falling) {
            // Zero out downward velocity to counteract water gravity
            if (vel.y < 0) {
                player.setDeltaMovement(vel.x, 0, vel.z);
                player.resetFallDistance();
            }
        }
    }

    private static void handleVision(Player player, Race race) {
        Race.Passives passives = race.passives() != null ? race.passives() : Race.Passives.DEFAULT;

        // Water Vision: server-side Night Vision effect.
        // Client-side fog/overlay removal is handled by FogRendererMixin +
        // LiquidOverlayMixin.
        if (passives.waterVision() && player.isInWater()) {
            MobEffect nightVision = MobEffects.NIGHT_VISION;
            if (nightVision != null) {
                player.addEffect(new MobEffectInstance(nightVision, 205, 0, false, false, false));
            }
        }

        // Lava Vision: also grants Fire Resistance so the player can actually use the
        // sight.
        // Client-side fog/overlay removal is handled by FogRendererMixin +
        // LiquidOverlayMixin.
        if (passives.lavaVision() && player.isInLava()) {
            MobEffect nightVision = MobEffects.NIGHT_VISION;
            MobEffect fireResistance = MobEffects.FIRE_RESISTANCE;
            if (nightVision != null) {
                player.addEffect(new MobEffectInstance(nightVision, 205, 0, false, false, false));
            }
            if (fireResistance != null) {
                player.addEffect(new MobEffectInstance(fireResistance, 205, 0, false, false, false));
            }
        }
    }
}
