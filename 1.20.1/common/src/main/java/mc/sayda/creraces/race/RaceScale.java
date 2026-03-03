package mc.sayda.creraces.race;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.engine.ScalingValue;
import mc.sayda.creraces.util.GsonHelper;

/**
 * Holds granular scaling information for a race.
 * Maps to various Pehkui ScaleTypes.
 */
public record RaceScale(
        ScalingValue base,
        ScalingValue width,
        ScalingValue height,
        ScalingValue hitboxWidth,
        ScalingValue hitboxHeight,
        ScalingValue eyeHeight,
        ScalingValue reach,
        ScalingValue miningSpeed,
        ScalingValue motion,
        ScalingValue stepHeight,
        ScalingValue jumpHeight,
        ScalingValue knockback,
        ScalingValue fallSpeed) {
    public static final RaceScale DEFAULT = new RaceScale(
            new ScalingValue(1.0, null, 0, new java.util.ArrayList<>()),
            new ScalingValue(1.0, null, 0, new java.util.ArrayList<>()),
            new ScalingValue(1.0, null, 0, new java.util.ArrayList<>()),
            new ScalingValue(1.0, null, 0, new java.util.ArrayList<>()),
            new ScalingValue(1.0, null, 0, new java.util.ArrayList<>()),
            new ScalingValue(1.0, null, 0, new java.util.ArrayList<>()),
            new ScalingValue(1.0, null, 0, new java.util.ArrayList<>()),
            new ScalingValue(1.0, null, 0, new java.util.ArrayList<>()),
            new ScalingValue(1.0, null, 0, new java.util.ArrayList<>()),
            new ScalingValue(1.0, null, 0, new java.util.ArrayList<>()),
            new ScalingValue(1.0, null, 0, new java.util.ArrayList<>()),
            new ScalingValue(1.0, null, 0, new java.util.ArrayList<>()),
            new ScalingValue(1.0, null, 0, new java.util.ArrayList<>()));

    public static RaceScale fromJson(JsonElement json) {
        if (json == null || json.isJsonNull()) {
            return DEFAULT;
        }
        if (json.isJsonPrimitive()) {
            JsonObject wrapper = new JsonObject();
            wrapper.add("val", json);
            ScalingValue s = ScalingValue.fromJson(wrapper, "val", 1.0);
            return new RaceScale(s, s, s, s, s, s, s, s, s, s, s, s, s);
        }
        if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();
            ScalingValue base = ScalingValue.fromJson(obj, "base", 1.0);
            // Pehkui's BASE scale is a master multiplier.
            // Default sub-scales to 1.0 to avoid double-scaling.
            return new RaceScale(
                    base,
                    ScalingValue.fromJson(obj, "width", 1.0),
                    ScalingValue.fromJson(obj, "height", 1.0),
                    ScalingValue.fromJson(obj, "hitbox_width", 1.0),
                    ScalingValue.fromJson(obj, "hitbox_height", 1.0),
                    ScalingValue.fromJson(obj, "eye_height", 1.0),
                    ScalingValue.fromJson(obj, "reach", 1.0),
                    ScalingValue.fromJson(obj, "mining_speed", 1.0),
                    ScalingValue.fromJson(obj, "motion", 1.0),
                    ScalingValue.fromJson(obj, "step_height", 1.0),
                    ScalingValue.fromJson(obj, "jump_height", 1.0),
                    ScalingValue.fromJson(obj, "knockback", 1.0),
                    ScalingValue.fromJson(obj, "fall_speed", 1.0));
        }
        return DEFAULT;
    }
}
