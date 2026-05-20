package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Action that applies, sets, or removes an enchantment from an item in a specific slot.
 */
public class EnchantAction implements ActionRegistry.RaceAction {
    private final String enchantmentId;
    private final ScalingValue level;
    private final String slot;
    private final String mode; // ADD, SET, REMOVE
    private final boolean useTarget;

    public EnchantAction(String enchantmentId, ScalingValue level, String slot, String mode, boolean useTarget) {
        this.enchantmentId = enchantmentId;
        this.level = level;
        this.slot = slot;
        this.mode = mode;
        this.useTarget = useTarget;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target,
            @Nullable mc.sayda.creraces.ability.AbilitySlot abilitySlot,
            @Nullable net.minecraft.core.BlockPos interact_pos) {
        
        LivingEntity actor = (useTarget && target != null) ? target : player;
        ItemStack stack = getItemInSlot(actor, slot);

        if (stack.isEmpty()) return true;

        String actualIdStr = enchantmentId;
        if (enchantmentId.startsWith("custom:")) {
            String key = enchantmentId.substring(7);
            actualIdStr = mc.sayda.creraces.capability.DataUtils.getVariables(player)
                .map(vars -> vars.getCustomization(key))
                .orElse(null);
            if (actualIdStr == null || actualIdStr.isEmpty()) {
                return true;
            }
        }

        ResourceLocation id = ResourceLocation.tryParse(actualIdStr);
        if (id == null) {
            CreRaces.LOGGER.error("Malformed enchantment ID: {}", actualIdStr);
            return true;
        }

        @SuppressWarnings("null")
        Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.get(id);
        if (enchantment == null) {
            CreRaces.LOGGER.error("Unknown enchantment ID: {}", id);
            return true;
        }

        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
        int currentLevel = enchants.getOrDefault(enchantment, 0);
        int targetLevel = (int) level.evaluate(player, target, abilitySlot);

        if (mode.equalsIgnoreCase("ADD")) {
            enchants.put(enchantment, currentLevel + targetLevel);
        } else if (mode.equalsIgnoreCase("SET")) {
            enchants.put(enchantment, targetLevel);
        } else if (mode.equalsIgnoreCase("REMOVE")) {
            enchants.remove(enchantment);
        }

        EnchantmentHelper.setEnchantments(enchants, stack);
        return true;
    }

    private ItemStack getItemInSlot(LivingEntity entity, String slot) {
        if (slot.equalsIgnoreCase("mainhand")) return entity.getMainHandItem();
        if (slot.equalsIgnoreCase("offhand")) return entity.getOffhandItem();
        if (slot.equalsIgnoreCase("head")) return entity.getItemBySlot(EquipmentSlot.HEAD);
        if (slot.equalsIgnoreCase("chest")) return entity.getItemBySlot(EquipmentSlot.CHEST);
        if (slot.equalsIgnoreCase("legs")) return entity.getItemBySlot(EquipmentSlot.LEGS);
        if (slot.equalsIgnoreCase("feet")) return entity.getItemBySlot(EquipmentSlot.FEET);

        if (entity instanceof Player player) {
            try {
                int index = Integer.parseInt(slot);
                if (index >= 0 && index < player.getInventory().getContainerSize()) {
                    return player.getInventory().getItem(index);
                }
            } catch (NumberFormatException ignored) {}
        }

        return ItemStack.EMPTY;
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "enchant"), json -> {
            String enchIdStr = GsonHelper.getAsString(json, "enchantment");
            ScalingValue level = ScalingValue.fromJson(json, "level", 1.0);
            String slot = GsonHelper.getAsString(json, "slot", "mainhand");
            String mode = GsonHelper.getAsString(json, "mode", "SET");
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            return new EnchantAction(enchIdStr, level, slot, mode, useTarget);
        });
    }
}
