package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public class AOEAction implements ActionRegistry.RaceAction {
    public static final ResourceLocation ID = new ResourceLocation(CreRaces.MODID, "aoe");

    private final double radius;
    private final boolean targetPlayers;
    private final boolean targetEnemies;
    private final boolean excludeCaster;
    private final String requiredEffect;
    private final String notEffect;
    private final List<ActionRegistry.RaceAction> actions;

    public AOEAction(double radius, boolean targetPlayers, boolean targetEnemies, boolean excludeCaster,
            String requiredEffect,
            String notEffect, List<ActionRegistry.RaceAction> actions) {
        this.radius = radius;
        this.targetPlayers = targetPlayers;
        this.targetEnemies = targetEnemies;
        this.excludeCaster = excludeCaster;
        this.requiredEffect = requiredEffect;
        this.notEffect = notEffect;
        this.actions = actions;
    }

    @Override
    public boolean execute(@javax.annotation.Nonnull Player player,
            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        if (player.level() == null)
            return true;
        AABB area = player.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(
                LivingEntity.class,
                area,
                e -> {
                    if (excludeCaster && e == player)
                        return false;
                    if (!targetPlayers && e instanceof Player)
                        return false;
                    if (!targetEnemies && !(e instanceof Player))
                        return false;

                    if (requiredEffect != null && !requiredEffect.isEmpty()) {
                        net.minecraft.resources.ResourceLocation effectId = new net.minecraft.resources.ResourceLocation(
                                requiredEffect);
                        net.minecraft.world.effect.MobEffect effect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                                .get(effectId);
                        if (effect != null && !e.hasEffect(effect))
                            return false;
                    }

                    if (notEffect != null && !notEffect.isEmpty()) {
                        net.minecraft.resources.ResourceLocation effectId = new net.minecraft.resources.ResourceLocation(
                                notEffect);
                        net.minecraft.world.effect.MobEffect effect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                                .get(effectId);
                        if (effect != null && e.hasEffect(effect))
                            return false;
                    }

                    return mc.sayda.creraces.team.RaceTeamManager.canHurt(e, player);
                });

        if (targets.isEmpty()) {
            return false;
        }

        for (LivingEntity e : targets) {
            for (ActionRegistry.RaceAction action : actions) {
                if (!action.execute(player, e, slot, interactionPos)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(ID, json -> {
            double radius = GsonHelper.getAsDouble(json, "radius", GsonHelper.getAsDouble(json, "range", 5.0));
            boolean targetPlayers = GsonHelper.getAsBoolean(json, "target_players", true);
            boolean targetEnemies = GsonHelper.getAsBoolean(json, "target_enemies", true);
            boolean excludeCaster = GsonHelper.getAsBoolean(json, "exclude_caster", true);
            String requiredEffect = GsonHelper.getAsString(json, "required_effect", "");
            String notEffect = GsonHelper.getAsString(json, "not_effect", "");
            List<ActionRegistry.RaceAction> actions = new ArrayList<>();
            if (json.has("actions")) {
                JsonArray array = json.getAsJsonArray("actions");
                for (int i = 0; i < array.size(); i++) {
                    actions.add(ActionRegistry.fromJson(array.get(i).getAsJsonObject()));
                }
            }
            return new AOEAction(radius, targetPlayers, targetEnemies, excludeCaster, requiredEffect, notEffect,
                    actions);
        });
    }
}
