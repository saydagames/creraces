package mc.sayda.creraces.engine;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import java.util.HashMap;
import java.util.Map;
import mc.sayda.creraces.CreRaces;

public class ActionRegistry {

    public interface RaceAction {
        void execute(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
                @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot);
    }

    public interface ActionFactory {
        RaceAction create(JsonObject data);
    }

    private static final Map<ResourceLocation, ActionFactory> REGISTRY = new HashMap<>();

    public static void register(ResourceLocation id, ActionFactory factory) {
        REGISTRY.put(id, factory);
    }

    public static RaceAction fromJson(JsonObject json) {
        if (!json.has("type")) {
            CreRaces.LOGGER.error("Action missing 'type' field — skipping. JSON: {}", json);
            return (player, target, slot) -> {
            };
        }
        String typeStr = json.get("type").getAsString();
        ResourceLocation type = new ResourceLocation(typeStr);
        ActionFactory factory = REGISTRY.get(type);
        if (factory == null) {
            CreRaces.LOGGER.error("Unknown action type '{}' — skipping. Did you forget to register it?", type);
            return (player, target, slot) -> {
            };
        }
        try {
            RaceAction action = factory.create(json);

            if (json.has("chance")) {
                double chance = json.get("chance").getAsDouble();
                return (player, target, slot) -> {
                    if (player.getRandom().nextDouble() < chance) {
                        action.execute(player, target, slot);
                    }
                };
            }

            return action;
        } catch (Exception e) {
            CreRaces.LOGGER.error(
                    "Failed to parse action '{}': {} — action will be skipped at runtime. JSON: {}",
                    type, e.getMessage(), json);
            return (player, target, slot) -> {
            };
        }
    }

    public static void init() {
        // Traits
        mc.sayda.creraces.engine.traits.OnAbilityUseTrait.register();
        // Core Actions will be registered here by their classes
        mc.sayda.creraces.engine.actions.ApplyEffectAction.register();
        mc.sayda.creraces.engine.actions.DelayAction.register();
        mc.sayda.creraces.engine.actions.AOEAction.register();
        mc.sayda.creraces.engine.actions.PlaySoundAction.register();
        mc.sayda.creraces.engine.actions.DashAction.register();
        mc.sayda.creraces.engine.actions.DamageAction.register();
        mc.sayda.creraces.engine.actions.HealAction.register();
        mc.sayda.creraces.engine.actions.ToggleStateAction.register();
        mc.sayda.creraces.engine.actions.SpawnParticlesAction.register();
        mc.sayda.creraces.engine.actions.MorphAction.register();
        mc.sayda.creraces.engine.actions.SetStateAction.register();
        mc.sayda.creraces.engine.actions.ConditionalAction.register();
        mc.sayda.creraces.engine.actions.ClearCooldownsAction.register();
        mc.sayda.creraces.engine.actions.LaunchProjectileAction.register();
        mc.sayda.creraces.engine.actions.DropItemAction.register();
        mc.sayda.creraces.engine.actions.SetCooldownAction.register();
        mc.sayda.creraces.engine.actions.RemoveEffectAction.register();
        mc.sayda.creraces.engine.actions.ModifyEntityDataAction.register();
        mc.sayda.creraces.engine.actions.ModifyResourceAction.register();
        mc.sayda.creraces.engine.actions.CommandAction.register();
        mc.sayda.creraces.engine.actions.ItemAnimationAction.register();
        mc.sayda.creraces.engine.actions.ApplyVelocityAction.register();
        mc.sayda.creraces.engine.actions.PlaceBlockAction.register();
        mc.sayda.creraces.engine.actions.BreakBlocksAction.register();
        mc.sayda.creraces.engine.actions.StealItemAction.register();
        mc.sayda.creraces.engine.actions.ConsumeItemAction.register();
        mc.sayda.creraces.engine.actions.ChangeSizeAction.register();
        mc.sayda.creraces.engine.actions.DisableShieldAction.register();
        mc.sayda.creraces.engine.actions.OpenGUIAction.register();
        mc.sayda.creraces.engine.actions.SmeltItemAction.register();
    }
}
