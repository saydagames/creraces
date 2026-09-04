package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Unbinds an ability from a specific slot, optionally restoring a previously saved ability from a customization key.
 */
public class UnbindAbilityAction implements ActionRegistry.RaceAction {
    private final AbilitySlot slot;
    private final String restoreFrom;

    public UnbindAbilityAction(AbilitySlot slot, @Nullable String restoreFrom) {
        this.slot = slot;
        this.restoreFrom = restoreFrom;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot triggerSlot, @Nullable BlockPos interact_pos) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            ResourceLocation toRestore = null;
            if (restoreFrom != null && !restoreFrom.isEmpty()) {
                String val = vars.getCustomization(restoreFrom);
                if (val != null && !val.isEmpty()) {
                    toRestore = ResourceLocation.tryParse(val);
                }
                // Clear the restore key after use to prevent double restoration
                vars.setCustomization(restoreFrom, null);
            }
            vars.equipAbility(slot, toRestore);
            vars.sync(player);
        });
        return true;
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "unbind"), json -> {
            AbilitySlot slot = AbilitySlot.A1;
            if (json.has("slot")) {
                try {
                    slot = AbilitySlot.valueOf(json.get("slot").getAsString().toUpperCase());
                } catch (Exception e) {
                    CreRaces.LOGGER.warn("Invalid slot in unbind action: {}", json.get("slot").getAsString());
                }
            }
            String restoreFrom = GsonHelper.getNullableString(json, "restore_from", null);
            return new UnbindAbilityAction(slot, restoreFrom);
        });
    }
}
