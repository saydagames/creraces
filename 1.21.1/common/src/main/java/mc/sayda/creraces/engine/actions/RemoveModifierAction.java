package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.EntitySelector;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Removes the attribute modifier an attribute_modifier action added under the id "modifier", from
 * whatever attribute it is on.
 */
@SuppressWarnings("null")
public class RemoveModifierAction implements ActionRegistry.RaceAction {
    private final ResourceLocation modifierId;
    private final EntitySelector entity;

    public RemoveModifierAction(ResourceLocation modifierId, EntitySelector entity) {
        this.modifierId = modifierId;
        this.entity = entity;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        LivingEntity subject = entity.resolve(player, target);
        if (subject == null) {
            return true;
        }
        BuiltInRegistries.ATTRIBUTE.holders().forEach(attribute -> {
            AttributeInstance instance = subject.getAttribute(attribute);
            if (instance != null) {
                instance.removeModifier(modifierId);
            }
        });
        if (subject instanceof Player subjectPlayer) {
            DataUtils.getVariables(subjectPlayer).ifPresent(vars -> vars.removeManagedModifier(modifierId));
        }
        return true;
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("remove_modifier"), json -> new RemoveModifierAction(
                AttributeModifierAction.modifierId(GsonHelper.getAsString(json, "modifier")),
                EntitySelector.fromJson(json, EntitySelector.SELF)));
    }
}
