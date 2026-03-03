package mc.sayda.creraces.client;

import dev.architectury.event.events.client.ClientTickEvent;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.engine.TraitRegistry;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public class SpiritMobilityClient {
    private static int airJumps = 0;
    private static boolean wasJumpKeyDown = false;
    private static boolean wasOnGround = true;

    public static void init() {
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            LocalPlayer player = minecraft.player;
            if (player == null)
                return;

            DataUtils.getVariables(player).ifPresent(vars -> {
                int maxAirJumps = 0;
                Race race = RaceRegistry.get(vars.getRace());
                if (race != null && race.traits() != null) {
                    for (TraitRegistry.RaceTrait trait : race.traits()) {
                        if (trait instanceof mc.sayda.creraces.engine.traits.DoubleJumpTrait djt) {
                            maxAirJumps = (int) djt.getMaxJumps().evaluate(player);
                            break;
                        }
                    }
                }

                if (vars.isInSpiritRealm()) {
                    maxAirJumps += 1;
                }

                boolean isJumpKeyDown = minecraft.options.keyJump.isDown();
                boolean onGround = player.onGround();

                if (onGround || player.onClimbable() || player.isInWater()) {
                    airJumps = 0;
                } else if (maxAirJumps > 0 && isJumpKeyDown && !wasJumpKeyDown && airJumps < maxAirJumps
                        && !wasOnGround) {
                    // Manual upward velocity for air jump
                    Vec3 vel = player.getDeltaMovement();
                    player.setDeltaMovement(vel.x, 0.42, vel.z);

                    // Client-side feedback
                    player.playSound(net.minecraft.sounds.SoundEvents.ENDER_DRAGON_FLAP, 0.5f, 1.5f);
                    player.fallDistance = 0;
                    airJumps++;
                }
                wasJumpKeyDown = isJumpKeyDown;
                wasOnGround = onGround;
            });
        });
    }
}
