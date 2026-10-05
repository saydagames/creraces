package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Adds to, sets, multiplies or fully restores (REMOVE) the remaining durability of the item in a
 * slot, reusing ModifyEntityDataAction's operations. A positive modifier repairs, a negative one
 * damages. It has no cost of its own; gate it with conditions or pair it with a cost action.
 */
public class DurabilityAction implements ActionRegistry.RaceAction {
    private final ScalingValue modifier;
    private final String slot;
    private final ModifyEntityDataAction.Operation operation;
    private final boolean useTarget;

    public DurabilityAction(ScalingValue modifier, String slot, ModifyEntityDataAction.Operation operation,
            boolean useTarget) {
        this.modifier = modifier;
        this.slot = slot;
        this.operation = operation;
        this.useTarget = useTarget;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot abilitySlot,
            @Nullable BlockPos interactPos) {
        LivingEntity holder = (useTarget && target != null) ? target : player;
        ItemStack stack = ItemSlotResolver.getItemInSlot(holder, slot);
        if (stack.isEmpty() || !stack.isDamageableItem()) {
            return true;
        }
        if (operation == ModifyEntityDataAction.Operation.REMOVE) {
            stack.setDamageValue(0);
            return true;
        }

        int maxDamage = stack.getMaxDamage();
        int remaining = maxDamage - stack.getDamageValue();
        double value = modifier.evaluate(player, target, abilitySlot);
        double newRemaining = switch (operation) {
            case SET -> value;
            case MULTIPLY -> remaining * value;
            default -> remaining + value;
        };
        int clamped = (int) Math.max(0, Math.min(maxDamage, Math.round(newRemaining)));
        stack.setDamageValue(maxDamage - clamped);
        return true;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "durability"), json -> new DurabilityAction(
                ScalingValue.fromJson(json, "modifier", 1.0),
                GsonHelper.getAsString(json, "slot", "mainhand"),
                ModifyEntityDataAction.Operation.fromString(GsonHelper.getAsString(json, "operation", "ADD")),
                GsonHelper.getAsBoolean(json, "use_target", false)));
    }
}
