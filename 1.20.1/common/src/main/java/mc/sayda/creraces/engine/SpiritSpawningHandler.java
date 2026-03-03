package mc.sayda.creraces.engine;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import mc.sayda.creraces.capability.DataUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class SpiritSpawningHandler {
    public static void init() {
        EntityEvent.ADD.register((entity, level) -> {
            if (level.isClientSide())
                return EventResult.pass();

            if (entity.getTags().contains("creraces:spirit")) {
                // If it's a spirit mob, check if any player in spirit realm is nearby (64
                // blocks)
                boolean playerInSpiritNearby = false;
                List<Player> players = level.getEntitiesOfClass(Player.class,
                        new AABB(entity.blockPosition()).inflate(64));
                for (Player p : players) {
                    if (DataUtils.getVariables(p).map(vars -> vars.isInSpiritRealm()).orElse(false)) {
                        playerInSpiritNearby = true;
                        break;
                    }
                }

                if (!playerInSpiritNearby) {
                    // Discard the entity if no spirit-aware player is around to see it
                    entity.discard();
                    return EventResult.interruptFalse();
                }
            }
            return EventResult.pass();
        });
    }
}
