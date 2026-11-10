package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.DataValue;
import mc.sayda.creraces.engine.EntitySelector;
import mc.sayda.creraces.network.BoundaryHandler;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Stores "value" (a data value: a literal, a scaling value or something read off an entity) as the
 * player's "customization". "operation": "remove" clears it instead.
 */
public class SetCustomizationAction implements ActionRegistry.RaceAction {
    private final String key;
    @Nullable
    private final DataValue value;
    private final EntitySelector entity;

    public SetCustomizationAction(String key, @Nullable DataValue value, EntitySelector entity) {
        this.key = key;
        this.value = value;
        this.entity = entity;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        if (!(entity.resolve(player, target) instanceof Player subject)) {
            return true;
        }
        Object resolved = value != null ? value.resolve(player, target, slot, interactPos) : null;
        if (value != null && resolved == null) {
            return false;
        }
        DataUtils.getVariables(subject).ifPresent(vars -> {
            vars.setCustomization(key, resolved != null ? String.valueOf(resolved) : null);
            BoundaryHandler.resyncVariables(subject, subject);
        });
        return true;
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("set_customization"), json -> {
            String operation = GsonHelper.getAsString(json, "operation", "set");
            if (!operation.equals("set") && !operation.equals("remove")) {
                throw new IllegalArgumentException("unknown operation '" + operation + "' (set, remove)");
            }
            DataValue value = operation.equals("set") ? DataValue.fromJson(json, "value", DataValue.DataType.STRING) : null;
            if (operation.equals("set") && value == null) {
                throw new IllegalArgumentException("needs a \"value\"");
            }
            return new SetCustomizationAction(GsonHelper.getAsString(json, "customization"), value,
                    EntitySelector.fromJson(json, EntitySelector.SELF));
        });
    }
}
