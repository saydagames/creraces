package mc.sayda.creraces.engine;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.registry.ModAttributes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

public class SpiritMobilityHandler {
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("f8b4c2a1-3d7e-4b9a-8c5d-2e1f0b9a8c7d");
    private static final AttributeModifier SPEED_MODIFIER = new AttributeModifier(SPEED_MODIFIER_UUID,
            "Spirit Speed Boost", 0.3, AttributeModifier.Operation.MULTIPLY_TOTAL);

    private static final UUID SPIRIT_DOUBLE_JUMP_MODIFIER_ID = UUID.fromString("c0d3b4be-0001-4000-8000-000000000006");
    private static final AttributeModifier SPIRIT_DOUBLE_JUMP_MODIFIER = new AttributeModifier(
            SPIRIT_DOUBLE_JUMP_MODIFIER_ID,
            "Spirit Double Jump", 1.0, AttributeModifier.Operation.ADDITION);

    private static final double MAX_SPIRIT_FALL_SPEED = 0.4;

    /**
     * Ticked from LivingEntityMixin. Caps how fast spirits fall (on both sides), and on the server
     * keeps the spirit-realm speed and double-jump modifiers in step with the realm state.
     */
    public static void tick(LivingEntity entity) {
        if (isOnSpiritPlane(entity)) {
            Vec3 vel = entity.getDeltaMovement();
            if (vel.y < -MAX_SPIRIT_FALL_SPEED) {
                entity.setDeltaMovement(vel.x, -MAX_SPIRIT_FALL_SPEED, vel.z);
                entity.resetFallDistance();
            }
        }

        if (entity.level().isClientSide())
            return;

        if (entity instanceof Player player) {
            DataUtils.getVariables(player).ifPresent(vars -> {
                boolean inRealm = vars.isInSpiritRealm();
                setModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_MODIFIER, inRealm);
                setModifier(player.getAttribute(ModAttributes.DOUBLE_JUMP.get()), SPIRIT_DOUBLE_JUMP_MODIFIER, inRealm);
            });
        }
    }

    private static void setModifier(@Nullable AttributeInstance attribute, AttributeModifier modifier, boolean active) {
        if (attribute == null)
            return;
        boolean present = attribute.getModifier(modifier.getId()) != null;
        if (active && !present) {
            attribute.addPermanentModifier(modifier);
        } else if (!active && present) {
            attribute.removeModifier(modifier.getId());
        }
    }

    /**
     * Whether spirit-plane rules (visibility, targeting, damage and fall immunity) apply to this entity
     * right now. For players that is being in the spirit realm, not being a spirit: a human in the realm
     * is on the plane, a fairy outside it is not. Mobs have no realm state of their own, so spirit mobs count.
     */
    public static boolean isOnSpiritPlane(LivingEntity entity) {
        if (entity instanceof Player player) {
            return DataUtils.getVariables(player).map(IPlayerVariables::isInSpiritRealm).orElse(false);
        }
        return entity.getTags().contains("creraces:spirit") || entity.getTags().contains("creraces:in_spirit_realm");
    }

    @SuppressWarnings("null")
    public static <T extends ParticleOptions> void sendParticlesIfSpirit(ServerLevel level, T particle,
            double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        for (ServerPlayer p : level.players()) {
            if (isOnSpiritPlane(p)) {
                p.connection.send(new ClientboundLevelParticlesPacket(
                        particle, false, x, y, z, (float) dx, (float) dy, (float) dz, (float) speed, count));
            }
        }
    }
}
