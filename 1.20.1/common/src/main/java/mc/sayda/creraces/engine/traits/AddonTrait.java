package mc.sayda.creraces.engine.traits;

import mc.sayda.creraces.CreRaces;
import mc.sayda.creraces.config.CreRacesConfig;
import mc.sayda.creraces.engine.TraitRegistry;
import mc.sayda.creraces.engine.condition.Condition;
import mc.sayda.creraces.race.CosmeticIncidents;
import mc.sayda.creraces.util.GsonHelper;
import mc.sayda.twilight_lib.capabilities.DataUtils;
import mc.sayda.twilight_lib.capabilities.IAddons;
import mc.sayda.twilight_lib.network.NetworkHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Trait for racial addons (cosmetic attachments via Twilight Lib).
 * Conditional addons (condition != null) are continuously re-evaluated in
 * tick(); CosmeticIncidents only performs the add/remove call itself.
 */
public class AddonTrait implements TraitRegistry.RaceTrait {
    private static final int CONDITION_CHECK_INTERVAL = 20;

    private final String addonId;
    @Nullable
    private final String tint; // hex colour
    @Nullable
    private final Condition condition;
    /**
     * Config gate: every group needs RACE_ADDONS_ENABLED, and "lore_addons" also needs
     * LORE_ADDONS_ENABLED. Defaults to "race_addons".
     */
    private final String configGroup;

    public AddonTrait(String addonId, @Nullable String tint, @Nullable Condition condition, String configGroup) {
        this.addonId = addonId;
        this.tint = tint;
        this.condition = condition;
        this.configGroup = configGroup;
    }

    /** Returns false if the config gate for this addon group is disabled. */
    public boolean isEnabled() {
        if (!CreRacesConfig.RACE_ADDONS_ENABLED.get())
            return false;
        return !"lore_addons".equals(configGroup) || CreRacesConfig.LORE_ADDONS_ENABLED.get();
    }

    @Override
    public void tick(Player player) {
        if (condition == null || !isEnabled()) return;
        if (player.level().isClientSide()) return;
        if (player.tickCount % CONDITION_CHECK_INTERVAL != 0) return;

        IAddons addons = DataUtils.getAddonsData(player);
        if (addons == null) return;

        boolean conditionMet = condition.evaluate(player, null, null, null);
        boolean current = addons.getActiveAddons().contains(addonId);
        if (conditionMet == current) return;

        CosmeticIncidents.setAddonActiveRobust(addons, addonId, conditionMet, false);

        var pkt = CosmeticIncidents.createSyncPacket(
                player.getUUID(),
                addons.getActiveAddons(),
                CosmeticIncidents.getExternalGrantsRobust(addons),
                addons.getAllAddonTints());
        if (pkt != null) {
            NetworkHandler.sendAddonsToAll(pkt);
        }
    }

    public String getAddonId() {
        return addonId;
    }

    @Nullable
    public String getTint() {
        return tint;
    }

    public String getConfigGroup() {
        return configGroup;
    }

    @Nullable
    public Condition getCondition() {
        return condition;
    }

    public static void register() {
        TraitRegistry.register(new ResourceLocation(CreRaces.MODID, "addon"), json -> {
            String addonId = GsonHelper.getAsString(json, "addon_id");
            String tint = GsonHelper.getNullableString(json, "tint", null);
            String configGroup = GsonHelper.getAsString(json, "config", "race_addons");
            Condition condition = json.has("condition") ? Condition.fromJson(json.getAsJsonObject("condition")) : null;
            return new AddonTrait(addonId, tint, condition, configGroup);
        });
    }
}
