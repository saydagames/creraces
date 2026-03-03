package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;

public class RemoveEffectAction implements ActionRegistry.RaceAction {

    private final MobEffect effect;
    private final ScalingValue radius;
    private final boolean targetEnemies;
    private final boolean targetPlayers;

    public RemoveEffectAction(MobEffect effect, ScalingValue radius, boolean targetEnemies, boolean targetPlayers) {
        this.effect = effect;
        this.radius = radius;
        this.targetEnemies = targetEnemies;
        this.targetPlayers = targetPlayers;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interactionPos) {
        if (effect == null)
            return true;

        java.util.function.Consumer<net.minecraft.world.entity.LivingEntity> remove = e -> {
            e.removeEffect(effect);
        };

        double r = radius.evaluate(player, target);
        if (r > 0) {
            net.minecraft.world.phys.AABB area = player.getBoundingBox().inflate(r);
            java.util.List<net.minecraft.world.entity.LivingEntity> targets = player.level().getEntitiesOfClass(
                    net.minecraft.world.entity.LivingEntity.class,
                    area,
                    entity -> {
                        if (entity == player)
                            return !targetEnemies; // Caster is not an enemy
                        if (!targetPlayers && entity instanceof Player)
                            return false;
                        if (targetEnemies && entity instanceof net.minecraft.world.entity.Mob)
                            return true;
                        return targetPlayers && entity instanceof Player;
                    });

            for (net.minecraft.world.entity.LivingEntity e : targets) {
                remove.accept(e);
            }
        } else if (target != null && targetEnemies) {
            remove.accept(target);
        } else {
            remove.accept(player);
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "remove_effect"), json -> {
            String effectId = GsonHelper.getAsString(json, "effect");
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(new ResourceLocation(effectId));
            ScalingValue radius = ScalingValue.fromJson(json, "radius", 0.0);
            boolean targetEnemies = GsonHelper.getAsBoolean(json, "target_enemies", false);
            boolean targetPlayers = GsonHelper.getAsBoolean(json, "target_players", false);

            return new RemoveEffectAction(effect, radius, targetEnemies, targetPlayers);
        });
    }
}
