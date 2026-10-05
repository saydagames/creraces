package mc.sayda.creraces.engine;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * Spirit mobs (entity tag creraces:spirit) only exist while a spirit-realm player is close enough
 * to see them; any that would be added to the level otherwise are discarded instead.
 */
public class SpiritSpawningHandler {
    private static final int SPIRIT_VIEWER_RANGE = 16;

    public static void init() {
        EntityEvent.ADD.register((entity, level) -> {
            if (level.isClientSide())
                return EventResult.pass();

            if (entity.getTags().contains("creraces:spirit")) {
                boolean spiritViewerNearby = false;
                for (Player p : level.getEntitiesOfClass(Player.class,
                        new AABB(entity.blockPosition()).inflate(SPIRIT_VIEWER_RANGE))) {
                    if (DataUtils.getVariables(p).map(IPlayerVariables::isInSpiritRealm).orElse(false)) {
                        spiritViewerNearby = true;
                        break;
                    }
                }

                if (!spiritViewerNearby) {
                    entity.discard();
                    return EventResult.interruptFalse();
                }
            }
            return EventResult.pass();
        });
    }
}
