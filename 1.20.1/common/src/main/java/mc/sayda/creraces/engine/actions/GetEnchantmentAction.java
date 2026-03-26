package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
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
 * Action that finds an enchantment on an item and saves its ID and Level to variables.
 */
@SuppressWarnings("null")
public class GetEnchantmentAction implements ActionRegistry.RaceAction {
    private final String enchantmentId; // Optional: specify which one to grab
    private final String slot;
    private final String saveIdTo;     // customization key
    private final String saveLevelTo;  // state key (ResourceLocation string)
    private final boolean useTarget;

    public GetEnchantmentAction(String enchantmentId, String slot, String saveIdTo, String saveLevelTo, boolean useTarget) {
        this.enchantmentId = enchantmentId;
        this.slot = slot;
        this.saveIdTo = saveIdTo;
        this.saveLevelTo = saveLevelTo;
        this.useTarget = useTarget;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target,
            @Nullable mc.sayda.creraces.ability.AbilitySlot abilitySlot,
            @Nullable net.minecraft.core.BlockPos interactionPos) {
        
        LivingEntity actor = (useTarget && target != null) ? target : player;
        ItemStack stack = getItemInSlot(actor, slot);

        if (stack.isEmpty()) return true;

        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
        Enchantment resultEnch = null;
        int level = 0;

        if (enchantmentId != null && !enchantmentId.isEmpty()) {
            ResourceLocation targetId = ResourceLocation.tryParse(enchantmentId);
            if (targetId != null) {
                @SuppressWarnings("null")
                Enchantment targetEnch = BuiltInRegistries.ENCHANTMENT.get(targetId);
                if (targetEnch != null && enchants.containsKey(targetEnch)) {
                    resultEnch = targetEnch;
                    level = enchants.get(targetEnch);
                }
            }
        } else {
            // Grab the first one
            for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                resultEnch = entry.getKey();
                level = entry.getValue();
                break;
            }
        }

        if (resultEnch != null) {
            ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(resultEnch);
            if (id != null) {
                @SuppressWarnings("null")
                final String idStr = id.toString();
                final int finalLevel = level;
                
                mc.sayda.creraces.capability.DataUtils.getVariables(player).ifPresent(vars -> {
                    if (saveIdTo != null && !saveIdTo.isEmpty()) {
                        vars.setCustomization(saveIdTo, idStr);
                    }
                    if (saveLevelTo != null && !saveLevelTo.isEmpty()) {
                        ResourceLocation levelLoc = resolveStateKey(saveLevelTo, player, vars, abilitySlot);
                        if (levelLoc != null) {
                            vars.setPersistentState(levelLoc, (double) finalLevel);
                        }
                    }
                    vars.sync(player);
                });
            }
        }

        return true;
    }

    private ResourceLocation resolveStateKey(String key, Player player, mc.sayda.creraces.capability.IPlayerVariables vars, @Nullable mc.sayda.creraces.ability.AbilitySlot slot) {
        if (key.startsWith("state:")) {
            String sub = key.substring(6);
            if (sub.startsWith("self:") && slot != null) {
                ResourceLocation abilityId = vars.getAbilityInSlot(slot);
                if (abilityId != null) {
                    return abilityId;
                }
                sub = sub.substring(5);
            } else if (sub.startsWith("self")) {
                ResourceLocation abilityId = vars.getAbilityInSlot(slot);
                if (abilityId != null) {
                    return abilityId;
                }
                sub = "current"; // fallback
            }
            if (!sub.contains(":")) sub = "creraces:" + sub;
            return ResourceLocation.tryParse(sub);
        }
        if (!key.contains(":")) key = "creraces:" + key;
        return ResourceLocation.tryParse(key);
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
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "get_enchantment"), json -> {
            String enchantmentId = GsonHelper.getAsString(json, "enchantment", "");
            String slot = GsonHelper.getAsString(json, "slot", "mainhand");
            String saveIdTo = GsonHelper.getAsString(json, "save_id_to", "");
            String saveLevelTo = GsonHelper.getAsString(json, "save_level_to", "");
            boolean useTarget = GsonHelper.getAsBoolean(json, "use_target", false);
            return new GetEnchantmentAction(enchantmentId, slot, saveIdTo, saveLevelTo, useTarget);
        });
    }
}
