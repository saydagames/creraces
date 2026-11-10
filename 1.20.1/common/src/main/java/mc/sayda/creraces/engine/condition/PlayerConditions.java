package mc.sayda.creraces.engine.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.engine.BiomeChecker;
import mc.sayda.creraces.engine.EntitySelector;
import mc.sayda.creraces.race.Race;
import mc.sayda.creraces.race.RaceRegistry;
import mc.sayda.creraces.territory.FactionLeaderManager;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.creraces.util.RaceUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Conditions about a player's race: which race, customizations, territory and faction. They read the
 * caster unless "entity" is "target", and are false for an entity that is not a player. Stored
 * numbers (states, cooldowns, resources, ability levels) are checked with compare.
 */
@SuppressWarnings("null")
final class PlayerConditions {
    private PlayerConditions() {
    }

    /** One check against the selected player's variables. */
    @FunctionalInterface
    interface PlayerTest {
        boolean test(Player player, IPlayerVariables vars);
    }

    private static void register(String id, Function<JsonObject, PlayerTest> parser) {
        EntityConditions.register(id, EntitySelector.SELF, json -> {
            PlayerTest test = parser.apply(json);
            return (entity, caster, target, slot, pos) -> entity instanceof Player player
                    && DataUtils.getVariables(player).map(vars -> test.test(player, vars)).orElse(false);
        });
    }

    static void register() {
        register("race", json -> {
            List<ResourceLocation> races = new ArrayList<>();
            for (JsonElement element : json.getAsJsonArray("races")) {
                ResourceLocation raceId = ResourceLocation.tryParse(element.getAsString());
                if (raceId == null) {
                    throw new IllegalArgumentException("malformed race id '" + element.getAsString() + "'");
                }
                races.add(raceId);
            }
            return (player, vars) -> races.stream().anyMatch(wanted -> RaceUtils.matchesRace(vars.getRace(), wanted));
        });

        // With "values", the customization is one of them; without, it is set to anything but empty or 0.
        register("customization", json -> {
            String customization = GsonHelper.getAsString(json, "customization");
            List<String> allowed = new ArrayList<>();
            if (json.has("values")) {
                json.getAsJsonArray("values").forEach(element -> allowed.add(element.getAsString()));
            }
            return (player, vars) -> {
                String value = customizationValue(vars, customization);
                if (allowed.isEmpty()) {
                    return value != null && !value.isEmpty() && !value.equals("0") && !value.equals("0.0");
                }
                return allowed.contains(value);
            };
        });

        register("in_habitable_biome", json -> (player, vars) -> {
            Race race = RaceRegistry.get(vars.getRace());
            if (race == null || !race.enableTerritory()) {
                return false;
            }
            List<String> biomes = race.claimValidBiomes();
            return biomes.isEmpty() || BiomeChecker.matches(player.level().getBiome(player.blockPosition()), biomes);
        });

        register("has_faction", json -> (player, vars) -> {
            Race race = RaceRegistry.get(vars.getRace());
            return race != null && race.factionGroup() != null;
        });

        register("faction_leader", json -> (player, vars) -> FactionLeaderManager.isLeader(player));
    }

    /** The stored customization, or the race's default for it when the player never chose one. */
    @Nullable
    private static String customizationValue(IPlayerVariables vars, String id) {
        String value = vars.getCustomization(id);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        Race race = RaceRegistry.get(vars.getRace());
        if (race == null) {
            return value;
        }
        return race.customization().stream()
                .filter(customization -> customization.id().equals(id))
                .map(customization -> customization.getDefaultValue(race.id()))
                .findFirst()
                .orElse(value);
    }
}
