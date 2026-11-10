package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.EntitySelector;
import mc.sayda.creraces.engine.ItemSlots;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Changes an enchantment on the item in "slot" (default mainhand): "operation" set (default) or add
 * "level", or remove it. "enchantment" is an id, or "custom:&lt;key&gt;" to read the id from the
 * caster's customization.
 */
@SuppressWarnings("null")
public class EnchantAction implements ActionRegistry.RaceAction {
    private static final String CUSTOM_PREFIX = "custom:";

    private enum Operation { SET, ADD, REMOVE }

    private final String enchantmentId;
    private final ScalingValue level;
    private final String slot;
    private final Operation operation;
    private final EntitySelector entity;

    private EnchantAction(String enchantmentId, ScalingValue level, String slot, Operation operation,
            EntitySelector entity) {
        this.enchantmentId = enchantmentId;
        this.level = level;
        this.slot = slot;
        this.operation = operation;
        this.entity = entity;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot abilitySlot,
            @Nullable BlockPos interactPos) {
        LivingEntity holder = entity.resolve(player, target);
        ItemStack stack = holder != null ? ItemSlots.stack(holder, slot) : ItemStack.EMPTY;
        if (stack.isEmpty()) {
            return true;
        }

        String idText = enchantmentId;
        if (enchantmentId.startsWith(CUSTOM_PREFIX)) {
            String key = enchantmentId.substring(CUSTOM_PREFIX.length());
            idText = DataUtils.getVariables(player).map(vars -> vars.getCustomization(key)).orElse(null);
            if (idText == null || idText.isEmpty()) {
                return true;
            }
        }
        ResourceLocation id = ResourceLocation.tryParse(idText);
        Enchantment enchantment = id != null ? BuiltInRegistries.ENCHANTMENT.get(id) : null;
        if (enchantment == null) {
            CreRaces.LOGGER.error("EnchantAction: unknown enchantment '{}'", idText);
            return true;
        }

        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        int amount = (int) level.evaluate(player, target, abilitySlot, interactPos);
        switch (operation) {
            case SET -> enchantments.put(enchantment, amount);
            case ADD -> enchantments.put(enchantment, enchantments.getOrDefault(enchantment, 0) + amount);
            case REMOVE -> enchantments.remove(enchantment);
        }
        EnchantmentHelper.setEnchantments(enchantments, stack);
        return true;
    }

    public static void register() {
        ActionRegistry.register(CreRaces.id("enchant"), json -> {
            String operationName = GsonHelper.getAsString(json, "operation", "set");
            Operation operation = switch (operationName) {
                case "set" -> Operation.SET;
                case "add" -> Operation.ADD;
                case "remove" -> Operation.REMOVE;
                default -> throw new IllegalArgumentException("unknown operation '" + operationName + "' (set, add, remove)");
            };
            return new EnchantAction(GsonHelper.getAsString(json, "enchantment"),
                    ScalingValue.fromJson(json, "level", 1.0),
                    GsonHelper.getAsString(json, "slot", "mainhand"),
                    operation,
                    EntitySelector.fromJson(json, EntitySelector.AUTO));
        });
    }
}
