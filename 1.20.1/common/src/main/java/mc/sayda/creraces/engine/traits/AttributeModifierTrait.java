package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.engine.condition.ConditionRegistry;
import mc.sayda.creraces.registry.ModAttributes;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import javax.annotation.Nullable;

/**
 * A race attribute modifier: "attribute" changed by "value" with "operation" (add_value,
 * add_multiplied_base or add_multiplied_total). It is re-evaluated every second, so scaling values
 * stay current, and is only present while "condition" holds. The modifier's identity is the trait's "id".
 */
public class AttributeModifierTrait implements TraitRegistry.RaceTrait {
    private final ResourceLocation attributeId;
    private final ScalingValue value;
    private final AttributeModifier.Operation operation;
    @Nullable
    private final Condition condition;
    private String traitId = "";

    AttributeModifierTrait(JsonObject json) {
        String attribute = GsonHelper.getAsString(json, "attribute");
        this.attributeId = ResourceLocation.tryParse(attribute);
        if (attributeId == null) {
            throw new IllegalArgumentException("malformed attribute id '" + attribute + "'");
        }
        this.value = ScalingValue.fromJson(json, "value", 0.0);
        this.operation = parseOperation(GsonHelper.getAsString(json, "operation", "add_value"));
        this.condition = ConditionRegistry.optional(json, "condition");
    }

    /** The modifier operations by their current vanilla names, the same on both game versions. */
    public static AttributeModifier.Operation parseOperation(String name) {
        return switch (name) {
            case "add_value" -> AttributeModifier.Operation.ADDITION;
            case "add_multiplied_base" -> AttributeModifier.Operation.MULTIPLY_BASE;
            case "add_multiplied_total" -> AttributeModifier.Operation.MULTIPLY_TOTAL;
            default -> throw new IllegalArgumentException(
                    "unknown operation '" + name + "' (add_value, add_multiplied_base, add_multiplied_total)");
        };
    }

    /** Resolved lazily, through the Apothic Attributes replacements when that mod is loaded; null if absent. */
    @Nullable
    public Attribute getAttribute() {
        return ModAttributes.getAttribute(attributeId);
    }

    public ScalingValue getValue() {
        return value;
    }

    public AttributeModifier.Operation getOperation() {
        return operation;
    }

    @Nullable
    public Condition getCondition() {
        return condition;
    }

    @Override
    public void setTraitId(String id) {
        this.traitId = id;
    }

    @Override
    public String getTraitId() {
        return traitId;
    }

    public static void register() {
        TraitRegistry.register(CreRaces.id("attribute_modifier"), AttributeModifierTrait::new);
    }
}
