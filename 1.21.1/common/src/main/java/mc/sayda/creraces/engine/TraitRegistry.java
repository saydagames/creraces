package mc.sayda.creraces.engine;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.Ability;
import mc.sayda.creraces.engine.traits.AddonTrait;
import mc.sayda.creraces.engine.traits.AquaticMovementTrait;
import mc.sayda.creraces.engine.traits.AttributeModifierTrait;
import mc.sayda.creraces.engine.traits.BlockInteractionTrait;
import mc.sayda.creraces.engine.traits.BlockPlaceTrait;
import mc.sayda.creraces.engine.traits.DamageMultiplierTrait;
import mc.sayda.creraces.engine.traits.FlightTrait;
import mc.sayda.creraces.engine.traits.FoodMultiplierTrait;
import mc.sayda.creraces.engine.traits.ItemInteractionTrait;
import mc.sayda.creraces.engine.traits.KeepEffectTrait;
import mc.sayda.creraces.engine.traits.OnAbilityUseTrait;
import mc.sayda.creraces.engine.traits.OnDeathTrait;
import mc.sayda.creraces.engine.traits.OnHitTrait;
import mc.sayda.creraces.engine.traits.OnHurtTrait;
import mc.sayda.creraces.engine.traits.OnItemPickupTrait;
import mc.sayda.creraces.engine.traits.OnKillTrait;
import mc.sayda.creraces.engine.traits.OnLandTrait;
import mc.sayda.creraces.engine.traits.OnRespawnTrait;
import mc.sayda.creraces.engine.traits.OnSelectTrait;
import mc.sayda.creraces.engine.traits.OnTickTrait;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

public class TraitRegistry {

    /** Every hook defaults to doing nothing, so a trait only overrides the events it reacts to. */
    public interface RaceTrait {
        default void setTraitId(String id) {
        }

        default String getTraitId() {
            return "";
        }

        default void tick(Player player) {
        }

        default void onKill(Player player, LivingEntity target) {
        }

        default void onHit(Player player, LivingEntity target, DamageSource source, float amount) {
        }

        default boolean onInteraction(Player player, ItemStack stack) {
            return false;
        }

        default void onAbilityUse(Player player, Ability ability) {
        }

        default void onHurt(Player player, DamageSource source, float amount) {
        }

        default float modifyDamageTaken(Player player, DamageSource source, float amount) {
            return amount;
        }

        default void onDeath(Player player, DamageSource source) {
        }

        default void onRespawn(Player player) {
        }

        default void onSelect(Player player) {
        }

        default void onItemPickup(Player player, ItemStack stack) {
        }

        default boolean onBlockInteraction(Player player, BlockPos pos, BlockState state) {
            return false;
        }

        default boolean onBlockPlace(Player player, BlockPos pos, BlockState state) {
            return false;
        }
    }

    public interface TraitFactory {
        RaceTrait create(JsonObject data);
    }

    private static final Map<ResourceLocation, TraitFactory> REGISTRY = new HashMap<>();

    /** Stands in for a trait entry that has no type or could not be resolved. */
    private static final RaceTrait NO_OP_TRAIT = new RaceTrait() {
    };

    public static void register(ResourceLocation id, TraitFactory factory) {
        REGISTRY.put(id, factory);
    }

    /**
     * Builds a trait. {@code defaultId} names it when the JSON has no "id" of its own; it is where the
     * trait sits in its file (e.g. "passive/2"), so it stays stable as long as the list isn't reordered.
     */
    public static RaceTrait fromJson(JsonObject json, String defaultId) {
        if (!json.has("type")) {
            return NO_OP_TRAIT;
        }
        String typeStr = json.get("type").getAsString();
        ResourceLocation type = ResourceLocation.tryParse(typeStr);
        if (type == null) {
            CreRaces.LOGGER.error("Malformed trait type '{}' - skipping.", typeStr);
            return NO_OP_TRAIT;
        }
        TraitFactory factory = REGISTRY.get(type);
        if (factory == null) {
            CreRaces.LOGGER.error("Unknown trait type: {}", type);
            return NO_OP_TRAIT;
        }

        JsonObject withId = json;
        if (!json.has("id")) {
            withId = json.deepCopy();
            withId.addProperty("id", defaultId.toLowerCase().replaceAll("[^a-z0-9/._-]", "/"));
        }
        try {
            RaceTrait trait = factory.create(withId);
            if (trait == null) {
                CreRaces.LOGGER.error("Trait factory for {} returned null - skipping.", type);
                return NO_OP_TRAIT;
            }
            trait.setTraitId(withId.get("id").getAsString());
            return trait;
        } catch (Exception e) {
            // Skip only this trait; letting it throw would make RaceManager drop the whole race.
            CreRaces.LOGGER.error("Failed to parse trait '{}': {} - skipping it. JSON: {}", type, e.getMessage(), json);
            return NO_OP_TRAIT;
        }
    }

    public static void init() {
        AddonTrait.register();
        FlightTrait.register();
        DamageMultiplierTrait.register();
        BlockInteractionTrait.register();
        BlockPlaceTrait.register();
        AquaticMovementTrait.register();
        OnTickTrait.register();
        OnKillTrait.register();
        OnHitTrait.register();
        OnHurtTrait.register();
        ItemInteractionTrait.register();
        OnLandTrait.register();
        OnRespawnTrait.register();
        OnSelectTrait.register();
        OnDeathTrait.register();
        OnItemPickupTrait.register();
        FoodMultiplierTrait.register();
        OnAbilityUseTrait.register();
        AttributeModifierTrait.register();
        KeepEffectTrait.register();
    }
}
