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
    @Nullable
    private final String restoreFrom;

    public UnbindAbilityAction(AbilitySlot slot, @Nullable String restoreFrom) {
        this.slot = slot;
        this.restoreFrom = restoreFrom;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot triggerSlot,
            @Nullable BlockPos interactPos) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            ResourceLocation toRestore = null;
            if (restoreFrom != null && !restoreFrom.isEmpty()) {
                String saved = vars.getCustomization(restoreFrom);
                if (saved != null && !saved.isEmpty()) {
                    toRestore = ResourceLocation.tryParse(saved);
                }
                // Consume the saved id so a second unbind can't restore it again.
                vars.setCustomization(restoreFrom, null);
            }
            vars.equipAbility(slot, toRestore);
            vars.sync(player);
        });
        return true;
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "unbind"), json -> new UnbindAbilityAction(
                BindAbilityAction.parseSlot(json, "unbind"),
                GsonHelper.getNullableString(json, "restore_from", null)));
    }
}
