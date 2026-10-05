package mc.sayda.creraces.race;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Makes nearby defender mobs (a race's defended_by_entities) attack whoever hurts the player.
 */
public class SocialPassivesEvent {
    private static final double DEFENDER_SEARCH_RADIUS = 16.0;

    public static void register() {
        EntityEvent.LIVING_HURT.register(SocialPassivesEvent::onPlayerHurt);
    }

    private static EventResult onPlayerHurt(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof Player player)) {
            return EventResult.pass();
        }

        if (player.level().isClientSide) {
            return EventResult.pass();
        }

        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            return EventResult.pass();
        }

        List<String> defenders = SocialPassivesHelper.getDefenders(player);
        if (defenders.isEmpty()) {
            return EventResult.pass();
        }

        Level level = player.level();
        AABB searchBox = player.getBoundingBox().inflate(DEFENDER_SEARCH_RADIUS);
        List<Mob> nearbyMobs = level.getEntitiesOfClass(Mob.class, searchBox,
                mob -> SocialPassivesHelper.defendsRace(player, mob));

        // Defenders already fighting something else keep their target.
        for (Mob defender : nearbyMobs) {
            if (defender.getTarget() == null || defender.getTarget() == player) {
                defender.setTarget(attacker);
            }
        }

        return EventResult.pass();
    }
}
