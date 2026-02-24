package mc.sayda.creraces.engine.traits;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import javax.annotation.Nullable;

/**
 * Trait for racial addons (cosmetic attachments via Twilight Lib).
 * Applied via CosmeticIncidents on race selection/change.
 */
public class AddonTrait implements TraitRegistry.RaceTrait {

    private final String addonId;
    private final String tint; // Hex color or null
    private final boolean permanent; // If true, not shown in mirror UI
    private final Condition condition;

    public AddonTrait(String addonId, String tint, boolean permanent, @Nullable Condition condition) {
        this.addonId = addonId;
        this.tint = tint;
        this.permanent = permanent;
        this.condition = condition;
    }

    @Override
    public void tick(Player player) {
        // No-op - addons are applied statically by CosmeticIncidents
    }

    public String getAddonId() {
        return addonId;
    }

    public String getTint() {
        return tint;
    }

    public boolean isPermanent() {
        return permanent;
    }

    @Nullable
    public Condition getCondition() {
        return condition;
    }

    public static void register() {
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "addon"), json -> {
            String addonId = GsonHelper.getAsString(json, "addon_id");
            String tint = GsonHelper.getAsString(json, "tint", null);
            boolean permanent = GsonHelper.getAsBoolean(json, "permanent", false);
            Condition condition = json.has("condition") ? Condition.fromJson(json.getAsJsonObject("condition")) : null;
            return new AddonTrait(addonId, tint, permanent, condition);
        });
    }
}
