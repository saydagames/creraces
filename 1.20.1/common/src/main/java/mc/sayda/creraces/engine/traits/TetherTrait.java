package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonArray;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.TraitRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class TetherTrait implements TraitRegistry.RaceTrait {

    private final String targetTag; // e.g., "minecraft:skeletons"
    private final mc.sayda.creraces.engine.ScalingValue radius;
    private final List<ActionRegistry.RaceAction> actions;
    private final int interval;
    private int timer = 0;

    public TetherTrait(String targetTag, mc.sayda.creraces.engine.ScalingValue radius,
            List<ActionRegistry.RaceAction> actions, int interval) {
        this.targetTag = targetTag;
        this.radius = radius;
        this.actions = actions;
        this.interval = interval;
    }

    @Override
    public void tick(Player player) {
        if (player.level().isClientSide())
            return;

        timer++;
        if (timer < interval)
            return;
        timer = 0;

        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius.evaluate(player)), e -> {
                    if (e == player)
                        return false;
                    if (targetTag.startsWith("#")) {
                        return e.getType()
                                .is(net.minecraft.tags.TagKey.create(
                                        net.minecraft.core.registries.Registries.ENTITY_TYPE,
                                        new ResourceLocation(targetTag.substring(1))));
                    } else {
                        return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType())
                                .toString().equals(targetTag);
                    }
                });

        for (LivingEntity target : targets) {
            for (ActionRegistry.RaceAction action : actions) {
                action.execute(player, target, null, null);
            }
            // Logic for visual beam would go here (packet to client)
        }
    }

    public static void register() {
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "tether"), json -> {
            String target = json.has("target") ? json.get("target").getAsString() : "minecraft:player";
            mc.sayda.creraces.engine.ScalingValue radius = mc.sayda.creraces.engine.ScalingValue.fromJson(json,
                    "radius", 10.0);
            int interval = json.has("interval") ? json.get("interval").getAsInt() : 20;
            List<ActionRegistry.RaceAction> actions = new ArrayList<>();
            if (json.has("actions")) {
                JsonArray array = json.getAsJsonArray("actions");
                for (int i = 0; i < array.size(); i++) {
                    actions.add(ActionRegistry.fromJson(array.get(i).getAsJsonObject()));
                }
            }
            return new TetherTrait(target, radius, actions, interval);
        });
    }
}
