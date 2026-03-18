package mc.sayda.creraces.engine;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;


public class AquaticMovementHandler {

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
        // Speed logic removed to fix FOV zoom. 
        // We only use the trait for buoyancy/gravity logic now.
    }

    private static void handleBuoyancy(Player player,
            @javax.annotation.Nullable mc.sayda.creraces.engine.traits.AquaticMovementTrait trait) {
        boolean inWater = player.isInWaterOrBubble();
        boolean hasTrait = trait != null && trait.isNeutralBuoyancy();

        if (hasTrait && inWater) {
            player.setNoGravity(true);
            player.resetFallDistance();
        } else {
            // Only toggle off if we ARE in a state that could have been set by this handler.
            // Since Minecraft handles gravity for many things, we just ensure it's off when not in water.
            if (player.isNoGravity() && (!inWater || !hasTrait)) {
                player.setNoGravity(false);
            }
        }
    }

}
