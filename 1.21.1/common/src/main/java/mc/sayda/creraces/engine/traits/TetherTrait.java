package mc.sayda.creraces.engine.traits;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TraitRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Objects;

/**
 * Every interval, runs its actions on each nearby entity of the "target" type or #tag (other than
 * the player), with that entity as the target.
 */
public class TetherTrait extends PeriodicTrait {

    private final TagKey<EntityType<?>> targetTag;
    private final ResourceLocation targetId;
    private final ScalingValue radius;
    private final List<ActionRegistry.RaceAction> actions;

    public TetherTrait(ResourceLocation traitId, String target, ScalingValue radius,
            List<ActionRegistry.RaceAction> actions, ScalingValue interval) {
        super(traitId, interval);
        this.radius = radius;
        this.actions = actions;

        if (target.startsWith("#")) {
            this.targetTag = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(target.substring(1)));
            this.targetId = null;
        } else {
            this.targetTag = null;
            this.targetId = ResourceLocation.parse(target);
        }
    }

    @Override
    protected boolean shouldExecute(Player player, IPlayerVariables vars) {
        return true;
    }

    @Override
    protected void execute(Player player, IPlayerVariables vars) {
        double r = Math.max(0, radius.evaluate(player, null));
        int maxAoeRadius = CreRacesConfig.AOE_MAX_RADIUS.get();
        if (maxAoeRadius > 0)
            r = Math.min(r, maxAoeRadius);

        final AABB playerBox = Objects.requireNonNull(player.getBoundingBox());
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class,
                Objects.requireNonNull(playerBox.inflate(r)), e -> e != player && matchesTarget(e));

        for (LivingEntity target : targets) {
            for (ActionRegistry.RaceAction action : actions) {
                action.execute(player, target, null, null);
            }
        }
    }

    private boolean matchesTarget(LivingEntity entity) {
        if (targetTag != null) {
            return entity.getType().is(targetTag);
        }
        return EntityType.getKey(entity.getType()).equals(targetId);
    }

    public static void register() {
        TraitRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "tether"), json -> {
            String target = json.has("target") ? json.get("target").getAsString() : "minecraft:player";
            ScalingValue radius = ScalingValue.fromJson(json, "radius", 10.0);
            ScalingValue interval = ScalingValue.fromJson(json, "interval", 20.0);
            return new TetherTrait(TraitIds.fromJson(json, "tether_"), target, radius,
                    ActionRegistry.listFromJson(json, "actions"), interval);
        });
    }
}
