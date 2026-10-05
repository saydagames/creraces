package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import javax.annotation.Nullable;

/**
 * Adds levels to, sets or removes an enchantment on the item in a slot. The enchantment may be
 * given as "custom:&lt;key&gt;" to read its id from a customization variable.
 */
public class EnchantAction implements ActionRegistry.RaceAction {
    private static final String CUSTOM_PREFIX = "custom:";

    private final String enchantmentId;
    private final ScalingValue level;
    private final String slot;
    private final String mode;
    private final boolean useTarget;

    public EnchantAction(String enchantmentId, ScalingValue level, String slot, String mode, boolean useTarget) {
        this.enchantmentId = enchantmentId;
        this.level = level;
        this.slot = slot;
        this.mode = mode;
        this.useTarget = useTarget;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot abilitySlot,
            @Nullable BlockPos interactPos) {
        LivingEntity holder = (useTarget && target != null) ? target : player;
        ItemStack stack = ItemSlotResolver.getItemInSlot(holder, slot);
        if (stack.isEmpty()) {
            return true;
        }

        String idString = enchantmentId;
        if (enchantmentId.startsWith(CUSTOM_PREFIX)) {
            String key = enchantmentId.substring(CUSTOM_PREFIX.length());
            idString = DataUtils.getVariables(player).map(vars -> vars.getCustomization(key)).orElse(null);
            if (idString == null || idString.isEmpty()) {
                return true;
            }
        }
        ResourceLocation id = ResourceLocation.tryParse(idString);
        if (id == null) {
            CreRaces.LOGGER.error("Malformed enchantment ID: {}", idString);
            return true;
        }

        // Enchantments live in a datapack registry in 1.21, so they resolve off the level.
        Holder<Enchantment> enchantment = player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(id).orElse(null);
        if (enchantment == null) {
            CreRaces.LOGGER.error("Unknown enchantment ID: {}", id);
            return true;
        }

        int targetLevel = (int) level.evaluate(player, target, abilitySlot);
        EnchantmentHelper.updateEnchantments(stack, enchants -> {
            if (mode.equalsIgnoreCase("ADD")) {
                enchants.set(enchantment, enchants.getLevel(enchantment) + targetLevel);
            } else if (mode.equalsIgnoreCase("SET")) {
                enchants.set(enchantment, targetLevel);
            } else if (mode.equalsIgnoreCase("REMOVE")) {
                enchants.removeIf(e -> e.equals(enchantment));
            }
        });
        return true;
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "enchant"), json -> new EnchantAction(
                GsonHelper.getAsString(json, "enchantment"),
                ScalingValue.fromJson(json, "level", 1.0),
                GsonHelper.getAsString(json, "slot", "mainhand"),
                GsonHelper.getAsString(json, "mode", "SET"),
                GsonHelper.getAsBoolean(json, "use_target", false)));
    }
}
