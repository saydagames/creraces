package mc.sayda.creraces.race;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mc.sayda.creraces.engine.ScalingValue;

import java.util.ArrayList;

/**
 * Per-race scale values, one for each Pehkui ScaleType the mod drives.
 */
public class RaceScale {
    private final ScalingValue base;
    private final ScalingValue width;
    private final ScalingValue height;
    private final ScalingValue hitboxWidth;
    private final ScalingValue hitboxHeight;
    private final ScalingValue eyeHeight;
    private final ScalingValue reach;
    private final ScalingValue miningSpeed;
    private final ScalingValue motion;
    private final ScalingValue stepHeight;
    private final ScalingValue jumpHeight;
    private final ScalingValue knockback;
    private final ScalingValue fallSpeed;

    public RaceScale(ScalingValue base, ScalingValue width, ScalingValue height, ScalingValue hitboxWidth,
            ScalingValue hitboxHeight, ScalingValue eyeHeight, ScalingValue reach, ScalingValue miningSpeed,
            ScalingValue motion, ScalingValue stepHeight, ScalingValue jumpHeight, ScalingValue knockback,
            ScalingValue fallSpeed) {
        this.base = base;
        this.width = width;
        this.height = height;
        this.hitboxWidth = hitboxWidth;
        this.hitboxHeight = hitboxHeight;
        this.eyeHeight = eyeHeight;
        this.reach = reach;
        this.miningSpeed = miningSpeed;
        this.motion = motion;
        this.stepHeight = stepHeight;
        this.jumpHeight = jumpHeight;
        this.knockback = knockback;
        this.fallSpeed = fallSpeed;
    }

    public ScalingValue base() {
        return base;
    }

    public ScalingValue width() {
        return width;
    }

    public ScalingValue height() {
        return height;
    }

    public ScalingValue hitboxWidth() {
        return hitboxWidth;
    }

    public ScalingValue hitboxHeight() {
        return hitboxHeight;
    }

    public ScalingValue eyeHeight() {
        return eyeHeight;
    }

    public ScalingValue reach() {
        return reach;
    }

    public ScalingValue miningSpeed() {
        return miningSpeed;
    }

    public ScalingValue motion() {
        return motion;
    }

    public ScalingValue stepHeight() {
        return stepHeight;
    }

    public ScalingValue jumpHeight() {
        return jumpHeight;
    }

    public ScalingValue knockback() {
        return knockback;
    }

    public ScalingValue fallSpeed() {
        return fallSpeed;
    }

    public static final RaceScale DEFAULT = new RaceScale(unscaled(), unscaled(), unscaled(), unscaled(),
            unscaled(), unscaled(), unscaled(), unscaled(), unscaled(), unscaled(), unscaled(), unscaled(),
            unscaled());

    private static ScalingValue unscaled() {
        return new ScalingValue(1.0, null, 0, new ArrayList<>());
    }

    public static RaceScale fromJson(JsonElement json) {
        if (json == null || !json.isJsonObject()) {
            return DEFAULT;
        }

        JsonObject obj = json.getAsJsonObject();
        // BASE already multiplies every other Pehkui scale, so the sub-scales default to 1.0
        // rather than inheriting base, which would apply it twice.
        return new RaceScale(
                ScalingValue.fromJson(obj, "base", 1.0),
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
}
