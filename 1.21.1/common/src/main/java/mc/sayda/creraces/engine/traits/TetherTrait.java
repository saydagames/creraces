package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonArray;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.capability.IPlayerVariables;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TetherTrait extends PeriodicTrait {

    private final String targetStr;
    private final TagKey<EntityType<?>> targetTag;
    private final ResourceLocation targetId;
    private final ScalingValue radius;
    private final List<ActionRegistry.RaceAction> actions;

    public TetherTrait(ResourceLocation traitId, String targetStr,
            ScalingValue radius,
            List<ActionRegistry.RaceAction> actions, ScalingValue interval) {
        super(traitId, interval);
        this.targetStr = targetStr;
        this.radius = radius;
        this.actions = actions;

        if (this.targetStr.startsWith("#")) {
            this.targetTag = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(this.targetStr.substring(1)));
            this.targetId = null;
        } else {
            this.targetTag = null;
            this.targetId = ResourceLocation.parse(this.targetStr);
        }
    }

    @Override
    protected boolean shouldExecute(Player player, IPlayerVariables vars) {
        return !player.level().isClientSide();
    }

    @Override
    protected void execute(Player player, IPlayerVariables vars) {
        if (player.level().isClientSide())
            return;

        double r = Math.max(0, radius.evaluate(player, null));
        int maxAoeRadius = mc.sayda.creraces.config.CreRacesConfig.AOE_MAX_RADIUS.get();
        if (maxAoeRadius > 0)
            r = Math.min(r, maxAoeRadius);

        final AABB playerBox = Objects.requireNonNull(player.getBoundingBox());
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class,
                Objects.requireNonNull(playerBox.inflate(r)), e -> {
                    if (e == player || e == null)
                        return false;
                    return matchesTarget(e);
                });

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
            String traitName = json.has("name") ? json.get("name").getAsString()
                    : "tether_" + Math.abs(json.toString().hashCode());
            ResourceLocation traitId = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, traitName);

            String target = json.has("target") ? json.get("target").getAsString() : "minecraft:player";

            ScalingValue radius = ScalingValue.fromJson(json, "radius", 10.0);
            ScalingValue interval = ScalingValue.fromJson(json, "interval", 20.0);
            List<ActionRegistry.RaceAction> actions = new ArrayList<>();
            if (json.has("actions")) {
                JsonArray array = json.getAsJsonArray("actions");
                for (int i = 0; i < array.size(); i++) {
                    actions.add(ActionRegistry.fromJson(array.get(i).getAsJsonObject()));
                }
            }
            return new TetherTrait(traitId, target, radius, actions, interval);
        });
    }
}
