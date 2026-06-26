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
 * Binds an ability to a specific slot, optionally saving the previous bound ability to a customization key.
 */
public class BindAbilityAction implements ActionRegistry.RaceAction {
    private final AbilitySlot slot;
    private final ResourceLocation abilityId;
    private final String saveTo;

    public BindAbilityAction(AbilitySlot slot, ResourceLocation abilityId, @Nullable String saveTo) {
        this.slot = slot;
        this.abilityId = abilityId;
        this.saveTo = saveTo;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot triggerSlot, @Nullable BlockPos interact_pos) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            if (saveTo != null && !saveTo.isEmpty()) {
                ResourceLocation current = vars.getAbilityInSlot(slot);
                vars.setCustomization(saveTo, current != null ? current.toString() : "");
            }
            vars.equipAbility(slot, abilityId);
            vars.sync(player);
        });
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "bind"), json -> {
            AbilitySlot slot = AbilitySlot.A1;
            if (json.has("slot")) {
                try {
                    slot = AbilitySlot.valueOf(json.get("slot").getAsString().toUpperCase());
                } catch (Exception e) {
                    CreRaces.LOGGER.warn("Invalid slot in bind action: {}", json.get("slot").getAsString());
                }
            }
            ResourceLocation abilityId = new ResourceLocation(GsonHelper.getAsString(json, "id", "minecraft:barrier"));
            String saveTo = GsonHelper.getNullableString(json, "save_to", null);
            return new BindAbilityAction(slot, abilityId, saveTo);
        });
    }
}
