package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class ChangeSizeAction implements ActionRegistry.RaceAction {

    private final mc.sayda.creraces.race.RaceScale scale;
    private final boolean useTarget;

    public ChangeSizeAction(mc.sayda.creraces.race.RaceScale scale, boolean useTarget) {
        this.scale = scale;
        this.useTarget = useTarget;
    }

    @Override
    public boolean execute(Player player, @javax.annotation.Nullable LivingEntity target,
            @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
            @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos) {
        // Prefer target if present, otherwise respect useTarget flag
        LivingEntity entity = (target != null) ? target : (useTarget ? null : player);
        if (entity == null)
            return true;

        mc.sayda.creraces.race.RaceIncidents.applyScale(entity, scale);
        return true;
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "change_size"), json -> {
            mc.sayda.creraces.race.RaceScale scale = mc.sayda.creraces.race.RaceScale.fromJson(json.get("scale"));
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            return new ChangeSizeAction(scale, useTarget);
        });
    }
}
