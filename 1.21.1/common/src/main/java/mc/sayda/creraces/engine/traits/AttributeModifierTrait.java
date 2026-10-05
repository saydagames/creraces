package mc.sayda.creraces.engine.traits;

import com.google.gson.JsonObject;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.AttributeMethod;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.registry.ModAttributes;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import javax.annotation.Nullable;

/** Data holder for an attribute modifier; AttributeIncidents applies, syncs and removes it. */
public class AttributeModifierTrait implements TraitRegistry.RaceTrait {

    private final ResourceLocation attributeId;
    private final ScalingValue value;
    private final JsonObject valueJson;
    private final AttributeModifier.Operation operation;
    @Nullable
    private final Condition condition;
    @Nullable
    private final JsonObject rawCondition;
    private final int interval;
    private final boolean managed;
    private final AttributeMethod method;
    private String traitId = "";

    public AttributeModifierTrait(ResourceLocation attributeId, ScalingValue value, JsonObject valueJson,
            AttributeModifier.Operation operation, @Nullable Condition condition,
            @Nullable JsonObject rawCondition, int interval, boolean managed, AttributeMethod method) {
        this.attributeId = attributeId;
        this.value = value;
        this.valueJson = valueJson;
        this.operation = operation;
        this.condition = condition;
        this.rawCondition = rawCondition;
        this.interval = interval;
        this.managed = managed;
        this.method = method;
    }

    /**
     * Resolves the attribute lazily at runtime using the centralized ModAttributes resolver.
     * Returns null if the attribute is not registered (e.g. the mod is absent).
     */
    @Nullable
    public Holder<Attribute> getAttribute() {
        return ModAttributes.getAttribute(attributeId);
    }

    /** Accepts both the pre-1.21 operation names and the current ones, since race JSON is shared. */
    private static AttributeModifier.Operation parseOperation(String opStr) {
        return switch (opStr) {
            case "ADDITION", "ADD_VALUE" -> AttributeModifier.Operation.ADD_VALUE;
            case "MULTIPLY_BASE", "ADD_MULTIPLIED_BASE" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case "MULTIPLY_TOTAL", "ADD_MULTIPLIED_TOTAL" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> {
                CreRaces.LOGGER.warn("Unknown attribute modifier operation '{}', using addition", opStr);
                yield AttributeModifier.Operation.ADD_VALUE;
            }
        };
    }

    /** The raw attribute ID as specified in the race JSON. */
    public ResourceLocation getAttributeId() {
        return attributeId;
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

    @Nullable
    public JsonObject getRawCondition() {
        return rawCondition;
    }

    public int getInterval() {
        return interval;
    }

    public boolean isManaged() {
        return managed;
    }

    public AttributeMethod getMethod() {
        return method;
    }

    public JsonObject getValueJson() {
        return valueJson;
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
        TraitRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "attribute_modifier"), json -> {
            String attrIdStr = GsonHelper.getAsString(json, "attribute", "minecraft:generic.attack_damage");

            // Resolved lazily in getAttribute(), where aliases and Apothic replacements are handled.
            ResourceLocation attrId = ResourceLocation.tryParse(attrIdStr);
            if (attrId == null) {
                attrId = ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, attrIdStr.toLowerCase());
            }

            ScalingValue value = ScalingValue.fromJson(json, "value", 0.0);
            String opStr = GsonHelper.getAsString(json, "operation", "addition").toUpperCase();
            AttributeModifier.Operation op = parseOperation(opStr);

            JsonObject valueJson;
            if (json.has("value") && json.get("value").isJsonObject()) {
                valueJson = json.getAsJsonObject("value");
            } else if (json.has("value") && json.get("value").isJsonPrimitive()) {
                valueJson = new JsonObject();
                valueJson.add("base", json.get("value"));
            } else {
                valueJson = new JsonObject();
            }

            Condition condition = null;
            JsonObject rawCondition = null;
            if (json.has("condition") && json.get("condition").isJsonObject()) {
                rawCondition = json.getAsJsonObject("condition");
                condition = Condition.fromJson(rawCondition);
            }

            int interval = GsonHelper.getAsInt(json, "interval", 20);
            boolean managed = GsonHelper.getAsBoolean(json, "managed", false);
            AttributeMethod method = AttributeMethod.fromString(GsonHelper.getAsString(json, "method", "ADD"));

            return new AttributeModifierTrait(attrId, value, valueJson, op, condition, rawCondition, interval, managed, method);
        });
    }
}
