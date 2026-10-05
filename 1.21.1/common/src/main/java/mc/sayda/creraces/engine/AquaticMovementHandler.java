package mc.sayda.creraces.engine;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.TraitRegistry.RaceTrait;
import mc.sayda.creraces.engine.traits.AquaticMovementTrait;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

import javax.annotation.Nullable;

/**
 * Water movement driven by race data: neutral buoyancy from the aquatic_movement trait, and the
 * unaffected_by_water/lava passives that let a race walk on the seabed with land physics.
 */
public class AquaticMovementHandler {

    public static void buoyancyTick(LivingEntity entity) {
        if (entity instanceof Player player) {
            if (player.isPassenger()) return;
            DataUtils.getVariables(player).ifPresent(vars -> {
                Race race = RaceRegistry.get(vars.getRace());
                if (race != null) {
                    AquaticMovementTrait aquaticTrait = null;
                    for (RaceTrait trait : TraitDispatch.forPlayer(player)) {
                        if (trait instanceof AquaticMovementTrait at) {
                            aquaticTrait = at;
                            break;
                        }
                    }
                    handleBuoyancy(player, aquaticTrait);
                }
            });
        }
    }

    /** Whether the entity ignores all physics of the given fluid. Mixins treat this as the source of truth. */
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

    private static void handleBuoyancy(Player player, @Nullable AquaticMovementTrait trait) {
        boolean hasTrait = trait != null && trait.isNeutralBuoyancy();
        boolean inWater = player.isInWaterOrBubble();

        // Seabed walkers keep land physics, so buoyancy must not lift them.
        boolean walksOnSeabed = isUnaffected(player, FluidTags.WATER) && inWater;

        if (hasTrait && inWater && !walksOnSeabed) {
            player.setNoGravity(true);
            player.resetFallDistance();
        } else if (player.isNoGravity()) {
            // Undo the flag set above once buoyancy no longer applies.
            player.setNoGravity(false);
        }
    }
}
