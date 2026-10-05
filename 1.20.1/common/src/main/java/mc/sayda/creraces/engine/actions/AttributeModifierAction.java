package mc.sayda.creraces.engine.actions;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.ability.AbilitySlot;
import mc.sayda.creraces.capability.DataUtils;
import mc.sayda.creraces.capability.IPlayerVariables;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.AttributeMethod;
import mc.sayda.creraces.engine.ManagedModifier;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.registry.ModAttributes;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Action counterpart to AttributeModifierTrait: explicitly adds or removes an attribute modifier.
 * With "managed", the modifier is also handed to the managed-modifier sync, which keeps its value
 * scaling and drops it once its condition fails.
 */
public class AttributeModifierAction implements ActionRegistry.RaceAction {
    private static final String NAME_PREFIX = "creraces:";

    private final ResourceLocation attributeId;
    private final String id;
    private final String modifierName;
    private final ScalingValue value;
    private final JsonObject rawValue;
    private final AttributeModifier.Operation operation;
    private final AttributeMethod method;
    @Nullable
    private final JsonObject conditionJson;
    @Nullable
    private final Condition condition;
    private final int interval;
    private final boolean managed;

    public AttributeModifierAction(ResourceLocation attributeId, String id, ScalingValue value, JsonObject rawValue,
            AttributeModifier.Operation operation, AttributeMethod method, @Nullable JsonObject conditionJson,
            int interval, boolean managed) {
        this.attributeId = attributeId;
        this.id = id;
        this.modifierName = NAME_PREFIX + id;
        this.value = value;
        this.rawValue = rawValue;
        this.operation = operation;
        this.method = method;
        this.conditionJson = conditionJson;
        this.condition = conditionJson != null ? Condition.fromJson(conditionJson) : null;
        this.interval = interval;
        this.managed = managed;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target, @Nullable AbilitySlot slot,
            @Nullable BlockPos interactPos) {
        Attribute attribute = ModAttributes.getAttribute(attributeId);
        if (attribute == null) {
            return true;
        }
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return true;
        }

        // Derived the same way as trait modifier UUIDs, so it stays stable across sessions.
        UUID uuid = UUID.nameUUIDFromBytes(modifierName.getBytes(StandardCharsets.UTF_8));

        if (method == AttributeMethod.REMOVE) {
            if (instance.getModifier(uuid) != null) {
                instance.removeModifier(uuid);
                DataUtils.getVariables(player).ifPresent(vars -> vars.removeManagedModifier(uuid));
                CreRaces.LOGGER.debug("AttributeModifierAction: REMOVED {} from {}", id, player.getScoreboardName());
            }
            return true;
        }

        // The condition only gates this application; the managed sync re-checks it afterwards.
        if (condition != null && !condition.evaluate(player, target, slot, interactPos)) {
            return true;
        }

        double newValue = value.evaluate(player, target, slot);
        if (ModAttributes.isPercentAttribute(attribute)) {
            newValue /= 100.0;
        }

        AttributeModifier existing = instance.getModifier(uuid);
        if (existing == null || Math.abs(existing.getAmount() - newValue) > 1e-6
                || existing.getOperation() != operation) {
            if (existing != null) {
                instance.removeModifier(uuid);
            }
            instance.addPermanentModifier(new AttributeModifier(uuid, modifierName, newValue, operation));

            DataUtils.getVariables(player).ifPresent(vars -> {
                if (managed) {
                    registerManaged(vars, uuid, player);
                } else {
                    // An unmanaged application must not leave a stale managed entry under the same UUID.
                    vars.removeManagedModifier(uuid);
                }
            });
            CreRaces.LOGGER.debug("AttributeModifierAction: APPLIED {} to {} (val: {})", id,
                    player.getScoreboardName(), newValue);
        }
        return true;
    }

    private void registerManaged(IPlayerVariables vars, UUID uuid, Player player) {
        Optional<ManagedModifier> current = vars.getManagedModifier(uuid);
        if (current.isPresent()) {
            ManagedModifier mod = current.get();
            boolean unchanged = mod.valueJson().equals(rawValue)
                    && (conditionJson == null || mod.conditionJson().equals(conditionJson));
            if (!unchanged) {
                vars.addManagedModifier(toManagedModifier(uuid, player));
                CreRaces.LOGGER.debug("AttributeModifierAction: Updated Managed Modifier {} for {}", id,
                        player.getScoreboardName());
            }
        } else {
            vars.addManagedModifier(toManagedModifier(uuid, player));
            CreRaces.LOGGER.debug("AttributeModifierAction: REGISTERED Managed Modifier {} for {}", id,
                    player.getScoreboardName());
        }
    }

    private ManagedModifier toManagedModifier(UUID uuid, Player player) {
        return new ManagedModifier(uuid, attributeId, rawValue, operation, modifierName,
                conditionJson != null ? conditionJson : new JsonObject(), conditionJson != null, interval,
                player.tickCount + interval);
    }

    public static void register() {
        ActionRegistry.register(new ResourceLocation(CreRaces.MODID, "attribute_modifier"), json -> {
            ResourceLocation attributeId = ResourceLocation.tryParse(
                    GsonHelper.getAsString(json, "attribute", "minecraft:generic.attack_damage"));
            String id = GsonHelper.getAsString(json, "id", "unnamed_modifier");
            JsonObject rawValue = new JsonObject();
            if (json.has("value") && json.get("value").isJsonObject()) {
                rawValue = json.getAsJsonObject("value");
            } else if (json.has("value") && json.get("value").isJsonPrimitive()) {
                // Same shape AttributeModifierTrait gives a plain number, so the managed sync can rebuild it.
                rawValue.add("base", json.get("value"));
            }
            ScalingValue value = ScalingValue.fromJson(json, "value", 0.0);
            AttributeModifier.Operation operation = AttributeModifier.Operation.valueOf(
                    GsonHelper.getAsString(json, "operation", "addition").toUpperCase());
            AttributeMethod method = AttributeMethod.fromString(GsonHelper.getAsString(json, "method", "ADD"));
            JsonObject conditionJson = json.has("condition") ? json.getAsJsonObject("condition") : null;
            int interval = GsonHelper.getAsInt(json, "interval", 20);
            boolean managed = GsonHelper.getAsBoolean(json, "managed", false);

            return new AttributeModifierAction(attributeId, id, value, rawValue, operation, method, conditionJson,
                    interval, managed);
        });
    }
}
