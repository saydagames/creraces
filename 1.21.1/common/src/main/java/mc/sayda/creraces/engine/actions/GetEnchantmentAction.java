package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import javax.annotation.Nullable;

/**
 * Reads an enchantment from the item in a slot (a given one, or else the first it has) and stores
 * its id in a customization variable and its level in a persistent state.
 */
@SuppressWarnings("null")
public class GetEnchantmentAction implements ActionRegistry.RaceAction {
    private static final String STATE_PREFIX = "state:";
    private static final String SELF_PREFIX = "self:";

    /** Enchantment to look for; empty takes the first one on the item. */
    private final String enchantmentId;
    private final String slot;
    /** Customization key that receives the enchantment id. */
    private final String saveIdTo;
    /** State key that receives the level: "state:self" (the casting ability), "state:&lt;id&gt;" or a plain id. */
    private final String saveLevelTo;
    private final boolean useTarget;

    public GetEnchantmentAction(String enchantmentId, String slot, String saveIdTo, String saveLevelTo,
            boolean useTarget) {
        this.enchantmentId = enchantmentId;
        this.slot = slot;
        this.saveIdTo = saveIdTo;
        this.saveLevelTo = saveLevelTo;
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

        ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        Holder<Enchantment> found = null;
        if (!enchantmentId.isEmpty()) {
            ResourceLocation wantedId = ResourceLocation.tryParse(enchantmentId);
            if (wantedId != null) {
                for (Holder<Enchantment> enchantment : enchants.keySet()) {
                    if (enchantment.is(wantedId)) {
                        found = enchantment;
                        break;
                    }
                }
            }
        } else if (!enchants.isEmpty()) {
            found = enchants.keySet().iterator().next();
        }
        if (found == null) {
            return true;
        }

        int level = enchants.getLevel(found);
        ResourceLocation foundId = found.unwrapKey().map(ResourceKey::location).orElse(null);
        if (foundId != null) {
            store(player, foundId, level, abilitySlot);
        }
        return true;
    }

    private void store(Player player, ResourceLocation enchantment, int level, @Nullable AbilitySlot abilitySlot) {
        DataUtils.getVariables(player).ifPresent(vars -> {
            if (!saveIdTo.isEmpty()) {
                vars.setCustomization(saveIdTo, enchantment.toString());
            }
            if (!saveLevelTo.isEmpty()) {
                ResourceLocation stateId = resolveStateKey(saveLevelTo, vars, abilitySlot);
                if (stateId != null) {
                    vars.setPersistentState(stateId, (double) level);
                }
            }
            vars.sync(player);
        });
    }

    @Nullable
    private static ResourceLocation resolveStateKey(String key, IPlayerVariables vars, @Nullable AbilitySlot slot) {
        String sub = key.startsWith(STATE_PREFIX) ? key.substring(STATE_PREFIX.length()) : key;
        if (key.startsWith(STATE_PREFIX) && sub.startsWith("self")) {
            ResourceLocation abilityId = slot != null ? vars.getAbilityInSlot(slot) : null;
            if (abilityId != null) {
                return abilityId;
            }
            // No ability to use: "self:<id>" falls back to <id> when a slot was given, anything else to "current".
            sub = sub.startsWith(SELF_PREFIX) && slot != null ? sub.substring(SELF_PREFIX.length()) : "current";
        }
        if (!sub.contains(":")) {
            sub = CreRaces.MODID + ":" + sub;
        }
        return ResourceLocation.tryParse(sub);
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "get_enchantment"),
                json -> new GetEnchantmentAction(
                        GsonHelper.getAsString(json, "enchantment", ""),
                        GsonHelper.getAsString(json, "slot", "mainhand"),
                        GsonHelper.getAsString(json, "save_id_to", ""),
                        GsonHelper.getAsString(json, "save_level_to", ""),
                        GsonHelper.getAsBoolean(json, "use_target", false)));
    }
}
