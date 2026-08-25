package mc.sayda.creraces.client;

import dev.architectury.event.events.client.ClientTickEvent;
import mc.sayda.creraces.capability.DataUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public class SpiritMobilityClient {
    private static int airJumps = 0;
    private static boolean wasJumpKeyDown = false;
    private static boolean wasOnGround = true;

    public static void reset() {
        airJumps = 0;
        wasJumpKeyDown = false;
        wasOnGround = true;
    }

    @SuppressWarnings("null")
    public static void init() {
        // Named 'minecraft' instead of 'mc' to avoid shadowing the mc.sayda.creraces package prefix used throughout this scope.
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            LocalPlayer player = minecraft.player;
            if (player == null)
                return;

            DataUtils.getVariables(player).ifPresent(vars -> {
                net.minecraft.world.entity.ai.attributes.Attribute doubleJumpAttr = mc.sayda.creraces.registry.ModAttributes.DOUBLE_JUMP
                        .get();
                int maxAirJumps = doubleJumpAttr != null ? (int) player.getAttributeValue(doubleJumpAttr) : 0;

                boolean isJumpKeyDown = minecraft.options.keyJump.isDown();
                boolean onGround = player.onGround();

                if (onGround || player.onClimbable() || player.isInWater()) {
                    airJumps = 0;
                } else if (maxAirJumps > 0 && isJumpKeyDown && !wasJumpKeyDown && airJumps < maxAirJumps
                        && !wasOnGround) {
                    // Manual upward velocity for air jump
                    net.minecraft.world.phys.Vec3 vel = player.getDeltaMovement();
                    player.setDeltaMovement(vel.x, 0.42, vel.z);

                    // Sound and particle syncing
                    mc.sayda.creraces.network.BoundaryHandler.sendDoubleJump();

                    // Client-side feedback (instant)
                    player.playSound(net.minecraft.sounds.SoundEvents.ENDER_DRAGON_FLAP, 0.5f, 1.5f);

                    // Add cloud particles for 'completed' feel
                    for (int i = 0; i < 10; ++i) {
                        double d0 = player.getRandom().nextGaussian() * 0.02D;
                        double d1 = player.getRandom().nextGaussian() * 0.02D;
                        double d2 = player.getRandom().nextGaussian() * 0.02D;
                        player.level().addParticle(net.minecraft.core.particles.ParticleTypes.CLOUD,
                                player.getX() + (double) (player.getRandom().nextFloat() * player.getBbWidth() * 2.0F)
                                        - (double) player.getBbWidth(),
                                player.getY(),
                                player.getZ() + (double) (player.getRandom().nextFloat() * player.getBbWidth() * 2.0F)
                                        - (double) player.getBbWidth(),
                                d0, d1, d2);
                    }

                    player.fallDistance = 0;
                    airJumps++;
                }
                wasJumpKeyDown = isJumpKeyDown;
                wasOnGround = onGround;
            });

            mc.sayda.creraces.client.render.SpiritRealmRenderer.spawnSpiritFlameParticles(minecraft);
        });
    }
}
