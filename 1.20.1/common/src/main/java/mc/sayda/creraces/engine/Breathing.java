package mc.sayda.creraces.engine;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.function.IntUnaryOperator;

/**
 * Air for races that don't breathe like a human. A race breathes open air unless
 * creraces:can_breathe_on_land is false, and also water with can_breathe_underwater or
 * can_breathe_in_water, or lava with can_breathe_in_lava. For such a race these rules replace vanilla's outright: every loader changes
 * air through setAirSupply during baseTick, so LivingEntityMixin has that ignored for the player and
 * runs the rules once baseTick is done. They run on both sides, so the client's air bar matches.
 */
public final class Breathing {
    /** The entity whose vanilla air changes are ignored right now; client and server tick on their own threads. */
    private static final ThreadLocal<LivingEntity> TAKEN_OVER = new ThreadLocal<>();

    private enum Medium { AIR, WATER, LAVA }

    private Breathing() {
    }

    /** As baseTick starts: takes air over from vanilla for players whose race breathes differently. */
    public static void beginBaseTick(LivingEntity entity) {
        TAKEN_OVER.set(entity.isAlive() && passives(entity) != null ? entity : null);
    }

    /** Whether a setAirSupply call is vanilla's breathing for an entity this class has taken over. */
    public static boolean ignoresVanillaAir(Entity entity) {
        return TAKEN_OVER.get() == entity;
    }

    /** As baseTick ends: applies the race's rules, with vanilla's own drain (Respiration) and refill. */
    public static void endBaseTick(LivingEntity entity, IntUnaryOperator drain, IntUnaryOperator refill) {
        if (TAKEN_OVER.get() != entity) {
            return;
        }
        TAKEN_OVER.remove();
        Race.Passives passives = passives(entity);
        if (passives != null && entity instanceof Player player && player.isAlive()) {
            tick(player, passives, drain, refill);
        }
    }

    private static void tick(Player player, Race.Passives passives, IntUnaryOperator drain, IntUnaryOperator refill) {
        Medium medium = medium(player);
        int air = player.getAirSupply();
        if (canBreathe(player, passives, medium)) {
            if (air < player.getMaxAirSupply()) {
                player.setAirSupply(refill.applyAsInt(air));
            }
            return;
        }

        // Open air drains at the race's land rate; a liquid it can't breathe drains as fast as water does.
        int interval = medium == Medium.AIR ? Math.max(1, passives.landSuffocationInterval()) : 1;
        if (player.tickCount % interval == 0) {
            air = drain.applyAsInt(air);
            player.setAirSupply(air);
        }
        if (air <= -20) {
            player.setAirSupply(0);
            if (medium == Medium.WATER) {
                spawnDrowningBubbles(player);
            }
            if (!player.level().isClientSide()) {
                player.hurt(player.damageSources().drown(), 2.0F);
            }
        }
    }

    private static boolean canBreathe(Player player, Race.Passives passives, Medium medium) {
        if (player.getAbilities().invulnerable) {
            return true;
        }
        boolean waterBreathing = MobEffectUtil.hasWaterBreathing(player);
        return switch (medium) {
            // Asks the player, so undead races never drown here either, as with vanilla undead.
            case WATER -> player.canBreatheUnderwater() || waterBreathing;
            // Lava never drowns anyone in vanilla, so air breathers keep breathing in it.
            case LAVA -> passives.canBreatheInLava() || passives.canBreatheOnLand();
            // A race that can't breathe air holds on under the matching effect: Water Breathing for water,
            // Fire Resistance for lava. Its eyes can only be in water here inside a bubble column. With
            // can_breathe_in_water or lava breathing, standing in the liquid (or rain, for water) is enough.
            case AIR -> passives.canBreatheOnLand()
                    || passives.canBreatheUnderwater() && (waterBreathing || player.isEyeInFluid(FluidTags.WATER))
                    || passives.canBreatheInWater()
                            && (player.isInWaterRainOrBubble() || touches(player, FluidTags.WATER))
                    || passives.canBreatheInLava()
                            && (touches(player, FluidTags.LAVA) || player.hasEffect(MobEffects.FIRE_RESISTANCE));
        };
    }

    /**
     * Whether the turtle helmet should treat the player as underwater. Vanilla tops up Water Breathing
     * while the head is out of water, for a dive. A race that breathes water but not air gets the
     * reverse: the helmet charges underwater and lasts its 10 seconds on land.
     */
    public static boolean turtleHelmetSeesWater(Player player, boolean eyesInWater) {
        Race.Passives passives = passives(player);
        boolean breathesOnlyWater = passives != null && passives.canBreatheUnderwater() && !passives.canBreatheOnLand();
        return breathesOnlyWater ? !eyesInWater : eyesInWater;
    }

    /** What the player's eyes are in. As in vanilla, a bubble column counts as air. */
    private static Medium medium(Player player) {
        if (player.isEyeInFluid(FluidTags.LAVA)) {
            return Medium.LAVA;
        }
        BlockPos eyes = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        if (player.isEyeInFluid(FluidTags.WATER) && !player.level().getBlockState(eyes).is(Blocks.BUBBLE_COLUMN)) {
            return Medium.WATER;
        }
        return Medium.AIR;
    }

    /**
     * Whether the fluid fills the block at the player's feet or waist. Read from the world rather than
     * isInWater/isInLava, which stay false for races that ignore the fluid's physics.
     */
    private static boolean touches(Player player, TagKey<Fluid> fluid) {
        BlockPos waist = BlockPos.containing(player.getX(), player.getY() + player.getBbHeight() / 2, player.getZ());
        return player.level().getFluidState(player.blockPosition()).is(fluid)
                || player.level().getFluidState(waist).is(fluid);
    }

    /** Vanilla's burst of bubbles when a drowning hit lands; only the client shows them. */
    private static void spawnDrowningBubbles(Player player) {
        RandomSource random = player.getRandom();
        Vec3 motion = player.getDeltaMovement();
        for (int i = 0; i < 8; i++) {
            player.level().addParticle(ParticleTypes.BUBBLE,
                    player.getX() + random.nextDouble() - random.nextDouble(),
                    player.getY() + random.nextDouble() - random.nextDouble(),
                    player.getZ() + random.nextDouble() - random.nextDouble(),
                    motion.x, motion.y, motion.z);
        }
    }

    /** The race passives of a player whose breathing differs from a human's, otherwise null. */
    @Nullable
    private static Race.Passives passives(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return null;
        }
        return DataUtils.getVariables(player)
                .map(vars -> RaceRegistry.get(vars.getRace()))
                .map(Race::passives)
                .filter(Race.Passives::hasCustomBreathing)
                .orElse(null);
    }
}
