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
 * Conditional addons (condition != null) are continuously re-evaluated in
 * tick(); CosmeticIncidents only performs the add/remove call itself.
 */
public class AddonTrait implements TraitRegistry.RaceTrait {

    private final String addonId;
    private final String tint; // Hex color or null
    private final boolean permanent; // If true, not shown in mirror UI
    private final Condition condition;
    /**
     * Config group this addon belongs to. RACE_ADDONS_ENABLED must be true for any
     * group.
     * "lore_addons" additionally requires LORE_ADDONS_ENABLED.
     * Defaults to "race_addons" if not specified.
     */
    private final String configGroup;

    public AddonTrait(String addonId, String tint, boolean permanent, @Nullable Condition condition,
            String configGroup) {
        this.addonId = addonId;
        this.tint = tint;
        this.permanent = permanent;
        this.condition = condition;
        this.configGroup = configGroup;
    }

    /** Returns false if the config gate for this addon group is disabled. */
    public boolean isEnabled() {
        if (!mc.sayda.creraces.config.CreRacesConfig.RACE_ADDONS_ENABLED.get())
            return false;
        if ("lore_addons".equals(configGroup) && !mc.sayda.creraces.config.CreRacesConfig.LORE_ADDONS_ENABLED.get())
            return false;
        return true;
    }

    @Override
    public void tick(Player player) {
        if (condition == null || !isEnabled()) return;
        if (player.level().isClientSide()) return;
        if (player.tickCount % 20 != 0) return;

        mc.sayda.twilight_lib.capabilities.IAddons addons =
                mc.sayda.twilight_lib.capabilities.DataUtils.getAddonsData(player);
        if (addons == null) return;

        boolean conditionMet = condition.evaluate(player, null, null, null);
        boolean current = addons.getActiveAddons().contains(addonId);
        if (conditionMet == current) return;

        mc.sayda.creraces.race.CosmeticIncidents.setAddonActiveRobust(addons, addonId, conditionMet, false);

        var pkt = mc.sayda.creraces.race.CosmeticIncidents.createSyncPacket(
                player.getUUID(),
                addons.getActiveAddons(),
                mc.sayda.creraces.race.CosmeticIncidents.getExternalGrantsRobust(addons),
                addons.getAllAddonTints());
        if (pkt != null) {
            mc.sayda.twilight_lib.network.NetworkHandler.sendAddonsToAll(pkt);
        }
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

    public String getConfigGroup() {
        return configGroup;
    }

    @Nullable
    public Condition getCondition() {
        return condition;
    }

    public static void register() {
        TraitRegistry.register(ResourceLocation.fromNamespaceAndPath(CreRaces.MODID, "addon"), json -> {
            String addonId = GsonHelper.getAsString(json, "addon_id");
            @javax.annotation.Nullable String tint = GsonHelper.getNullableString(json, "tint", null);
            boolean permanent = GsonHelper.getAsBoolean(json, "permanent", false);
            String configGroup = GsonHelper.getAsString(json, "config", "race_addons");
            Condition condition = json.has("condition") ? Condition.fromJson(json.getAsJsonObject("condition")) : null;
            return new AddonTrait(addonId, tint, permanent, condition, configGroup);
        });
    }
}
