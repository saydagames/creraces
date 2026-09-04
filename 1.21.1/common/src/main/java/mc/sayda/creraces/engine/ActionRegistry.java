package mc.sayda.creraces.engine;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import java.util.HashMap;
import java.util.Map;
import mc.sayda.creraces.CreRaces;

public class ActionRegistry {
    /**
     * Clears all non-persistent cached states related to actions for a player.
     * Should be called on logout, death (if resetOnDeath), or race reset.
     *
     * This serves as the 'Universal Clear' for both server and client side.
     */
    public static void cleanup(Player player) {
        if (player == null)
            return;

        // Server-side action state cleanup
        mc.sayda.creraces.engine.actions.BeamAction.clearForPlayer(player);
        mc.sayda.creraces.engine.actions.TetherAction.clearTethersFor(player);
        mc.sayda.creraces.engine.ChannelingManager.clear(player);

        // Client-side renderer cleanup
        dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> {
            mc.sayda.creraces.client.render.AnimationHandler.clear();
            mc.sayda.creraces.client.render.BeamRenderer.clear();
            mc.sayda.creraces.client.render.TetherRenderer.clear();
        });
    }

    public interface RaceAction {
        boolean execute(Player player, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity target,
                @javax.annotation.Nullable mc.sayda.creraces.ability.AbilitySlot slot,
                @javax.annotation.Nullable net.minecraft.core.BlockPos interact_pos);
    }

    public interface ActionFactory {
        RaceAction create(JsonObject data);
    }

    private static final Map<ResourceLocation, ActionFactory> REGISTRY = new HashMap<>();
    private static final ThreadLocal<Integer> RECURSION_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final int MAX_RECURSION_DEPTH = 16;

    public static void register(ResourceLocation id, ActionFactory factory) {
        REGISTRY.put(id, factory);
    }

    public static RaceAction fromJson(JsonObject json) {
        if (!json.has("type")) {
            CreRaces.LOGGER.error("Action missing 'type' field - skipping. JSON: {}", json);
            return (player, target, slot, interact_pos) -> false;
        }
        String typeStr = json.get("type").getAsString();
        ResourceLocation type = ResourceLocation.tryParse(typeStr);
        if (type == null) {
            CreRaces.LOGGER.error("Malformed action type '{}' - skipping.", typeStr);
            return (player, target, slot, interact_pos) -> false;
        }
        ActionFactory factory = REGISTRY.get(type);
        if (factory == null) {
            CreRaces.LOGGER.error("Unknown action type '{}' - skipping. Did you forget to register it?", type);
            return (player, target, slot, interact_pos) -> false;
        }
        try {
            RaceAction action = factory.create(json);
            if (action == null) {
                CreRaces.LOGGER.error("Action factory for '{}' returned null - skipping.", type);
                return (player, target, slot, interact_pos) -> false;
            }

            ScalingValue chance = json.has("chance") ? ScalingValue.fromJson(json, "chance", 1.0) : null;

            return (player, target, slot, interact_pos) -> {
                int depth = RECURSION_DEPTH.get();
                if (depth >= MAX_RECURSION_DEPTH) {
                    CreRaces.LOGGER.warn(
                            "Action recursion depth limit reached ({})! Skipping action to prevent stack overflow.", MAX_RECURSION_DEPTH);
                    return true;
                }

                if (chance != null && player.getRandom().nextDouble() >= chance.evaluate(player, target)) {
                    return true;
                }

                RECURSION_DEPTH.set(depth + 1);
                try {
                    return action.execute(player, target, slot, interact_pos);
                } finally {
                    RECURSION_DEPTH.set(depth);
                }
            };
        } catch (Exception e) {
            CreRaces.LOGGER.error(
                    "Failed to parse action '{}': {} - action will be skipped at runtime. JSON: {}",
                    type, e.getMessage(), json);
            return (player, target, slot, interact_pos) -> false;
        }
    }

    public static void init() {
        // Core Actions will be registered here by their classes
        mc.sayda.creraces.engine.actions.ApplyEffectAction.register();
        mc.sayda.creraces.engine.actions.DelayAction.register();
        mc.sayda.creraces.engine.actions.AOEAction.register();
        mc.sayda.creraces.engine.actions.PlaySoundAction.register();
        mc.sayda.creraces.engine.actions.DashAction.register();
        mc.sayda.creraces.engine.actions.DamageAction.register();
        mc.sayda.creraces.engine.actions.HealAction.register();
        mc.sayda.creraces.engine.actions.PocketEntryAction.register();
        mc.sayda.creraces.engine.actions.ExpandPocketAction.register();
        mc.sayda.creraces.engine.actions.ToggleStateAction.register();
        mc.sayda.creraces.engine.actions.SpawnParticlesAction.register();
        mc.sayda.creraces.engine.actions.MorphAction.register();
        mc.sayda.creraces.engine.actions.ModifyValueAction.register();
        mc.sayda.creraces.engine.actions.ConditionalAction.register();
        mc.sayda.creraces.engine.actions.ClearCooldownsAction.register();
        mc.sayda.creraces.engine.actions.LaunchProjectileAction.register();
        mc.sayda.creraces.engine.actions.DropItemAction.register();
        mc.sayda.creraces.engine.actions.SetCooldownAction.register();
        mc.sayda.creraces.engine.actions.RemoveEffectAction.register();
        mc.sayda.creraces.engine.actions.ModifyEntityDataAction.register();
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
        mc.sayda.creraces.engine.actions.TeleportAction.register();
        mc.sayda.creraces.engine.actions.SetCustomizationAction.register();
        mc.sayda.creraces.engine.actions.EnterSpiritRealmAction.register();
        mc.sayda.creraces.engine.actions.ToggleMinibuildAction.register();
        mc.sayda.creraces.engine.actions.SetFlagAction.register();
        mc.sayda.creraces.engine.actions.BeamAction.register();
        mc.sayda.creraces.engine.actions.RemoveBlockAction.register();
        mc.sayda.creraces.engine.actions.SleepAction.register();
        mc.sayda.creraces.engine.actions.MessageAction.register();
        mc.sayda.creraces.engine.actions.SummonEntityAction.register();
        mc.sayda.creraces.engine.actions.SetOnFireAction.register();
        mc.sayda.creraces.engine.actions.DisplayResourceAction.register();
        mc.sayda.creraces.engine.actions.TetherAction.register();
        mc.sayda.creraces.engine.actions.StopSoundAction.register();
        mc.sayda.creraces.engine.actions.CancelAction.register();
        mc.sayda.creraces.engine.actions.GiveItemAction.register();
        mc.sayda.creraces.engine.actions.RecallProjectilesAction.register();
        mc.sayda.creraces.engine.actions.MassSummonAction.register();
        mc.sayda.creraces.engine.actions.BindAbilityAction.register();
        mc.sayda.creraces.engine.actions.UnbindAbilityAction.register();
        mc.sayda.creraces.engine.actions.EnchantAction.register();
        mc.sayda.creraces.engine.actions.GetEnchantmentAction.register();
        mc.sayda.creraces.engine.actions.UpdateBlockAction.register();
        mc.sayda.creraces.engine.actions.AttributeModifierAction.register();
        mc.sayda.creraces.engine.actions.InteractBlockAction.register();
        mc.sayda.creraces.engine.actions.ClaimTerritoryAction.register();
        mc.sayda.creraces.engine.actions.UnclaimTerritoryAction.register();
        mc.sayda.creraces.engine.actions.ChannelAction.register();
        mc.sayda.creraces.engine.actions.DurabilityAction.register();
    }
}
