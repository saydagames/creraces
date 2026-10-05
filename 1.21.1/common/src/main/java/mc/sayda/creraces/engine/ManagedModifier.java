package mc.sayda.creraces.engine;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.condition.Condition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * An attribute modifier the engine keeps in sync with its JSON value and condition. The JSON is
 * kept for saving and change detection; the parsed Condition and ScalingValue are built once and
 * carried over by {@link #withNextCheck}.
 */
public final class ManagedModifier {

    private final ResourceLocation id;
    private final ResourceLocation attributeId;
    private final JsonObject valueJson;
    private final AttributeModifier.Operation operation;
    private final String name;
    private final JsonObject conditionJson;
    private final boolean hasLifecycle;
    private final int interval;
    private final long nextCheck;

    private final Condition cachedCondition;
    private final ScalingValue cachedScalingValue;

    /**
     * Builds the modifier's registry id from a plain engine id string. 1.21+ keys attribute
     * modifiers by ResourceLocation rather than UUID, so the id has to be path-safe: anything
     * outside [a-z0-9_.-] is folded to an underscore.
     */
    public static ResourceLocation idOf(String rawId) {
        String path = rawId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        if (path.isEmpty()) path = "unnamed_modifier";
        return ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, path);
    }

    public ManagedModifier(
            ResourceLocation id,
            ResourceLocation attributeId,
            JsonObject valueJson,
            AttributeModifier.Operation operation,
            String name,
            JsonObject conditionJson,
            boolean hasLifecycle,
            int interval,
            long nextCheck) {
        this(id, attributeId, valueJson, operation, name, conditionJson, hasLifecycle, interval, nextCheck,
                Condition.fromJson(conditionJson), parseValue(valueJson));
    }

    private ManagedModifier(ResourceLocation id, ResourceLocation attributeId, JsonObject valueJson,
            AttributeModifier.Operation operation, String name, JsonObject conditionJson, boolean hasLifecycle,
            int interval, long nextCheck, Condition cachedCondition, ScalingValue cachedScalingValue) {
        this.id = id;
        this.attributeId = attributeId;
        this.valueJson = valueJson;
        this.operation = operation;
        this.name = name;
        this.conditionJson = conditionJson;
        this.hasLifecycle = hasLifecycle;
        this.interval = interval;
        this.nextCheck = nextCheck;
        this.cachedCondition = cachedCondition;
        this.cachedScalingValue = cachedScalingValue;
    }

    private static ScalingValue parseValue(JsonObject valueJson) {
        JsonObject wrapper = new JsonObject();
        wrapper.add("value", valueJson);
        return ScalingValue.fromJson(wrapper, "value", 0.0);
    }

    public ResourceLocation id()    { return id; }
    public ResourceLocation attributeId() { return attributeId; }
    public JsonObject valueJson()   { return valueJson; }
    public AttributeModifier.Operation operation() { return operation; }
    public String name()            { return name; }
    public JsonObject conditionJson() { return conditionJson; }
    public boolean hasLifecycle()   { return hasLifecycle; }
    public int interval()           { return interval; }
    public long nextCheck()         { return nextCheck; }

    public Condition getCondition()         { return cachedCondition; }
    public ScalingValue getScalingValue()   { return cachedScalingValue; }

    public boolean shouldCheck(long currentTick) {
        return currentTick >= nextCheck;
    }

    public ManagedModifier withNextCheck(long currentTick) {
        return new ManagedModifier(id, attributeId, valueJson, operation, name, conditionJson, hasLifecycle,
                interval, currentTick + interval, cachedCondition, cachedScalingValue);
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id.toString());
        tag.putString("attribute", attributeId.toString());
        tag.putString("value", valueJson.toString());
        tag.putString("operation", operation.name());
        tag.putString("name", name);
        tag.putString("condition", conditionJson.toString());
        tag.putBoolean("hasLifecycle", hasLifecycle);
        tag.putInt("interval", interval);
        tag.putLong("nextCheck", nextCheck);
        return tag;
    }

    @Nullable
    public static ManagedModifier fromNBT(CompoundTag tag) {
        try {
            return new ManagedModifier(
                    ResourceLocation.parse(tag.getString("id")),
                    ResourceLocation.parse(tag.getString("attribute")),
                    JsonParser.parseString(tag.getString("value")).getAsJsonObject(),
                    AttributeModifier.Operation.valueOf(tag.getString("operation")),
                    tag.getString("name"),
                    JsonParser.parseString(tag.getString("condition")).getAsJsonObject(),
                    tag.getBoolean("hasLifecycle"),
                    tag.getInt("interval"),
                    tag.getLong("nextCheck")
            );
        } catch (Exception e) {
            CreRaces.LOGGER.error("Failed to deserialize ManagedModifier from NBT: {}", e.getMessage());
            return null;
        }
    }
}
