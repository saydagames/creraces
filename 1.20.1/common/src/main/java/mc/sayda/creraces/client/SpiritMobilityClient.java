package mc.sayda.creraces.client;

import dev.architectury.event.events.client.ClientTickEvent;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.client.render.SpiritRealmRenderer;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.registry.ModAttributes;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.Attribute;
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
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            LocalPlayer player = minecraft.player;
            if (player == null)
                return;

            DataUtils.getVariables(player).ifPresent(vars -> {
                Attribute doubleJumpAttr = ModAttributes.DOUBLE_JUMP.get();
                int maxAirJumps = doubleJumpAttr != null ? (int) player.getAttributeValue(doubleJumpAttr) : 0;

                boolean isJumpKeyDown = minecraft.options.keyJump.isDown();
                boolean onGround = player.onGround();

                if (onGround || player.onClimbable() || player.isInWater()) {
                    airJumps = 0;
                } else if (maxAirJumps > 0 && isJumpKeyDown && !wasJumpKeyDown && airJumps < maxAirJumps
                        && !wasOnGround) {
                    Vec3 vel = player.getDeltaMovement();
                    player.setDeltaMovement(vel.x, 0.42, vel.z);

                    BoundaryHandler.sendDoubleJump();
                    // The server plays the flap for everyone except this player, so play it here
                    // too; doing it locally also keeps it in step with the jump.
                    player.playSound(SoundEvents.ENDER_DRAGON_FLAP, 0.5f, 1.5f);

                    for (int i = 0; i < 10; ++i) {
                        double speedX = player.getRandom().nextGaussian() * 0.02D;
                        double speedY = player.getRandom().nextGaussian() * 0.02D;
                        double speedZ = player.getRandom().nextGaussian() * 0.02D;
                        player.level().addParticle(ParticleTypes.CLOUD,
                                player.getX() + (double) (player.getRandom().nextFloat() * player.getBbWidth() * 2.0F)
                                        - (double) player.getBbWidth(),
                                player.getY(),
                                player.getZ() + (double) (player.getRandom().nextFloat() * player.getBbWidth() * 2.0F)
                                        - (double) player.getBbWidth(),
                                speedX, speedY, speedZ);
                    }

                    player.fallDistance = 0;
                    airJumps++;
                }
                wasJumpKeyDown = isJumpKeyDown;
                wasOnGround = onGround;
            });

            SpiritRealmRenderer.spawnSpiritFlameParticles(minecraft);
        });
    }
}
