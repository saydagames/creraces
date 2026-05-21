package mc.sayda.creraces.engine;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

/**
 * Handles specialized movement for aquatic races, including neutral buoyancy and
 * physics bypass for races that walk on the seabed.
 */
public class AquaticMovementHandler {

    /**
     * Periodic tick for aquatic movement features.
     */
    public static void buoyancyTick(LivingEntity entity) {
        if (entity instanceof Player player) {
            if (player.isPassenger()) return;
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

    /**
     * Determines if an entity should bypass all fluid physics for a specific fluid.
     * This is the source of truth for land-physics-in-water.
     */
    public static boolean isUnaffected(LivingEntity entity, TagKey<Fluid> tag) {
        if (entity instanceof Player player) {
            var vars = DataUtils.getVariables(player).orElse(null);
            if (vars != null) {
                Race race = RaceRegistry.get(vars.getRace());
                if (race != null) {
                    var passives = race.passives();
                    if (passives != null) {
                        if (tag.equals(FluidTags.WATER) && passives.unaffectedByWater()) {
                            return true;
                        }
                        if (tag.equals(FluidTags.LAVA) && passives.unaffectedByLava()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static void handleBuoyancy(Player player,
            @javax.annotation.Nullable mc.sayda.creraces.engine.traits.AquaticMovementTrait trait) {
        
        boolean hasTrait = trait != null && trait.isNeutralBuoyancy();
        boolean inWater = player.isInWaterOrBubble();

        // If the race is unaffected by water physics (walking on seabed), don't apply buoyancy
        boolean walksOnSeabed = isUnaffected(player, FluidTags.WATER) && inWater;

        if (hasTrait && inWater && !walksOnSeabed) {
            player.setNoGravity(true);
            player.resetFallDistance();
        } else {
            // Only toggle off if we ARE in a state that could have been set by this handler.
            if (player.isNoGravity() && (!inWater || !hasTrait || walksOnSeabed)) {
                player.setNoGravity(false);
            }
        }
    }
}
