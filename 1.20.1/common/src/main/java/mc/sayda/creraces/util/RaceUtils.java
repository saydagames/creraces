package mc.sayda.creraces.util;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.engine.TraitDispatch;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.traits.FoodMultiplierTrait;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class RaceUtils {
    private static final ResourceLocation KITSUNE = new ResourceLocation(CreRaces.MODID, "kitsune");

    /**
     * Food keywords a race's allowed/blocked food lists can use, each standing for the common
     * loader and Farmer's Delight tags that cover it.
     */
    private static final Map<String, List<String>> FOOD_KEYWORD_TAGS = Map.of(
            "vegetable", List.of("#forge:vegetables", "#farmersdelight:vegetables", "#forge:salad_ingredients",
                    "#c:vegetables"),
            "fruit", List.of("#forge:fruits", "#farmersdelight:fruits", "#forge:berries", "#c:fruits"),
            "grain", List.of("#forge:grain", "#forge:grains", "#farmersdelight:grains", "#forge:bread",
                    "#forge:pasta", "#c:grains"),
            "sweet", List.of("#forge:sweets", "#forge:desserts", "#farmersdelight:desserts", "#c:sweets"),
            "dairy", List.of("#forge:dairy", "#forge:milk", "#farmersdelight:milk", "#c:dairy"),
            "seafood", List.of("#minecraft:fishes", "#forge:raw_fishes", "#forge:cooked_fishes",
                    "#farmersdelight:fish", "#c:fishes"),
            "fishes", List.of("#minecraft:fishes", "#forge:raw_fishes", "#forge:cooked_fishes",
                    "#farmersdelight:fish", "#c:fishes"));

    /** The player's race id, or null if their data isn't available. */
    @Nullable
    public static ResourceLocation raceOf(Player player) {
        return DataUtils.getVariables(player).map(IPlayerVariables::getRace).orElse(null);
    }

    /** Torii gates and their waypoints belong to the kitsune and to any race that lists it as a parent. */
    public static boolean isKitsune(Player player) {
        return isRaceOrDescendant(raceOf(player), KITSUNE);
    }

    /**
     * A spirit by race (its JSON sets creraces:is_spirit) or by the per-player is_spirit flag. Being in
     * the spirit realm has nothing to do with it.
     */
    public static boolean isSpirit(Player player) {
        return DataUtils.getVariables(player).map(RaceUtils::isSpirit).orElse(false);
    }

    public static boolean isSpirit(IPlayerVariables vars) {
        if (vars.isSpirit()) return true;
        Race race = RaceRegistry.get(vars.getRace());
        return race != null && race.isSpirit();
    }

    /** True if {@code raceId} is {@code ancestorId} or has it somewhere up its parent chain. */
    public static boolean isRaceOrDescendant(@Nullable ResourceLocation raceId, ResourceLocation ancestorId) {
        if (raceId == null) return false;
        if (raceId.equals(ancestorId)) return true;
        Race race = RaceRegistry.get(raceId);
        if (race == null) return false;
        for (ResourceLocation parentId : race.parentRaces()) {
            if (isRaceOrDescendant(parentId, ancestorId)) return true;
        }
        return false;
    }

    public static double getFoodMultiplier(Player player) {
        return DataUtils.getVariables(player).map(vars -> {
            for (TraitRegistry.RaceTrait trait : TraitDispatch.forPlayer(player)) {
                if (trait instanceof FoodMultiplierTrait fmt) {
                    return fmt.getMultiplier().evaluate(player);
                }
            }
            return 1.0;
        }).orElse(1.0);
    }

    /**
     * Checks if the entity (if it's a player) is immune to the specified potion effect based on
     * their race's negate_effects list.
     */
    @SuppressWarnings("null")
    public static boolean isImmuneToEffect(LivingEntity entity, ResourceLocation effectId) {
        if (!(entity instanceof Player player))
            return false;

        return DataUtils.getVariables(player).map(vars -> {
            Race race = RaceRegistry.get(vars.getRace());
            if (race == null || race.passives() == null)
                return false;

            List<String> negated = race.passives().immuneToPotionEffects();
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
    public static boolean isFoodBlocked(Player player, ItemStack stack) {
        if (stack.isEmpty() || !stack.isEdible())
            return false;

        return DataUtils.getVariables(player).map(vars -> {
            Race race = RaceRegistry.get(vars.getRace());
            if (race == null || race.passives() == null)
                return false;

            List<String> blocked = race.passives().blockedFoodTypes();
            List<String> allowed = race.passives().allowedFoodTypes();

            // A non-empty allowlist blocks everything it doesn't name.
            if (allowed != null && !allowed.isEmpty()
                    && allowed.stream().noneMatch(filter -> stackMatchesFilter(stack, filter))) {
                return true;
            }
            return blocked != null && blocked.stream().anyMatch(filter -> stackMatchesFilter(stack, filter));
        }).orElse(false);
    }

    @SuppressWarnings("null")
    private static boolean stackMatchesFilter(ItemStack stack, String filter) {
        if (stack.isEmpty()) return false;

        // Food keywords first; they accept the same # and creraces: prefixes as ids.
        if (stackMatchesNativeKeyword(stack, filter)) return true;

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) return false;

        if (filter.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(filter.substring(1));
            if (tagId != null) {
                TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
                return stack.is(tag);
            }
        } else if (filter.contains(":")) {
            return filter.equals(itemId.toString());
        } else {
            return filter.equals(itemId.getPath());
        }
        return false;
    }

    private static boolean stackMatchesNativeKeyword(ItemStack stack, String keyword) {
        String stripped = keyword.startsWith("#") ? keyword.substring(1) : keyword;
        if (stripped.startsWith("creraces:")) stripped = stripped.substring(9);

        if (stripped.equalsIgnoreCase("meat")) {
            return stack.getItem().getFoodProperties() != null && stack.getItem().getFoodProperties().isMeat();
        }

        // Tag ids never name a keyword, so this recursion stops one level down.
        for (String tagFilter : FOOD_KEYWORD_TAGS.getOrDefault(stripped.toLowerCase(), List.of())) {
            if (stackMatchesFilter(stack, tagFilter)) return true;
        }
        return false;
    }
}
