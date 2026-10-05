package mc.sayda.creraces.engine;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.client.render.AnimationHandler;
import mc.sayda.creraces.client.render.BeamRenderer;
import mc.sayda.creraces.client.render.TetherRenderer;
import mc.sayda.creraces.engine.actions.AOEAction;
import mc.sayda.creraces.engine.actions.ApplyEffectAction;
import mc.sayda.creraces.engine.actions.ApplyVelocityAction;
import mc.sayda.creraces.engine.actions.AttributeModifierAction;
import mc.sayda.creraces.engine.actions.BeamAction;
import mc.sayda.creraces.engine.actions.BindAbilityAction;
import mc.sayda.creraces.engine.actions.BreakBlocksAction;
import mc.sayda.creraces.engine.actions.CancelAction;
import mc.sayda.creraces.engine.actions.ChangeSizeAction;
import mc.sayda.creraces.engine.actions.ChannelAction;
import mc.sayda.creraces.engine.actions.ClaimTerritoryAction;
import mc.sayda.creraces.engine.actions.ClearCooldownsAction;
import mc.sayda.creraces.engine.actions.CommandAction;
import mc.sayda.creraces.engine.actions.ConditionalAction;
import mc.sayda.creraces.engine.actions.ConsumeItemAction;
import mc.sayda.creraces.engine.actions.DamageAction;
import mc.sayda.creraces.engine.actions.DashAction;
import mc.sayda.creraces.engine.actions.DelayAction;
import mc.sayda.creraces.engine.actions.DisableShieldAction;
import mc.sayda.creraces.engine.actions.DisplayResourceAction;
import mc.sayda.creraces.engine.actions.DropItemAction;
import mc.sayda.creraces.engine.actions.DurabilityAction;
import mc.sayda.creraces.engine.actions.EnchantAction;
import mc.sayda.creraces.engine.actions.ExpandPocketAction;
import mc.sayda.creraces.engine.actions.GetEnchantmentAction;
import mc.sayda.creraces.engine.actions.GiveItemAction;
import mc.sayda.creraces.engine.actions.HealAction;
import mc.sayda.creraces.engine.actions.InteractBlockAction;
import mc.sayda.creraces.engine.actions.ItemAnimationAction;
import mc.sayda.creraces.engine.actions.LaunchProjectileAction;
import mc.sayda.creraces.engine.actions.MassSummonAction;
import mc.sayda.creraces.engine.actions.MessageAction;
import mc.sayda.creraces.engine.actions.ModifyEntityDataAction;
import mc.sayda.creraces.engine.actions.ModifyValueAction;
import mc.sayda.creraces.engine.actions.MorphAction;
import mc.sayda.creraces.engine.actions.OpenGUIAction;
import mc.sayda.creraces.engine.actions.PlaceBlockAction;
import mc.sayda.creraces.engine.actions.PlaySoundAction;
import mc.sayda.creraces.engine.actions.PocketEntryAction;
import mc.sayda.creraces.engine.actions.RecallProjectilesAction;
import mc.sayda.creraces.engine.actions.RemoveBlockAction;
import mc.sayda.creraces.engine.actions.RemoveEffectAction;
import mc.sayda.creraces.engine.actions.RemoveEntityAction;
import mc.sayda.creraces.engine.actions.SetCooldownAction;
import mc.sayda.creraces.engine.actions.SetCustomizationAction;
import mc.sayda.creraces.engine.actions.SetFlagAction;
import mc.sayda.creraces.engine.actions.SetOnFireAction;
import mc.sayda.creraces.engine.actions.SleepAction;
import mc.sayda.creraces.engine.actions.SmeltItemAction;
import mc.sayda.creraces.engine.actions.SpawnParticlesAction;
import mc.sayda.creraces.engine.actions.StealItemAction;
import mc.sayda.creraces.engine.actions.StopSoundAction;
import mc.sayda.creraces.engine.actions.SummonEntityAction;
import mc.sayda.creraces.engine.actions.TeleportAction;
import mc.sayda.creraces.engine.actions.TetherAction;
import mc.sayda.creraces.engine.actions.ToggleMinibuildAction;
import mc.sayda.creraces.engine.actions.ToggleStateAction;
import mc.sayda.creraces.engine.actions.UnbindAbilityAction;
import mc.sayda.creraces.engine.actions.UnclaimTerritoryAction;
import mc.sayda.creraces.engine.actions.UpdateBlockAction;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionRegistry {

    public interface RaceAction {
        /** Returns false when the action failed or was cancelled; action chains stop there. */
        boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
                @Nullable BlockPos interactPos);
    }

    public interface ActionFactory {
        RaceAction create(JsonObject data);
    }

    private static final Map<ResourceLocation, ActionFactory> REGISTRY = new HashMap<>();
    private static final ThreadLocal<Integer> RECURSION_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final int MAX_RECURSION_DEPTH = 16;

    /** Stands in for an action that could not be parsed; failing keeps the rest of its chain from running. */
    private static final RaceAction INVALID_ACTION = (player, target, slot, interactPos) -> false;

    /**
     * Drops a player's transient action state (beams, tethers, channels) and resets the client-side
     * renderers. Runs on logout (on both sides) and on death.
     */
    public static void cleanup(Player player) {
        if (player == null)
            return;

        BeamAction.clearForPlayer(player);
        TetherAction.clearTethersFor(player);
        ChannelingManager.clear(player);

        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
            AnimationHandler.clear();
            BeamRenderer.clear();
            TetherRenderer.clear();
        });
    }

    public static void register(ResourceLocation id, ActionFactory factory) {
        REGISTRY.put(id, factory);
    }

    public static RaceAction fromJson(JsonObject json) {
        if (!json.has("type")) {
            CreRaces.LOGGER.error("Action missing 'type' field - skipping. JSON: {}", json);
            return INVALID_ACTION;
        }
        String typeStr = json.get("type").getAsString();
        ResourceLocation type = ResourceLocation.tryParse(typeStr);
        if (type == null) {
            CreRaces.LOGGER.error("Malformed action type '{}' - skipping.", typeStr);
            return INVALID_ACTION;
        }
        ActionFactory factory = REGISTRY.get(type);
        if (factory == null) {
            CreRaces.LOGGER.error("Unknown action type '{}' - skipping. Did you forget to register it?", type);
            return INVALID_ACTION;
        }
        try {
            RaceAction action = factory.create(json);
            if (action == null) {
                CreRaces.LOGGER.error("Action factory for '{}' returned null - skipping.", type);
                return INVALID_ACTION;
            }

            ScalingValue chance = json.has("chance") ? ScalingValue.fromJson(json, "chance", 1.0) : null;

            return (player, target, slot, interactPos) -> {
                int depth = RECURSION_DEPTH.get();
                if (depth >= MAX_RECURSION_DEPTH) {
                    CreRaces.LOGGER.warn(
                            "Action recursion depth limit reached ({})! Skipping action to prevent stack overflow.", MAX_RECURSION_DEPTH);
                    return true;
                }

                if (chance != null
                        && player.getRandom().nextDouble() >= chance.evaluate(player, target, slot, interactPos)) {
                    return true;
                }

                RECURSION_DEPTH.set(depth + 1);
                try {
                    return action.execute(player, target, slot, interactPos);
                } finally {
                    RECURSION_DEPTH.set(depth);
                }
            };
        } catch (Exception e) {
            CreRaces.LOGGER.error(
                    "Failed to parse action '{}': {} - action will be skipped at runtime. JSON: {}",
                    type, e.getMessage(), json);
            return INVALID_ACTION;
        }
    }

    /**
     * Parses the action array under {@code key}. A missing key gives an empty list; a malformed
     * array or entry is logged and skipped.
     */
    public static List<RaceAction> listFromJson(JsonObject json, String key) {
        List<RaceAction> actions = new ArrayList<>();
        if (!json.has(key)) {
            return actions;
        }
        if (!json.get(key).isJsonArray()) {
            CreRaces.LOGGER.warn("'{}' should be an array of actions - ignoring it. JSON: {}", key, json);
            return actions;
        }
        for (JsonElement element : json.getAsJsonArray(key)) {
            if (element.isJsonObject()) {
                actions.add(fromJson(element.getAsJsonObject()));
            } else {
                CreRaces.LOGGER.warn("Skipping non-object entry in '{}': {}", key, element);
            }
        }
        return actions;
    }

    /** Runs the actions in order and stops at the first one that fails. Returns false if any failed. */
    public static boolean runChain(List<RaceAction> actions, Player player, @Nullable LivingEntity target,
            @Nullable AbilitySlot slot, @Nullable BlockPos interactPos) {
        for (RaceAction action : actions) {
            if (!action.execute(player, target, slot, interactPos)) {
                return false;
            }
        }
        return true;
    }

    /** Runs every action in order, even after one fails. */
    public static void runAll(List<RaceAction> actions, Player player, @Nullable LivingEntity target,
            @Nullable AbilitySlot slot, @Nullable BlockPos interactPos) {
        for (RaceAction action : actions) {
            action.execute(player, target, slot, interactPos);
        }
    }

    public static void init() {
        ApplyEffectAction.register();
        DelayAction.register();
        AOEAction.register();
        PlaySoundAction.register();
        DashAction.register();
        DamageAction.register();
        HealAction.register();
        PocketEntryAction.register();
        ExpandPocketAction.register();
        ToggleStateAction.register();
        SpawnParticlesAction.register();
        MorphAction.register();
        ModifyValueAction.register();
        ConditionalAction.register();
        ClearCooldownsAction.register();
        LaunchProjectileAction.register();
        DropItemAction.register();
        SetCooldownAction.register();
        RemoveEffectAction.register();
        ModifyEntityDataAction.register();
        CommandAction.register();
        ItemAnimationAction.register();
        ApplyVelocityAction.register();
        PlaceBlockAction.register();
        BreakBlocksAction.register();
        StealItemAction.register();
        ConsumeItemAction.register();
        ChangeSizeAction.register();
        DisableShieldAction.register();
        OpenGUIAction.register();
        SmeltItemAction.register();
        TeleportAction.register();
        SetCustomizationAction.register();
        ToggleMinibuildAction.register();
        SetFlagAction.register();
        BeamAction.register();
        RemoveBlockAction.register();
        SleepAction.register();
        MessageAction.register();
        SummonEntityAction.register();
        SetOnFireAction.register();
        DisplayResourceAction.register();
        TetherAction.register();
        StopSoundAction.register();
        CancelAction.register();
        GiveItemAction.register();
        RecallProjectilesAction.register();
        MassSummonAction.register();
        RemoveEntityAction.register();
        BindAbilityAction.register();
        UnbindAbilityAction.register();
        EnchantAction.register();
        GetEnchantmentAction.register();
        UpdateBlockAction.register();
        AttributeModifierAction.register();
        InteractBlockAction.register();
        ClaimTerritoryAction.register();
        UnclaimTerritoryAction.register();
        ChannelAction.register();
        DurabilityAction.register();
    }
}
