package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.EntitySelector;
import mc.sayda.creraces.engine.ItemSlots;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Changes the remaining durability of the item in "slot" (default mainhand): "operation" add
 * (default), set or multiply by "value". The result stays between broken-but-intact and full.
 */
public class DurabilityAction implements ActionRegistry.RaceAction {
    private enum Operation { SET, ADD, MULTIPLY }

    private final ScalingValue value;
    private final String slot;
    private final Operation operation;
    private final EntitySelector entity;

    private DurabilityAction(ScalingValue value, String slot, Operation operation, EntitySelector entity) {
        this.value = value;
        this.slot = slot;
        this.operation = operation;
        this.entity = entity;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot abilitySlot,
            @Nullable BlockPos interactPos) {
        LivingEntity holder = entity.resolve(player, target);
        ItemStack stack = holder != null ? ItemSlots.stack(holder, slot) : ItemStack.EMPTY;
        if (stack.isEmpty() || !stack.isDamageableItem()) {
            return true;
        }
        int maxDamage = stack.getMaxDamage();
        int remaining = maxDamage - stack.getDamageValue();
        double incoming = value.evaluate(player, target, abilitySlot, interactPos);
        double next = switch (operation) {
            case SET -> incoming;
            case ADD -> remaining + incoming;
            case MULTIPLY -> remaining * incoming;
        };
        int clamped = (int) Math.max(0, Math.min(maxDamage, Math.round(next)));
        stack.setDamageValue(maxDamage - clamped);
        return true;
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("durability"), json -> {
            String operationName = GsonHelper.getAsString(json, "operation", "add");
            Operation operation = switch (operationName) {
                case "set" -> Operation.SET;
                case "add" -> Operation.ADD;
                case "multiply" -> Operation.MULTIPLY;
                default -> throw new IllegalArgumentException("unknown operation '" + operationName + "' (set, add, multiply)");
            };
            return new DurabilityAction(ScalingValue.fromJson(json, "value", 1.0),
                    GsonHelper.getAsString(json, "slot", "mainhand"), operation,
                    EntitySelector.fromJson(json, EntitySelector.AUTO));
        });
    }
}
