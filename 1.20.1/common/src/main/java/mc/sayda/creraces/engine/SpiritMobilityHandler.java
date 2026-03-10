package mc.sayda.creraces.engine;

import mc.sayda.creraces.capability.DataUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class SpiritMobilityHandler {
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("f8b4c2a1-3d7e-4b9a-8c5d-2e1f0b9a8c7d");
    private static final AttributeModifier SPEED_MODIFIER = new AttributeModifier(SPEED_MODIFIER_UUID,
            "Spirit Speed Boost", 0.3, AttributeModifier.Operation.MULTIPLY_TOTAL);

    private static final UUID SPIRIT_DOUBLE_JUMP_MODIFIER_ID = UUID.fromString("c0d3b4be-0001-4000-8000-000000000006");
    private static final AttributeModifier SPIRIT_DOUBLE_JUMP_MODIFIER = new AttributeModifier(
            SPIRIT_DOUBLE_JUMP_MODIFIER_ID,
            "Spirit Double Jump", 1.0, AttributeModifier.Operation.ADDITION);

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

        if (entity instanceof Player player) {
            DataUtils.getVariables(player).ifPresent(vars -> {
                var speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speedAttr != null) {
                    boolean hasMod = speedAttr.getModifier(SPEED_MODIFIER_UUID) != null;
                    if (vars.isInSpiritRealm()) {
                        if (!hasMod) {
                            speedAttr.addPermanentModifier(SPEED_MODIFIER);
                        }
                    } else {
                        if (hasMod) {
                            speedAttr.removeModifier(SPEED_MODIFIER_UUID);
                        }
                    }
                }

                var doubleJumpAttr = player.getAttribute(mc.sayda.creraces.registry.ModAttributes.DOUBLE_JUMP.get());
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
        if (entity instanceof Player player) {
            return DataUtils.getVariables(player).map(mc.sayda.creraces.capability.IPlayerVariables::isInSpiritRealm)
                    .orElse(false);
        }
        return entity.getTags().contains("creraces:spirit");
    }
}
