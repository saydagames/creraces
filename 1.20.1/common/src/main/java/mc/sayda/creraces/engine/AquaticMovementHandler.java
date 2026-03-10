package mc.sayda.creraces.engine;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
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
                    }

                    handleBuoyancy(player, aquaticTrait);
                }
            });
        }
    }

    public static void buoyancyTick(LivingEntity entity) {
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

        boolean inWater = player.isInWater();
        boolean inLava = player.isInLava();

        // unaffectedByWater / unaffectedByLava: treat liquid as air for movement
        if (inWater && passives.unaffectedByWater())
            multiplier = 1.0;
        if (inLava && passives.unaffectedByLava())
            multiplier = 1.0;

        boolean inLiquid = inWater || inLava;
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

        net.minecraft.world.phys.Vec3 vel = player.getDeltaMovement();

        boolean rising = ((mc.sayda.creraces.mixin.LivingEntityAccessor) player).isJumping();
        boolean sinking = player.isShiftKeyDown();

        if (!rising && !sinking) {
            // Clamp Y to 0: prevents both sinking (negative Y from gravity)
            // and unwanted upward drift. The player floats in place.
            player.setDeltaMovement(vel.x, 0.0, vel.z);
            player.resetFallDistance();
        } else if (rising) {
            // Rising: allow positive Y velocity up to a gentle max
            if (vel.y < 0) {
                player.setDeltaMovement(vel.x, 0.0, vel.z);
            }
            player.resetFallDistance();
        }
        // sinking: leave velocity unchanged so Sneak naturally pulls them down
    }

}
