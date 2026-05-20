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
    public Attribute getAttribute() {
        return ModAttributes.getAttribute(attributeId);
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
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "attribute_modifier"), json -> {
            String attrIdStr = GsonHelper.getAsString(json, "attribute", "minecraft:generic.attack_damage");

            // Store the raw ResourceLocation  Edo NOT resolve the Attribute here.
            ResourceLocation attrId = ResourceLocation.tryParse(attrIdStr);
            if (attrId == null) {
                attrId = new ResourceLocation("creraces", attrIdStr.toLowerCase());
            }

            ScalingValue value = ScalingValue.fromJson(json, "value", 0.0);
            String opStr = GsonHelper.getAsString(json, "operation", "addition").toUpperCase();
            AttributeModifier.Operation op = AttributeModifier.Operation.valueOf(opStr);

            com.google.gson.JsonObject valueJson = json.has("value") && json.get("value").isJsonObject() 
                    ? json.getAsJsonObject("value") : new com.google.gson.JsonObject();

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
