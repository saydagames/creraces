package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Action to set a boolean identity flag on the player (isSpirit, isSmallBuild, isTiny).
 * JSON: { "type": "creraces:set_flag", "flag": "isSpirit", "value": true }
 */
public class SetFlagAction implements ActionRegistry.RaceAction {

    private final String flag;
    private final boolean value;

    public SetFlagAction(String flag, boolean value) {
        this.flag = flag;
        this.value = value;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target,
            @Nullable mc.sayda.creraces.ability.AbilitySlot slot, @Nullable BlockPos interact_pos) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            switch (flag.toLowerCase()) {
                case "isinspiritrealm", "is_in_spirit_realm", "inspiritrealm" -> {
                    vars.setInSpiritRealm(value);
                    mc.sayda.creraces.network.BoundaryHandler.resyncForAllTrackers(player);
                }
                case "issmallbuild", "is_small_build", "minibuild" -> vars.setSmallBuild(value);
                case "isspirit", "is_spirit", "spirit" -> {
                    vars.setSpirit(value);
                    mc.sayda.creraces.network.BoundaryHandler.resyncForAllTrackers(player);
                }
                case "istiny", "is_tiny", "tiny" -> vars.setTiny(value);
            }
            mc.sayda.creraces.network.BoundaryHandler.resyncVariables(player, player);
        });
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation("creraces", "set_flag"), json -> {
            String flag = GsonHelper.getAsString(json, "flag", "isSpirit");
            boolean value = GsonHelper.getAsBoolean(json, "value", false);
            return new SetFlagAction(flag, value);
        });
    }
}
