package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
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
 * Binds an ability to a specific slot, optionally saving the previously bound ability to a customization key.
 */
public class BindAbilityAction implements ActionRegistry.RaceAction {
    private final AbilitySlot slot;
    private final ResourceLocation abilityId;
    @Nullable
    private final String saveTo;

    public BindAbilityAction(AbilitySlot slot, ResourceLocation abilityId, @Nullable String saveTo) {
        this.slot = slot;
        this.abilityId = abilityId;
        this.saveTo = saveTo;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot triggerSlot,
            @Nullable BlockPos interactPos) {
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

    /** Reads the "slot" field shared by bind and unbind; missing or invalid values fall back to A1. */
    static AbilitySlot parseSlot(JsonObject json, String actionName) {
        if (!json.has("slot")) {
            return AbilitySlot.A1;
        }
        String name = json.get("slot").getAsString();
        try {
            return AbilitySlot.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            CreRaces.LOGGER.warn("Invalid slot in {} action: {}", actionName, name);
            return AbilitySlot.A1;
        }
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "bind"), json -> {
            ResourceLocation abilityId = ResourceLocation.parse(
                    GsonHelper.getAsString(json, "id", "minecraft:barrier"));
            return new BindAbilityAction(parseSlot(json, "bind"), abilityId,
                    GsonHelper.getNullableString(json, "save_to", null));
        });
    }
}
