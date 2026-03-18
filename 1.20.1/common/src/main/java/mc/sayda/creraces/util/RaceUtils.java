package mc.sayda.creraces.util;

import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.traits.FoodMultiplierTrait;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import net.minecraft.world.entity.player.Player;

public class RaceUtils {
    public static double getFoodMultiplier(Player player) {
        return DataUtils.getVariables(player).map(vars -> {
            Race race = RaceRegistry.get(vars.getRace());
            if (race != null && race.traits() != null) {
                for (TraitRegistry.RaceTrait trait : race.traits()) {
                    if (trait instanceof FoodMultiplierTrait fmt) {
                        return fmt.getMultiplier().evaluate(player);
                    }
                }
            }
            return 1.0;
        }).orElse(1.0);
    }

    /**
     * Checks if the entity (if it's a player) is immune to the specified potion
     * effect
     * based on their race's negate_effects list.
     */
    public static boolean isImmuneToEffect(net.minecraft.world.entity.LivingEntity entity,
            net.minecraft.resources.ResourceLocation effectId) {
        if (!(entity instanceof Player player))
            return false;

        return DataUtils.getVariables(player).map(vars -> {
            Race race = RaceRegistry.get(vars.getRace());
            if (race == null || race.passives() == null)
                return false;

            java.util.List<String> negated = race.passives().immuneToPotionEffects();
            if (negated == null || negated.isEmpty())
                return false;

            String idStr = effectId.toString();
            String path = effectId.getPath();
            for (String blocked : negated) {
                if (blocked.equals(idStr) || blocked.equals(path)) {
                    return true;
                }
            }
            return false;
        }).orElse(false);
    }

    /**
     * Checks if the specified food item is blocked for the player's race.
     */
    public static boolean isFoodBlocked(Player player, net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty() || !stack.isEdible())
            return false;

        return DataUtils.getVariables(player).map(vars -> {
            Race race = RaceRegistry.get(vars.getRace());
            if (race == null || race.passives() == null)
                return false;

            java.util.List<String> blocked = race.passives().blockedFoodTypes();
            java.util.List<String> allowed = race.passives().allowedFoodTypes();
            
            // 1. If allowlist is not empty, everything is blocked unless allowed
            if (allowed != null && !allowed.isEmpty()) {
                boolean isAllowed = false;
                for (String filter : allowed) {
                    if (stackMatchesFilter(stack, filter)) {
                        isAllowed = true;
                        break;
                    }
                }
                if (!isAllowed) return true;
            }

            // 2. Blacklist check
            if (blocked != null) {
                for (String filter : blocked) {
                    if (stackMatchesFilter(stack, filter)) {
                        return true;
                    }
                }
            }
            return false;
        }).orElse(false);
    }

    private static boolean stackMatchesFilter(net.minecraft.world.item.ItemStack stack, String filter) {
        if (stack.isEmpty()) return false;
        net.minecraft.world.item.Item item = stack.getItem();
        net.minecraft.resources.ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);

        if (filter.equalsIgnoreCase("meat")) {
            return item.getFoodProperties() != null && item.getFoodProperties().isMeat();
        } else if (filter.startsWith("#")) {
            net.minecraft.resources.ResourceLocation tagId = net.minecraft.resources.ResourceLocation.tryParse(filter.substring(1));
            if (tagId != null) {
                net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, tagId);
                return stack.is(tag);
            }
        } else {
            return filter.equals(itemId.toString()) || filter.equals(itemId.getPath());
        }
        return false;
    }
}
