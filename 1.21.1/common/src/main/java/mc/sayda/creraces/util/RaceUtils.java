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
    @SuppressWarnings("null")
    public static boolean isImmuneToEffect(net.minecraft.world.entity.LivingEntity entity,
            net.minecraft.resources.ResourceLocation effectId) {
        if (!(entity instanceof Player player))
            return false;

        return DataUtils.getVariables(player).map(vars -> {
            Race race = RaceRegistry.get(vars.getRace());
            if (race == null || race.passives() == null)
                return false;

            java.util.List<String> negated = race.passives().immuneToPotionEffects();
            if (negated == null)
                return false;
            String idStr = effectId.toString();
            String path = effectId.getPath();
            for (String blocked : negated) {
                if (blocked.equalsIgnoreCase(idStr) || blocked.equalsIgnoreCase(path)
                        || blocked.equalsIgnoreCase("creraces:" + path)) {
                    return true;
                }
            }
            return false;
        }).orElse(false);
    }

    /**
     * Checks if the specified food item is blocked for the player's race.
     */
    @SuppressWarnings("null")
    public static boolean isFoodBlocked(Player player, net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty() || stack.get(net.minecraft.core.component.DataComponents.FOOD) == null)
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

    @SuppressWarnings("null")
    private static boolean stackMatchesFilter(net.minecraft.world.item.ItemStack stack, String filter) {
        if (stack.isEmpty()) return false;

        // Check native keywords first (handles # and creraces: prefix)
        if (stackMatchesNativeKeyword(stack, filter)) return true;

        net.minecraft.world.item.Item item = stack.getItem();
        net.minecraft.resources.ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
        if (itemId == null) return false;

        if (filter.startsWith("#")) {
            net.minecraft.resources.ResourceLocation tagId = net.minecraft.resources.ResourceLocation.tryParse(filter.substring(1));
            if (tagId != null) {
                net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, tagId);
                return stack.is(tag);
            }
        } else if (filter.contains(":")) {
            return filter.equals(itemId.toString());
        } else {
            return filter.equals(itemId.getPath());
        }
        return false;
    }

    private static boolean stackMatchesNativeKeyword(net.minecraft.world.item.ItemStack stack, String keyword) {
        String stripped = keyword.startsWith("#") ? keyword.substring(1) : keyword;
        if (stripped.startsWith("creraces:")) stripped = stripped.substring(9);

        if (stripped.equalsIgnoreCase("meat")) {
            // FoodProperties lost its isMeat flag in 1.21; the vanilla meat item tag is the equivalent.
            return stack.is(net.minecraft.tags.ItemTags.MEAT);
        }
        
        java.util.List<String> tags = new java.util.ArrayList<>();
        switch (stripped.toLowerCase()) {
            case "vegetable" -> {
                tags.add("#forge:vegetables");
                tags.add("#farmersdelight:vegetables");
                tags.add("#forge:salad_ingredients");
                tags.add("#c:vegetables");
            }
            case "fruit" -> {
                tags.add("#forge:fruits");
                tags.add("#farmersdelight:fruits");
                tags.add("#forge:berries");
                tags.add("#c:fruits");
            }
            case "grain" -> {
                tags.add("#forge:grain");
                tags.add("#forge:grains");
                tags.add("#farmersdelight:grains");
                tags.add("#forge:bread");
                tags.add("#forge:pasta");
                tags.add("#c:grains");
            }
            case "sweet" -> {
                tags.add("#forge:sweets");
                tags.add("#forge:desserts");
                tags.add("#farmersdelight:desserts");
                tags.add("#c:sweets");
            }
            case "dairy" -> {
                tags.add("#forge:dairy");
                tags.add("#forge:milk");
                tags.add("#farmersdelight:milk");
                tags.add("#c:dairy");
            }
            case "seafood", "fishes" -> {
                tags.add("#minecraft:fishes");
                tags.add("#forge:raw_fishes");
                tags.add("#forge:cooked_fishes");
                tags.add("#farmersdelight:fish");
                tags.add("#c:fishes");
            }
        }
        
        for (String tagStr : tags) {
            // Recursively call stackMatchesFilter since it handles # tags correctly
            if (stackMatchesFilter(stack, tagStr)) return true;
        }
        return false;
    }
}
