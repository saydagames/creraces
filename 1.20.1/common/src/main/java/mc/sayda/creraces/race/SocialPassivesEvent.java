package mc.sayda.creraces.race;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import mc.sayda.creraces.CreRaces;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Handles social passive events (defendedByEntities)
 */
public class SocialPassivesEvent {

    private static final double DEFENSE_RANGE = 16.0; // Blocks

    public static void register() {
        // Listen to player damage events for defensive allies
        EntityEvent.LIVING_HURT.register(SocialPassivesEvent::onPlayerHurt);
    }

    /**
     * When a player is hurt, nearby defending entities attack the source
     */
    private static EventResult onPlayerHurt(LivingEntity entity, DamageSource source, float amount) {
        // Only handle player damage
        if (!(entity instanceof Player player)) {
            return EventResult.pass();
        }

        // Only on server side
        if (player.level().isClientSide) {
            return EventResult.pass();
        }

        // Get the attacker
        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            return EventResult.pass();
        }

        // Get defenders for this race (will be empty if no defenders configured)
        List<String> defenders = SocialPassivesHelper.getDefenders(player);
        if (defenders.isEmpty()) {
            return EventResult.pass();
        }

        // Find nearby entities that defend this race (using tag-aware helper)
        Level level = player.level();
        AABB searchBox = player.getBoundingBox().inflate(DEFENSE_RANGE);
        List<Mob> nearbyMobs = level.getEntitiesOfClass(Mob.class, searchBox,
                mob -> SocialPassivesHelper.defendsRace(player, mob));

        // Make them target the attacker
        for (Mob defender : nearbyMobs) {
            if (defender.getTarget() == null || defender.getTarget() == player) {
                defender.setTarget(attacker);
            }
        }

        return EventResult.pass();
    }
}
