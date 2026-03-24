package mc.sayda.creraces.engine;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.HashMap;
import java.util.Map;
import mc.sayda.creraces.CreRaces;

public class TraitRegistry {

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

        default void onHit(Player player, LivingEntity target) {
        }

        default boolean onInteraction(Player player, net.minecraft.world.item.ItemStack stack) {
            return false;
        }

        default void onAbilityUse(Player player, mc.sayda.creraces.ability.Ability ability) {
        }

        default void onHurt(Player player, net.minecraft.world.damagesource.DamageSource source, float amount) {
        }

        default float modifyDamageTaken(Player player, net.minecraft.world.damagesource.DamageSource source,
                float amount) {
            return amount;
        }

        default void onDeath(Player player, net.minecraft.world.damagesource.DamageSource source) {
        }

        default void onRespawn(Player player) {
        }

        default void onItemPickup(Player player, net.minecraft.world.item.ItemStack stack) {
        }

        default boolean onBlockInteraction(Player player, net.minecraft.core.BlockPos pos,
                net.minecraft.world.level.block.state.BlockState state) {
            return false;
        }

        default boolean onBlockPlace(Player player, net.minecraft.core.BlockPos pos,
                net.minecraft.world.level.block.state.BlockState state) {
            return false;
        }
    }

    public enum TraitType {
        PASSIVE_TICK,
        ATTRIBUTE_MODIFIER,
        PERMANENT_EFFECT,
        EVENT_HOOK // e.g., on_hit
    }

    public interface TraitFactory {
        RaceTrait create(JsonObject data);
    }

    private static final Map<ResourceLocation, TraitFactory> REGISTRY = new HashMap<>();

    public static void register(ResourceLocation id, TraitFactory factory) {
        REGISTRY.put(id, factory);
    }

    public static RaceTrait fromJson(JsonObject json) {
        return fromJson(json, "");
    }

    public static RaceTrait fromJson(JsonObject json, String defaultId) {
        if (!json.has("type")) {
            return new RaceTrait() {
            };
        }
        String typeStr = json.get("type").getAsString();

        ResourceLocation type = ResourceLocation.tryParse(typeStr);
        if (type == null) {
            CreRaces.LOGGER.error("Malformed trait type '{}' - skipping.", typeStr);
            return new RaceTrait() {
            };
        }
        TraitFactory factory = REGISTRY.get(type);
        if (factory == null) {
            CreRaces.LOGGER.error("Unknown trait type: {}", type);
            return new RaceTrait() {
            };
        }

        RaceTrait trait = factory.create(json);
        if (trait != null) {
            String id = json.has("id") ? json.get("id").getAsString() : defaultId;
            trait.setTraitId(id);
        }
        return trait;
    }

    public static void init() {
        // Attribute Modifiers
        mc.sayda.creraces.engine.traits.AttributeModifierTrait.register();

        // Passive Traits
        mc.sayda.creraces.engine.traits.AddonTrait.register();
        mc.sayda.creraces.engine.traits.PermanentEffectTrait.register();
        mc.sayda.creraces.engine.traits.FlightTrait.register();
        mc.sayda.creraces.engine.traits.ContinuousEffectTrait.register();
        mc.sayda.creraces.engine.traits.DamageMultiplierTrait.register();
        mc.sayda.creraces.engine.traits.BlockInteractionTrait.register();
        mc.sayda.creraces.engine.traits.BlockPlaceTrait.register();
        mc.sayda.creraces.engine.traits.AquaticMovementTrait.register();
        mc.sayda.creraces.engine.traits.OnTickTrait.register();
        mc.sayda.creraces.engine.traits.OnKillTrait.register();
        mc.sayda.creraces.engine.traits.OnHitTrait.register();
        mc.sayda.creraces.engine.traits.OnHurtTrait.register();
        mc.sayda.creraces.engine.traits.ItemInteractionTrait.register();
        mc.sayda.creraces.engine.traits.OnLandTrait.register();
        mc.sayda.creraces.engine.traits.OnRespawnTrait.register();
        mc.sayda.creraces.engine.traits.OnDeathTrait.register();
        mc.sayda.creraces.engine.traits.OnItemPickupTrait.register();
        mc.sayda.creraces.engine.traits.FoodMultiplierTrait.register();
        mc.sayda.creraces.engine.traits.TetherTrait.register();
        mc.sayda.creraces.engine.traits.DomainTrait.register();
        mc.sayda.creraces.engine.traits.OnAbilityUseTrait.register();
    }
}
