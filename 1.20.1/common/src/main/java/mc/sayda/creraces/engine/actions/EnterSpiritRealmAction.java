package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import mc.sayda.creraces.network.BoundaryHandler;

public class EnterSpiritRealmAction implements ActionRegistry.RaceAction {
    public static void register() {
        ActionRegistry.register(new ResourceLocation("creraces", "enter_spirit_realm"), EnterSpiritRealmAction::new);
    }

    private final mc.sayda.creraces.engine.ScalingValue radius;

    public EnterSpiritRealmAction(JsonObject data) {
        this.radius = mc.sayda.creraces.engine.ScalingValue.fromJson(data, "radius", 0.0);
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            if (!vars.isInSpiritRealm()) {
                vars.setInSpiritRealm(true);
                vars.setReturnX(player.getX());
                vars.setReturnY(player.getY());
                vars.setReturnZ(player.getZ());
                vars.setReturnDim(player.level().dimension().location().toString());
            } else {
                vars.setInSpiritRealm(false);
            }

            // Sync to self and trackers (for visibility)
            BoundaryHandler.resyncForAllTrackers(player);
            BoundaryHandler.resyncVariables(player, player);

            // Handle AoE "guiding" if radius is set
            double r = radius.evaluate(player, target, slot);
            if (r > 0) {
                net.minecraft.world.level.Level level = player.level();
                net.minecraft.world.phys.AABB area = player.getBoundingBox().inflate(r);
                boolean enteringRealm = vars.isInSpiritRealm();
                for (Player nearby : level.getEntitiesOfClass(Player.class, area)) {
                    if (nearby == player)
                        continue;
                    DataUtils.getVariables(nearby).ifPresent(nVars -> {
                        if (enteringRealm && !nVars.isInSpiritRealm()) {
                            nVars.setReturnX(nearby.getX());
                            nVars.setReturnY(nearby.getY());
                            nVars.setReturnZ(nearby.getZ());
                            nVars.setReturnDim(nearby.level().dimension().location().toString());
                        }
                        nVars.setInSpiritRealm(enteringRealm);
                        BoundaryHandler.resyncForAllTrackers(nearby);
                        BoundaryHandler.resyncVariables(nearby, nearby);
                    });
                }
            }
        });
        return true;
    }
}
