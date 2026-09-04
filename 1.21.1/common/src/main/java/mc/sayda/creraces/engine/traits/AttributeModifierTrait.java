package mc.sayda.creraces.engine.traits;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.AttributeMethod;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.registry.ModAttributes;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.engine.condition.Condition;
import javax.annotation.Nullable;

public class AttributeModifierTrait implements TraitRegistry.RaceTrait {

    private final ResourceLocation attributeId;
    private final ScalingValue value;
    private final com.google.gson.JsonObject valueJson;
    private final AttributeModifier.Operation operation;
    @Nullable
    private final Condition condition;
    @Nullable
    private final com.google.gson.JsonObject rawCondition;
    private final int interval;
    private final boolean managed;
    private final AttributeMethod method;
    private String traitId = "";

    public AttributeModifierTrait(ResourceLocation attributeId, ScalingValue value, 
            com.google.gson.JsonObject valueJson,
            AttributeModifier.Operation operation, @Nullable Condition condition, 
            @Nullable com.google.gson.JsonObject rawCondition, int interval, boolean managed,
            AttributeMethod method) {
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

    @Override
    public void tick(Player player) {
        // No-op for attributes, they are applied statically or via AttributeIncidents
    }

    /**
     * Resolves the attribute lazily at runtime using the centralized ModAttributes resolver.
     * Returns null if the attribute is not registered (e.g. the mod is absent).
     */
    @Nullable
    public net.minecraft.core.Holder<Attribute> getAttribute() {
        return ModAttributes.getAttribute(attributeId);
    }

    /**
     * Race JSON still uses the pre-1.21 operation names, so those keep working here
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
    public com.google.gson.JsonObject getRawCondition() {
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

    public com.google.gson.JsonObject getValueJson() {
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

            // Store the raw ResourceLocation; do NOT resolve the Attribute here.
            ResourceLocation attrId = ResourceLocation.tryParse(attrIdStr);
            if (attrId == null) {
                attrId = ResourceLocation.fromNamespaceAndPath("creraces", attrIdStr.toLowerCase());
            }

            ScalingValue value = ScalingValue.fromJson(json, "value", 0.0);
            String opStr = GsonHelper.getAsString(json, "operation", "addition").toUpperCase();
            AttributeModifier.Operation op = parseOperation(opStr);

            com.google.gson.JsonObject valueJson;
            if (json.has("value") && json.get("value").isJsonObject()) {
                valueJson = json.getAsJsonObject("value");
            } else if (json.has("value") && json.get("value").isJsonPrimitive()) {
                valueJson = new com.google.gson.JsonObject();
                valueJson.add("base", json.get("value"));
            } else {
                valueJson = new com.google.gson.JsonObject();
            }

            Condition condition = null;
            com.google.gson.JsonObject rawCondition = null;
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
