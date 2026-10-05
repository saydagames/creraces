package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.TargetFilter;
import mc.sayda.creraces.race.RaceIncidents;
import mc.sayda.creraces.race.RaceScale;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/** Applies a race scale (size) to the target, or to the caster when there is none and use_target is off. */
public class ChangeSizeAction implements ActionRegistry.RaceAction {

    private final RaceScale scale;
    private final boolean useTarget;

    public ChangeSizeAction(RaceScale scale, boolean useTarget) {
        this.scale = scale;
        this.useTarget = useTarget;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        LivingEntity entity = TargetFilter.resolveSmartTarget(player, target, useTarget);
        if (entity != null) {
            RaceIncidents.applyScale(entity, scale);
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "change_size"), json -> new ChangeSizeAction(
                RaceScale.fromJson(json.get("scale")),
                GsonHelper.getAsBoolean(json, "use_target", false)));
    }
}
