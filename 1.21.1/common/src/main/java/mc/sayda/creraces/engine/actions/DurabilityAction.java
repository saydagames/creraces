package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Generic durability modifier: adds to, sets, multiplies, or fully restores the remaining
 * durability of an item in a slot (ModifyEntityDataAction's Operation enum, reused here so both
 * actions share one vocabulary). A positive "modifier" repairs, a negative one damages. Carries
 * no cost of its own - gate it behind whatever the caller wants (an on_tick interval/condition,
 * a resource-spending action alongside it, etc).
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
    public boolean execute(Player player, @Nullable LivingEntity target,
            @Nullable mc.sayda.creraces.ability.AbilitySlot abilitySlot,
            @Nullable net.minecraft.core.BlockPos interact_pos) {
        LivingEntity actor = (useTarget && target != null) ? target : player;
        ItemStack stack = ItemSlotResolver.getItemInSlot(actor, slot);
        if (stack.isEmpty() || !stack.isDamageableItem()) return true;

        int maxDamage = stack.getMaxDamage();

        if (operation == ModifyEntityDataAction.Operation.REMOVE) {
            stack.setDamageValue(0);
            return true;
        }

        int remaining = maxDamage - stack.getDamageValue();
        double val = modifier.evaluate(player, target, abilitySlot);
        double newRemaining = switch (operation) {
            case SET -> val;
            case MULTIPLY -> remaining * val;
            default -> remaining + val; // ADD
        };

        int clamped = (int) Math.max(0, Math.min(maxDamage, Math.round(newRemaining)));
        stack.setDamageValue(maxDamage - clamped);
        return true;
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "durability"), json -> {
            ScalingValue modifier = ScalingValue.fromJson(json, "modifier", 1.0);
            String slot = GsonHelper.getAsString(json, "slot", "mainhand");
            ModifyEntityDataAction.Operation operation = ModifyEntityDataAction.Operation
                    .fromString(GsonHelper.getAsString(json, "operation", "ADD"));
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            return new DurabilityAction(modifier, slot, operation, useTarget);
        });
    }
}
