package mc.sayda.creraces.engine;

import mc.sayda.creraces.capability.DataUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.minecraft.resources.ResourceLocation;

public class SpiritMobilityHandler {
    private static final ResourceLocation SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("creraces", "spirit_speed_boost");
    private static final AttributeModifier SPEED_MODIFIER = new AttributeModifier(SPEED_MODIFIER_ID,
            0.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    private static final ResourceLocation SPIRIT_DOUBLE_JUMP_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("creraces", "spirit_double_jump");
    private static final AttributeModifier SPIRIT_DOUBLE_JUMP_MODIFIER = new AttributeModifier(
            SPIRIT_DOUBLE_JUMP_MODIFIER_ID,
            1.0, AttributeModifier.Operation.ADD_VALUE);

    /**
     * Called from LivingEntityMixin to handle server-side/synced mechanics like
     * speed and slow fall.
     */
    public static void tick(LivingEntity entity) {
        // Slow Fall logic
        if (isSpirit(entity)) {
            Vec3 vel = entity.getDeltaMovement();
            // Constant drift downwards if falling
            if (vel.y < -0.4) {
                entity.setDeltaMovement(vel.x, -0.4, vel.z);
                entity.resetFallDistance();
            }
        }

        if (entity.level().isClientSide())
            return;

        if (entity instanceof Player) {
            Player player = (Player) entity;
            DataUtils.getVariables(player).ifPresent(vars -> {
                var speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speedAttr != null) {
                    boolean hasMod = speedAttr.getModifier(SPEED_MODIFIER_ID) != null;
                    if (vars.isInSpiritRealm()) {
                        if (!hasMod) {
                            speedAttr.addPermanentModifier(SPEED_MODIFIER);
                        }
                    } else {
                        if (hasMod) {
                            speedAttr.removeModifier(SPEED_MODIFIER_ID);
                        }
                    }
                }

                var doubleJumpAttr = player.getAttribute(mc.sayda.creraces.registry.ModAttributes.DOUBLE_JUMP);
                if (doubleJumpAttr != null) {
                    boolean hasMod = doubleJumpAttr.getModifier(SPIRIT_DOUBLE_JUMP_MODIFIER_ID) != null;
                    if (vars.isInSpiritRealm()) {
                        if (!hasMod) {
                            doubleJumpAttr.addPermanentModifier(SPIRIT_DOUBLE_JUMP_MODIFIER);
                        }
                    } else {
                        if (hasMod) {
                            doubleJumpAttr.removeModifier(SPIRIT_DOUBLE_JUMP_MODIFIER_ID);
                        }
                    }
                }
            });
        }
    }

    public static boolean isSpirit(LivingEntity entity) {
        if (entity instanceof Player) {
            Player player = (Player) entity;
            return DataUtils.getVariables(player).map(v -> v.isInSpiritRealm() || v.isSpirit())
                    .orElse(false);
        }
        return entity.getTags().contains("creraces:spirit") || entity.getTags().contains("creraces:in_spirit_realm");
    }

    @SuppressWarnings("null")
    public static <T extends net.minecraft.core.particles.ParticleOptions> void sendParticlesIfSpirit(
            net.minecraft.server.level.ServerLevel level, T particle,
            double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        for (net.minecraft.server.level.ServerPlayer p : level.players()) {
            if (isSpirit(p)) {
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket(
                        particle, false, x, y, z, (float) dx, (float) dy, (float) dz, (float) speed, count));
            }
        }
    }
}
