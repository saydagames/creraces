package mc.sayda.creraces.race;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.util.GsonHelper;

/**
 * Holds granular scaling information for a race.
 * Maps to various Pehkui ScaleTypes.
 */
public record RaceScale(
        float base,
        float width,
        float height,
        float hitboxWidth,
        float hitboxHeight,
        float eyeHeight,
        float reach,
        float miningSpeed,
        float motion,
        float stepHeight,
        float jumpHeight,
        float knockback,
        float fallSpeed) {
    public static final RaceScale DEFAULT = new RaceScale(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 1.0f);

    public static RaceScale fromJson(JsonElement json) {
        if (json == null || json.isJsonNull()) {
            return DEFAULT;
        }
        if (json.isJsonPrimitive()) {
            float s = json.getAsFloat();
            return new RaceScale(s, s, s, s, s, s, s, s, s, s, s, s, s);
        }
        if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();
            float base = GsonHelper.getAsFloat(obj, "base", 1.0f);
            // Default sub-scales to base if not specified
            return new RaceScale(
                    base,
                    GsonHelper.getAsFloat(obj, "width", base),
                    GsonHelper.getAsFloat(obj, "height", base),
                    GsonHelper.getAsFloat(obj, "hitbox_width", base),
                    GsonHelper.getAsFloat(obj, "hitbox_height", base),
                    GsonHelper.getAsFloat(obj, "eye_height", base),
                    GsonHelper.getAsFloat(obj, "reach", base),
                    GsonHelper.getAsFloat(obj, "mining_speed", base),
                    GsonHelper.getAsFloat(obj, "motion", base),
                    GsonHelper.getAsFloat(obj, "step_height", base),
                    GsonHelper.getAsFloat(obj, "jump_height", base),
                    GsonHelper.getAsFloat(obj, "knockback", base),
                    GsonHelper.getAsFloat(obj, "fall_speed", base));
        }
        return DEFAULT;
    }
}
