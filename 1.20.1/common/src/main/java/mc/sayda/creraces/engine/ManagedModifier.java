package mc.sayda.creraces.engine;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mc.sayda.creraces.engine.condition.Condition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Metadata for an attribute modifier managed by the engine.
 * Condition and ScalingValue are parsed once at construction to avoid
 * re-parsing JSON on every tick.
 */
public final class ManagedModifier {

    private final UUID uuid;
    private final ResourceLocation attributeId;
    private final JsonObject valueJson;
    private final net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation operation;
    private final String name;
    private final JsonObject conditionJson;
    private final boolean hasLifecycle;
    private final int interval;
    private final long nextCheck;

    // Cached at construction - never re-parsed
    private final Condition cachedCondition;
    private final ScalingValue cachedScalingValue;

    public ManagedModifier(
            UUID uuid,
            ResourceLocation attributeId,
            JsonObject valueJson,
            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation operation,
            String name,
            JsonObject conditionJson,
            boolean hasLifecycle,
            int interval,
            long nextCheck) {
        this.uuid = uuid;
        this.attributeId = attributeId;
        this.valueJson = valueJson;
        this.operation = operation;
        this.name = name;
        this.conditionJson = conditionJson;
        this.hasLifecycle = hasLifecycle;
        this.interval = interval;
        this.nextCheck = nextCheck;

        this.cachedCondition = Condition.fromJson(conditionJson);
        JsonObject wrapper = new JsonObject();
        wrapper.add("value", valueJson);
        this.cachedScalingValue = ScalingValue.fromJson(wrapper, "value", 0.0);
    }

    // Accessors

    public UUID uuid()              { return uuid; }
    public ResourceLocation attributeId() { return attributeId; }
    public JsonObject valueJson()   { return valueJson; }
    public net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation operation() { return operation; }
    public String name()            { return name; }
    public JsonObject conditionJson() { return conditionJson; }
    public boolean hasLifecycle()   { return hasLifecycle; }
    public int interval()           { return interval; }
    public long nextCheck()         { return nextCheck; }

    // Cached accessors

    public Condition getCondition()         { return cachedCondition; }
    public ScalingValue getScalingValue()   { return cachedScalingValue; }

    // Lifecycle

    public boolean shouldCheck(long currentTick) {
        return currentTick >= nextCheck;
    }

    public ManagedModifier withNextCheck(long currentTick) {
        return new ManagedModifier(uuid, attributeId, valueJson, operation, name,
                conditionJson, hasLifecycle, interval, currentTick + interval);
    }

    // Serialization

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("uuid", uuid);
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

    public static ManagedModifier fromNBT(CompoundTag tag) {
        try {
            return new ManagedModifier(
                    tag.getUUID("uuid"),
                    new ResourceLocation(tag.getString("attribute")),
                    JsonParser.parseString(tag.getString("value")).getAsJsonObject(),
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.valueOf(tag.getString("operation")),
                    tag.getString("name"),
                    JsonParser.parseString(tag.getString("condition")).getAsJsonObject(),
                    tag.getBoolean("hasLifecycle"),
                    tag.getInt("interval"),
                    tag.getLong("nextCheck")
            );
        } catch (Exception e) {
            mc.sayda.creraces.CreRaces.LOGGER.error("Failed to deserialize ManagedModifier from NBT: {}", e.getMessage());
            return null;
        }
    }
}
