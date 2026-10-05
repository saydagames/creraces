package mc.sayda.creraces.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

// Distinguishes Remains left by a tagged undead servant from a plain Remains, so the
// entity_type condition and RemainsRenderer's texture switch can tell them apart. Ownership
// and the original entity type are stored via the generic persistent-data tags
// creraces:owner / creraces:stored_entity, not bespoke fields, so generic conditions/actions
// (entity_data, summon_entity) can read them.
public class UndeadRemainsEntity extends RemainsEntity {
    public UndeadRemainsEntity(EntityType<? extends UndeadRemainsEntity> type, Level level) {
        super(type, level);
    }
}
