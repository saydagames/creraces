package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Action that sets the cooldown for a specific ability.
 */
public class SetCooldownAction implements ActionRegistry.RaceAction {
    private final ResourceLocation abilityId;
    private final int value;

    public SetCooldownAction(ResourceLocation abilityId, int value) {
        this.abilityId = abilityId;
        this.value = value;
    }

    @Override
    public boolean execute(Player player, @Nullable net.minecraft.world.entity.LivingEntity target,
            @Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @Nullable net.minecraft.core.BlockPos interactionPos) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            vars.setCooldown(abilityId, Math.max(0, value));
            mc.sayda.creraces.network.BoundaryHandler.resyncVariables(player, player);
        });
        return false;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "set_cooldown"), json -> {
            ResourceLocation ability = new ResourceLocation(GsonHelper.getAsString(json, "ability"));
            int val = GsonHelper.getAsInt(json, "value", 0);
            return new SetCooldownAction(ability, val);
        });
    }
}
