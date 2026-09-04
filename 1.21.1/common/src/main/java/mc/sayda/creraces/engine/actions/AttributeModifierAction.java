package mc.sayda.creraces.engine.actions;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ActionRegistry;
import mc.sayda.creraces.engine.AttributeMethod;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.registry.ModAttributes;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Action counterpart to AttributeModifierTrait.
 * Allows explicit ADD or REMOVE of attribute modifiers via abilities.
 * Uses the same deterministic UUID logic as traits for compatibility.
 */
public class AttributeModifierAction implements ActionRegistry.RaceAction {

    private final ResourceLocation attributeId;
    private final String id;
    private final ScalingValue value;
    private final AttributeModifier.Operation operation;
    private final AttributeMethod method;
    @Nullable private final com.google.gson.JsonObject condition;
    private final com.google.gson.JsonObject rawValue;
    private final int interval;
    private final boolean managed;

    public AttributeModifierAction(ResourceLocation attributeId, String id, ScalingValue value,
                                    com.google.gson.JsonObject rawValue,
                                    AttributeModifier.Operation operation, AttributeMethod method,
                                    @Nullable com.google.gson.JsonObject condition, int interval, boolean managed) {
        this.attributeId = attributeId;
        this.id = id;
        this.value = value;
        this.rawValue = rawValue;
        this.operation = operation;
        this.method = method;
        this.condition = condition;
        this.interval = interval;
        this.managed = managed;
    }

    @Override
    public boolean execute(Player player, @Nullable LivingEntity target,
                           @Nullable mc.sayda.creraces.ability.AbilitySlot slot,
                           @Nullable net.minecraft.core.BlockPos interact_pos) {
        
        // Resolve attribute (same logic as Trait)
        net.minecraft.core.Holder<Attribute> resolvedAttr = ModAttributes.getAttribute(attributeId);
        if (resolvedAttr == null) return true;

        AttributeInstance instance = player.getAttribute(resolvedAttr);
        if (instance == null) return true;

        // Deterministic modifier id derived from the engine id (consistent with trait purge logic)
        ResourceLocation modifierId = mc.sayda.creraces.engine.ManagedModifier.idOf(id);

        if (method == AttributeMethod.REMOVE) {
            if (instance.getModifier(modifierId) != null) {
                instance.removeModifier(modifierId);
                mc.sayda.creraces.capability.DataUtils.getVariables(player).ifPresent(vars -> vars.removeManagedModifier(modifierId));
                CreRaces.LOGGER.debug("AttributeModifierAction: REMOVED {} from {}", id, player.getScoreboardName());
            }
            return true;
        }

        // ADD logic (Condition is a one-time gate)
        boolean conditionMet = condition == null || mc.sayda.creraces.engine.condition.Condition.fromJson(condition).evaluate(player, target, slot, interact_pos);
        if (!conditionMet) return true;

        double newValue = value.evaluate(player, target, slot);
        if (ModAttributes.isPercentAttribute(resolvedAttr)) {
            newValue /= 100.0;
        }

        // Check if update is needed
        AttributeModifier existing = instance.getModifier(modifierId);
        if (existing == null || Math.abs(existing.amount() - newValue) > 1e-6 || existing.operation() != operation) {
            if (existing != null) instance.removeModifier(modifierId);

            AttributeModifier newMod = new AttributeModifier(modifierId, newValue, operation);
            instance.addPermanentModifier(newMod);

            // Register as Managed if 'managed' flag is present
            if (managed) {
                mc.sayda.creraces.capability.DataUtils.getVariables(player).ifPresent(vars -> {
                    vars.getManagedModifier(modifierId).ifPresentOrElse(mod -> {
                        if (!mod.valueJson().equals(rawValue) ||
                            (condition != null && !mod.conditionJson().equals(condition))) {
                            vars.addManagedModifier(new mc.sayda.creraces.engine.ManagedModifier(
                                modifierId, attributeId, rawValue, operation, "creraces:" + id,
                                condition != null ? condition : new com.google.gson.JsonObject(),
                                condition != null, interval, player.tickCount + interval
                            ));
                            CreRaces.LOGGER.debug("AttributeModifierAction: Updated Managed Modifier {} for {}", id, player.getScoreboardName());
                        }
                    }, () -> {
                        vars.addManagedModifier(new mc.sayda.creraces.engine.ManagedModifier(
                            modifierId, attributeId, rawValue, operation, "creraces:" + id,
                            condition != null ? condition : new com.google.gson.JsonObject(),
                            condition != null, interval, player.tickCount + interval
                        ));
                        CreRaces.LOGGER.debug("AttributeModifierAction: REGISTERED Managed Modifier {} for {}", id, player.getScoreboardName());
                    });
                });
            } else {
                // Not managed, but we should make sure any old managed entry for this id is gone
                mc.sayda.creraces.capability.DataUtils.getVariables(player).ifPresent(vars -> vars.removeManagedModifier(modifierId));
            }
            
            CreRaces.LOGGER.debug("AttributeModifierAction: APPLIED {} to {} (val: {})", id, player.getScoreboardName(), newValue);
        }

        return true;
    }


    /**
     * Ability JSON still uses the pre-1.21 operation names, so those keep working here
     * alongside the current ones.
     */
    private static AttributeModifier.Operation parseOperation(String opStr) {
        return switch (opStr) {
            case "ADDITION", "ADD_VALUE" -> AttributeModifier.Operation.ADD_VALUE;
            case "MULTIPLY_BASE", "ADD_MULTIPLIED_BASE" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case "MULTIPLY_TOTAL", "ADD_MULTIPLIED_TOTAL" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> AttributeModifier.Operation.ADD_VALUE;
        };
    }

    public static void register() {
        ActionRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "attribute_modifier"), json -> {
            String attrIdStr = GsonHelper.getAsString(json, "attribute", "minecraft:generic.attack_damage");
            ResourceLocation attrId = ResourceLocation.tryParse(attrIdStr);
            
            String id = GsonHelper.getAsString(json, "id", "unnamed_modifier");
            
            com.google.gson.JsonObject valueJson = json.has("value") ? json.get("value").getAsJsonObject() : new com.google.gson.JsonObject();
            ScalingValue value = ScalingValue.fromJson(json, "value", 0.0);
            
            String opStr = GsonHelper.getAsString(json, "operation", "addition").toUpperCase();
            AttributeModifier.Operation operation = parseOperation(opStr);
            
            String methodStr = GsonHelper.getAsString(json, "method", "ADD");
            AttributeMethod method = AttributeMethod.fromString(methodStr);

            com.google.gson.JsonObject condition = json.has("condition") ? json.getAsJsonObject("condition") : null;
            int interval = GsonHelper.getAsInt(json, "interval", 20);
            boolean managed = GsonHelper.getAsBoolean(json, "managed", false);

            return new AttributeModifierAction(attrId, id, value, valueJson, operation, method, condition, interval, managed);
        });
    }
}
