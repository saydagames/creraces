package mc.sayda.creraces.engine;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mc.sayda.creraces.engine.condition.Condition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Metadata for an attribute modifier that is "Managed" by the engine.
 * Managed modifiers re-evaluate a condition periodically and remove themselves if it fails.
 */
public record ManagedModifier(
        UUID uuid,
        ResourceLocation attributeId,
        JsonObject valueJson,
        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation operation,
        String name,
        JsonObject conditionJson,
        boolean hasLifecycle,
        int interval,
        long nextCheck
) {
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
    }

    public boolean shouldCheck(long currentTick) {
        return currentTick >= nextCheck;
    }

    public ManagedModifier withNextCheck(long currentTick) {
        return new ManagedModifier(uuid, attributeId, valueJson, operation, name, conditionJson, hasLifecycle, interval, currentTick + interval);
    }

    public Condition getCondition() {
        return Condition.fromJson(conditionJson);
    }

    public mc.sayda.creraces.engine.ScalingValue getScalingValue() {
        com.google.gson.JsonObject wrapper = new com.google.gson.JsonObject();
        wrapper.add("value", valueJson);
        return mc.sayda.creraces.engine.ScalingValue.fromJson(wrapper, "value", 0.0);
    }
}
