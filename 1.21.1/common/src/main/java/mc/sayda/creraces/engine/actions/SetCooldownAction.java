package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/** Sets the remaining cooldown (in ticks) of an ability, given by "state" or "id". */
public class SetCooldownAction implements ActionRegistry.RaceAction {
    private final ResourceLocation abilityId;
    private final ScalingValue value;

    public SetCooldownAction(ResourceLocation abilityId, ScalingValue value) {
        this.abilityId = abilityId;
        this.value = value;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            vars.setCooldown(abilityId, (int) Math.max(0, value.evaluate(player, target, slot)));
            BoundaryHandler.resyncVariables(player, player);
        });
        return true;
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "set_cooldown"), json -> {
            String id = json.has("state") ? GsonHelper.getAsString(json, "state") : GsonHelper.getAsString(json, "id");
            if (!id.contains(":")) {
                id = CreRaces.MODID + ":" + id;
            }
            return new SetCooldownAction(ResourceLocation.parse(id), ScalingValue.fromJson(json, "value", 100.0));
        });
    }
}
